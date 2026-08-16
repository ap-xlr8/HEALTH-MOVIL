package com.healthos.presentation.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthos.domain.model.Alert
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MlRiskResult
import com.healthos.domain.model.WearableDevice
import com.healthos.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val patientRepository: PatientRepository,
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

        private val _actionState = MutableStateFlow(PatientActionState())
        val actionState: StateFlow<PatientActionState> = _actionState

        fun triggerSos(
            lat: Double = 19.4326,
            lng: Double = -99.1332,
        ) {
            viewModelScope.launch {
                val alert = patientRepository.triggerSos(com.healthos.domain.model.SosLocation(lat, lng))
                _actionState.value = PatientActionState(message = alert.title)
            }
        }

        fun linkMockDevice() {
            viewModelScope.launch {
                patientRepository.linkDevice(
                    WearableDevice(
                        id = "AA:BB:CC:DD:EE:FF",
                        model = "Xiaomi Band 8",
                        protocol = com.healthos.domain.model.DeviceProtocol.PROPRIETARY_XIAOMI,
                        publicKey = "MIIBIjANBgkqhkiG9w0",
                        connected = true,
                    ),
                )
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
    }
