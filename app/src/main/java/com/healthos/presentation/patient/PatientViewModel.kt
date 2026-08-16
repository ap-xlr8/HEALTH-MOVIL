package com.healthos.presentation.patient

import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.bluetooth.BleConnectionManager
import com.healthos.bluetooth.BleState
import com.healthos.bluetooth.ScannedBleDevice
import com.healthos.domain.model.Alert
import com.healthos.domain.model.DeviceProtocol
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MlRiskResult
import com.healthos.domain.model.SosLocation
import com.healthos.domain.model.WearableDevice
import com.healthos.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientActionState(
    val message: String? = null,
    val risk: MlRiskResult? = null,
)

@HiltViewModel
class PatientViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val patientRepository: PatientRepository,
        private val bleManager: BleConnectionManager,
    ) : ViewModel() {
        val measurements: StateFlow<List<Measurement>> =
            patientRepository.latestMeasurements()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val medications: StateFlow<List<Medication>> =
            patientRepository.medications()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val alerts: StateFlow<List<Alert>> =
            patientRepository.activeAlerts()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val devices: StateFlow<List<WearableDevice>> =
            patientRepository.devices()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val pendingSyncCount: StateFlow<Int> =
            patientRepository.pendingSyncCount()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
        val healthProfile: StateFlow<HealthProfile?> =
            patientRepository.healthProfile()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        val bleState: StateFlow<BleState> = bleManager.connectionState
        val scannedDevices: StateFlow<List<ScannedBleDevice>> = bleManager.scannedDevices

        private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
        val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation

        private val _actionState = MutableStateFlow(PatientActionState())
        val actionState: StateFlow<PatientActionState> = _actionState

        private val locationListener =
            android.location.LocationListener { loc ->
                _currentLocation.value = loc.latitude to loc.longitude
            }

        init {
            viewModelScope.launch {
                bleManager.measurements.collect { measurement ->
                    patientRepository.saveBleMeasurement(measurement.heartRate.toDouble())
                }
            }
            startLocationUpdates()
        }

        fun startLocationUpdates() {
            try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
                // Check latest from best provider immediately
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER,
                )
                for (provider in providers) {
                    if (manager.isProviderEnabled(provider)) {
                        val last = manager.getLastKnownLocation(provider)
                        if (last != null) {
                            _currentLocation.value = last.latitude to last.longitude
                            break
                        }
                    }
                }
                // Register live listener on GPS and Network
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
                }
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener)
                }
            } catch (_: SecurityException) {
                // Permission not yet granted
            } catch (_: Exception) {
                // Ignore provider error
            }
        }

        override fun onCleared() {
            super.onCleared()
            try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                manager?.removeUpdates(locationListener)
            } catch (_: Exception) {
            }
        }

        fun startBleScan() {
            bleManager.startScan()
        }

        fun stopBleScan() {
            bleManager.stopScan()
        }

        fun connectToScannedDevice(device: ScannedBleDevice) {
            viewModelScope.launch {
                bleManager.connectToAddress(device.mac)
                patientRepository.linkDevice(
                    WearableDevice(
                        id = device.mac,
                        model = device.name,
                        protocol = DeviceProtocol.GATT_STANDARD,
                        publicKey = device.mac,
                        connected = true,
                    ),
                )
                _actionState.value = PatientActionState(message = "Dispositivo ${device.name} vinculado")
            }
        }

        fun triggerSos() {
            viewModelScope.launch {
                val location = _currentLocation.value ?: lastKnownLocation()
                if (location == null) {
                    _actionState.value = PatientActionState(message = "No se pudo obtener tu ubicación GPS en tiempo real.")
                    return@launch
                }
                val alert = patientRepository.triggerSos(SosLocation(location.first, location.second))
                _actionState.value = PatientActionState(message = alert.title)
            }
        }

        fun linkDevice(device: WearableDevice) {
            viewModelScope.launch {
                patientRepository.linkDevice(device)
                _actionState.value = PatientActionState(message = "Dispositivo vinculado")
            }
        }

        fun unlinkDevice(id: String) {
            viewModelScope.launch {
                patientRepository.unlinkDevice(id)
                _actionState.value = PatientActionState(message = "Dispositivo desvinculado")
            }
        }

        fun analyzeRisk() {
            viewModelScope.launch {
                _actionState.value = PatientActionState(risk = patientRepository.runPreventiveAnalysis())
            }
        }

        fun markMedicationTaken(id: String) {
            viewModelScope.launch {
                patientRepository.markMedicationTaken(id)
                _actionState.value = PatientActionState(message = "Medicamento registrado")
            }
        }

        private fun lastKnownLocation(): Pair<Double, Double>? {
            return try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER,
                )
                for (provider in providers) {
                    if (manager.isProviderEnabled(provider)) {
                        val last = manager.getLastKnownLocation(provider)
                        if (last != null) return last.latitude to last.longitude
                    }
                }
                null
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }
