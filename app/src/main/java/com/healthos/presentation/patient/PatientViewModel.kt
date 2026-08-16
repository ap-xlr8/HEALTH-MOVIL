package com.healthos.presentation.patient

import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.bluetooth.BleConnectionManager
import com.healthos.bluetooth.BleState
import com.healthos.bluetooth.ScannedBleDevice
import com.healthos.domain.model.Alert
import com.healthos.domain.model.Allergy
import com.healthos.domain.model.DeviceProtocol
import com.healthos.domain.model.DynamicSyncConfig
import com.healthos.domain.model.FamilyHistory
import com.healthos.domain.model.GynecoObstetricHistory
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.LifestyleHabits
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.MlRiskResult
import com.healthos.domain.model.NotificationPreferences
import com.healthos.domain.model.PathologicalHistory
import com.healthos.domain.model.SosLocation
import com.healthos.domain.model.UserProfile
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
        val userProfile: StateFlow<UserProfile?> =
            patientRepository.userProfile()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        val bleState: StateFlow<BleState> = bleManager.connectionState
        val scannedDevices: StateFlow<List<ScannedBleDevice>> = bleManager.scannedDevices

        // Clinical History Reactive Flows
        val allergies: StateFlow<List<Allergy>> =
            patientRepository.allergies()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val pathologicalHistory: StateFlow<List<PathologicalHistory>> =
            patientRepository.pathologicalHistory()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val gynecoObstetricHistory: StateFlow<GynecoObstetricHistory?> =
            patientRepository.gynecoObstetricHistory()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        val familyHistory: StateFlow<List<FamilyHistory>> =
            patientRepository.familyHistory()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        val lifestyleHabits: StateFlow<LifestyleHabits?> =
            patientRepository.lifestyleHabits()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        val notificationPreferences: StateFlow<NotificationPreferences> =
            patientRepository.notificationPreferences()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPreferences())
        val dynamicSyncConfig: StateFlow<DynamicSyncConfig> =
            patientRepository.dynamicSyncConfig()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DynamicSyncConfig())

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
                    measurement.heartRate?.let { hr ->
                        patientRepository.saveBleMeasurementMulti(MetricType.HEART_RATE, hr.toDouble(), "bpm")
                    }
                    measurement.spo2?.let { spo2 ->
                        patientRepository.saveBleMeasurementMulti(MetricType.SPO2, spo2, "%")
                    }
                    measurement.skinTempCelsius?.let { temp ->
                        patientRepository.saveBleMeasurementMulti(MetricType.SKIN_TEMPERATURE, temp, "°C")
                    }
                    measurement.edaMicroSiemens?.let { eda ->
                        patientRepository.saveBleMeasurementMulti(MetricType.EDA, eda, "µS")
                    }
                    measurement.systolicBp?.let { sbp ->
                        patientRepository.saveBleMeasurementMulti(MetricType.BLOOD_PRESSURE_SYSTOLIC, sbp, "mmHg")
                    }
                    measurement.rmssd?.let { rmssd ->
                        patientRepository.saveBleMeasurementMulti(MetricType.HRV_RMSSD, rmssd, "ms")
                    }
                }
            }
            startLocationUpdates()
        }

        fun startLocationUpdates() {
            try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
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
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
                }
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener)
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
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
                        batteryPercent = 100,
                        rssi = -55,
                        firmwareVersion = "1.0.0",
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
                _actionState.value = PatientActionState(message = "Dosis de medicamento registrada")
            }
        }

        // --- Clinical History Actions ---

        fun addAllergy(allergy: Allergy) {
            viewModelScope.launch {
                patientRepository.addAllergy(allergy)
                _actionState.value = PatientActionState(message = "Alergia registrada")
            }
        }

        fun deleteAllergy(id: String) {
            viewModelScope.launch {
                patientRepository.deleteAllergy(id)
                _actionState.value = PatientActionState(message = "Alergia eliminada")
            }
        }

        fun addPathologicalHistory(entry: PathologicalHistory) {
            viewModelScope.launch {
                patientRepository.addPathologicalHistory(entry)
                _actionState.value = PatientActionState(message = "Antecedente patológico guardado")
            }
        }

        fun deletePathologicalHistory(id: String) {
            viewModelScope.launch {
                patientRepository.deletePathologicalHistory(id)
                _actionState.value = PatientActionState(message = "Antecedente eliminado")
            }
        }

        fun saveGynecoObstetricHistory(history: GynecoObstetricHistory) {
            viewModelScope.launch {
                patientRepository.saveGynecoObstetricHistory(history)
                _actionState.value = PatientActionState(message = "Datos gineco-obstétricos guardados")
            }
        }

        fun addFamilyHistory(entry: FamilyHistory) {
            viewModelScope.launch {
                patientRepository.addFamilyHistory(entry)
                _actionState.value = PatientActionState(message = "Antecedente familiar registrado")
            }
        }

        fun deleteFamilyHistory(id: String) {
            viewModelScope.launch {
                patientRepository.deleteFamilyHistory(id)
                _actionState.value = PatientActionState(message = "Antecedente familiar eliminado")
            }
        }

        fun saveLifestyleHabits(habits: LifestyleHabits) {
            viewModelScope.launch {
                patientRepository.saveLifestyleHabits(habits)
                _actionState.value = PatientActionState(message = "Hábitos de vida actualizados")
            }
        }

        fun updateNotificationPreferences(preferences: NotificationPreferences) {
            viewModelScope.launch {
                patientRepository.saveNotificationPreferences(preferences)
                _actionState.value = PatientActionState(message = "Preferencias de notificación actualizadas")
            }
        }

        fun updatePatientProfile(
            firstName: String,
            lastName: String,
            phone: String,
            healthProfile: HealthProfile,
        ) {
            viewModelScope.launch {
                patientRepository.updatePatientProfile(firstName, lastName, phone, healthProfile)
                _actionState.value = PatientActionState(message = "Perfil actualizado correctamente")
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

