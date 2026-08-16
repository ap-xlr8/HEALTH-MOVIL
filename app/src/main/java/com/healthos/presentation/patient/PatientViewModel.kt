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

        private val _actionState = MutableStateFlow(PatientActionState())
        val actionState: StateFlow<PatientActionState> = _actionState

        init {
            viewModelScope.launch {
                bleManager.measurements.collect { measurement ->
                    patientRepository.saveBleMeasurement(measurement.heartRate.toDouble())
                }
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
                val location = lastKnownLocation()
                if (location == null) {
                    _actionState.value = PatientActionState(message = "No se pudo obtener tu ubicación GPS.")
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
                val last = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: return null
                last.latitude to last.longitude
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }
