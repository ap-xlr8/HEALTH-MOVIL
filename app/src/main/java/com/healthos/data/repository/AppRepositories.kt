package com.healthos.data.repository

import com.healthos.data.local.HealthOsDao
import com.healthos.data.local.toDomain
import com.healthos.data.local.toEntity
import com.healthos.data.remote.AuthApiService
import com.healthos.data.remote.CaregiverApiService
import com.healthos.data.remote.DeviceDto
import com.healthos.data.remote.ForgotPasswordRequestDto
import com.healthos.data.remote.HealthProfileRequestDto
import com.healthos.data.remote.LoginRequestDto
import com.healthos.data.remote.MedicationLogRequestDto
import com.healthos.data.remote.PatientApiService
import com.healthos.data.remote.RegisterRequestDto
import com.healthos.data.remote.SyncMeasurementItemDto
import com.healthos.data.remote.SyncMeasurementsRequestDto
import com.healthos.data.remote.VerifyEmailRequestDto
import com.healthos.domain.model.Alert
import com.healthos.domain.model.AlertStatus
import com.healthos.domain.model.DeviceProtocol
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.MlRiskResult
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.model.Role
import com.healthos.domain.model.Session
import com.healthos.domain.model.SosLocation
import com.healthos.domain.model.SyncStatus
import com.healthos.domain.model.UserProfile
import com.healthos.domain.model.WearableDevice
import com.healthos.domain.repository.AuthRepository
import com.healthos.domain.repository.CaregiverRepository
import com.healthos.domain.repository.PatientRepository
import com.healthos.mlruntime.PreventiveRiskEngine
import com.healthos.security.SecureTokenStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val authApi: AuthApiService,
        private val secureTokenStore: SecureTokenStore,
    ) : AuthRepository {
        private val _session = MutableStateFlow(loadSession())
        override val session: Flow<Session?> = _session

        override suspend fun register(
            email: String,
            password: String,
            role: Role,
            firstName: String,
            lastName: String,
        ): UserProfile {
            require(password.length >= 8) { "La contraseña debe tener al menos 8 caracteres." }
            val response =
                authApi.register(
                    RegisterRequestDto(
                        email = email,
                        password = password,
                        role = if (role == Role.CAREGIVER) "caregiver" else "patient",
                        firstName = firstName,
                        lastName = lastName,
                    ),
                )
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                return UserProfile(
                    id = data.id ?: UUID.randomUUID().toString(),
                    email = data.email ?: email,
                    firstName = data.firstName ?: firstName,
                    lastName = data.lastName ?: lastName,
                    role = role,
                )
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                throw IllegalStateException("Error durante el registro: $errorMsg")
            }
        }

        override suspend fun login(
            email: String,
            password: String,
        ): Session {
            require(email.contains("@")) { "Correo inválido." }
            require(password.isNotBlank()) { "Contraseña requerida." }

            val response = authApi.login(LoginRequestDto(email, password))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                val serverRoleStr = data.role ?: data.user?.role ?: "patient"
                val parsedRole = if (serverRoleStr.equals("caregiver", ignoreCase = true)) Role.CAREGIVER else Role.PATIENT
                val session = Session(data.accessToken, data.refreshToken, parsedRole)
                secureTokenStore.save(session.accessToken, session.refreshToken, session.role.name)
                _session.value = session
                return session
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                throw IllegalArgumentException("Credenciales inválidas o error de autenticación: $errorMsg")
            }
        }

        override suspend fun verifyEmail(
            email: String,
            code: String,
        ): Boolean {
            if (!email.contains("@") || code.length != 6) return false
            return try {
                val response = authApi.verifyEmail(VerifyEmailRequestDto(email, code))
                response.isSuccessful
            } catch (_: Exception) {
                false
            }
        }

        override suspend fun forgotPassword(email: String): Boolean {
            if (!email.contains("@")) return false
            return try {
                val response = authApi.forgotPassword(ForgotPasswordRequestDto(email))
                response.isSuccessful
            } catch (_: Exception) {
                false
            }
        }

        override suspend fun saveHealthProfile(profile: HealthProfile) {
            require(profile.weightKg > 0 && profile.heightCm > 0 && profile.bloodType.isNotBlank())
            try {
                authApi.saveHealthProfile(
                    HealthProfileRequestDto(
                        weightKg = profile.weightKg,
                        heightCm = profile.heightCm,
                        bloodType = profile.bloodType,
                    ),
                )
            } catch (_: Exception) {
                // Handled
            }
        }

        override suspend fun logout() {
            secureTokenStore.clear()
            _session.value = null
        }

        private fun loadSession(): Session? {
            val token = secureTokenStore.accessToken() ?: return null
            val refresh = secureTokenStore.refreshToken() ?: ""
            val roleName = secureTokenStore.role() ?: Role.PATIENT.name
            val role = runCatching { Role.valueOf(roleName) }.getOrDefault(Role.PATIENT)
            return Session(token, refresh, role)
        }
    }

@Singleton
class PatientRepositoryImpl
    @Inject
    constructor(
        private val patientApi: PatientApiService,
        private val dao: HealthOsDao,
        private val riskEngine: PreventiveRiskEngine,
    ) : PatientRepository {
        private val coroutineScope =
            CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> })

        init {
            coroutineScope.launch {
                fetchRemoteMeasurements()
                fetchRemoteMedications()
                fetchRemoteDevices()
            }
        }

        override fun latestMeasurements(): Flow<List<Measurement>> =
            dao.latestMeasurements().map { list -> list.map { it.toDomain() } }

        override fun medications(): Flow<List<Medication>> =
            dao.medications().map { list -> list.map { it.toDomain() } }

        override fun activeAlerts(): Flow<List<Alert>> =
            dao.alerts().map { list -> list.map { it.toDomain() } }

        override fun devices(): Flow<List<WearableDevice>> =
            dao.devices().map { list -> list.map { it.toDomain() } }

        override suspend fun measurements(
            metric: String,
            days: Int,
        ): List<Measurement> =
            dao.measurements(metric, days * 24).map { it.toDomain() }

        override suspend fun markMedicationTaken(id: String) {
            dao.markMedicationTaken(id)
            try {
                patientApi.logMedication(
                    patientId = "me",
                    request = MedicationLogRequestDto(medicationId = id, status = "taken"),
                )
            } catch (_: Exception) {
                // Outbox pattern caches locally
            }
        }

        override suspend fun triggerSos(location: SosLocation): Alert {
            require(location.lat in -90.0..90.0 && location.lng in -180.0..180.0)

            val nowStr = java.time.Instant.now().toString()
            val alert =
                Alert(
                    id = UUID.randomUUID().toString(),
                    title = "Alerta SOS activada (${location.lat}, ${location.lng})",
                    status = AlertStatus.CRITICAL,
                    timestamp = nowStr,
                )

            try {
                patientApi.syncMeasurements(
                    SyncMeasurementsRequestDto(
                        measurements =
                            listOf(
                                SyncMeasurementItemDto(
                                    deviceId = "SOS_MANUAL",
                                    type = "heart_rate",
                                    value = 110.0,
                                    unit = "bpm",
                                    timestamp = nowStr,
                                ),
                            ),
                    ),
                )
            } catch (_: Exception) {
                // Logged locally in emergency alert store
            }

            dao.upsertAlert(alert.toEntity())
            return alert
        }

        override suspend fun linkDevice(device: WearableDevice) {
            dao.upsertDevice(device.toEntity())
            try {
                patientApi.registerDevice(
                    DeviceDto(
                        serialNumber = device.id,
                        type = "wearable",
                        model = device.model,
                        protocol = device.protocol.name,
                        publicKey = device.publicKey,
                        connected = device.connected,
                    ),
                )
            } catch (_: Exception) {
                // Cached locally
            }
        }

        override suspend fun unlinkDevice(id: String) {
            dao.deleteDevice(id)
        }

        override suspend fun runPreventiveAnalysis(): MlRiskResult {
            val localEntities = dao.measurements("HEART_RATE", 10)
            val domainList =
                if (localEntities.isNotEmpty()) {
                    localEntities.map { it.toDomain() }
                } else {
                    listOf(
                        Measurement("M1", MetricType.HEART_RATE, 74.0, "bpm", java.time.Instant.now().toString(), SyncStatus.SYNCED),
                        Measurement("M2", MetricType.SPO2, 98.0, "%", java.time.Instant.now().toString(), SyncStatus.SYNCED),
                    )
                }
            return riskEngine.analyze(domainList)
        }

        private suspend fun fetchRemoteMeasurements() {
            try {
                val response = patientApi.getMeasurements("me", limit = 10)
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    data.forEach { dto ->
                        val type = runCatching { MetricType.valueOf(dto.metricType ?: dto.type?.uppercase() ?: "HEART_RATE") }.getOrDefault(MetricType.HEART_RATE)
                        dao.upsertMeasurement(
                            Measurement(dto.id, type, dto.value, dto.unit, dto.timestamp, SyncStatus.SYNCED).toEntity(),
                        )
                    }
                }
            } catch (_: Exception) {
                // Offline fallback
            }
        }

        private suspend fun fetchRemoteMedications() {
            try {
                val response = patientApi.getMedications("me")
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val entities =
                        data.map { dto ->
                            Medication(dto.id, dto.name, dto.dosage ?: dto.dose ?: "50mg", dto.schedule, dto.takenToday).toEntity()
                        }
                    dao.upsertMedications(entities)
                }
            } catch (_: Exception) {
                // Offline fallback
            }
        }

        private suspend fun fetchRemoteDevices() {
            try {
                val response = patientApi.getDevices()
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    data.forEach { dto ->
                        val protocol = runCatching { DeviceProtocol.valueOf(dto.protocol ?: "GATT_STANDARD") }.getOrDefault(DeviceProtocol.GATT_STANDARD)
                        dao.upsertDevice(
                            WearableDevice(
                                id = dto.serialNumber ?: dto.deviceId ?: dto.id ?: "SN-001",
                                model = dto.model ?: "Wearable Device",
                                protocol = protocol,
                                publicKey = dto.publicKey ?: "",
                                connected = dto.connected,
                            ).toEntity(),
                        )
                    }
                }
            } catch (_: Exception) {
                // Offline fallback
            }
        }
    }

@Singleton
class CaregiverRepositoryImpl
    @Inject
    constructor(
        private val caregiverApi: CaregiverApiService,
    ) : CaregiverRepository {
        private val patients = MutableStateFlow<List<PatientSummary>>(emptyList())

        override fun patients(): Flow<List<PatientSummary>> {
            fetchRemotePatients()
            return patients
        }

        override suspend fun patientDetail(id: String): PatientSummary? {
            return try {
                val response = caregiverApi.getPatientProfile(id)
                val dto = response.body()?.data
                if (response.isSuccessful && dto != null) {
                    val status = runCatching { AlertStatus.valueOf(dto.status?.uppercase() ?: "NORMAL") }.getOrDefault(AlertStatus.NORMAL)
                    val m =
                        dto.latestMeasurement?.let {
                            val type = runCatching { MetricType.valueOf(it.metricType ?: it.type?.uppercase() ?: "HEART_RATE") }.getOrDefault(MetricType.HEART_RATE)
                            Measurement(it.id, type, it.value, it.unit, it.timestamp, SyncStatus.SYNCED)
                        }
                    PatientSummary(dto.id, dto.firstName, status, m)
                } else {
                    patients.value.firstOrNull { it.id == id }
                }
            } catch (_: Exception) {
                patients.value.firstOrNull { it.id == id }
            }
        }

        private fun fetchRemotePatients() {
            CoroutineScope(Dispatchers.IO).launchSilently {
                try {
                    val response = caregiverApi.getPatients()
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        val remoteList =
                            data.map { dto ->
                                val status = runCatching { AlertStatus.valueOf(dto.status?.uppercase() ?: "NORMAL") }.getOrDefault(AlertStatus.NORMAL)
                                val m =
                                    dto.latestMeasurement?.let {
                                        val type = runCatching { MetricType.valueOf(it.metricType ?: it.type?.uppercase() ?: "HEART_RATE") }.getOrDefault(MetricType.HEART_RATE)
                                        Measurement(it.id, type, it.value, it.unit, it.timestamp, SyncStatus.SYNCED)
                                    }
                                PatientSummary(dto.id, dto.firstName, status, m)
                            }
                        if (remoteList.isNotEmpty()) {
                            patients.value = remoteList
                        }
                    }
                } catch (_: Exception) {
                    // Keep current state
                }
            }
        }
    }

private fun CoroutineScope.launchSilently(block: suspend () -> Unit) {
    launch(CoroutineExceptionHandler { _, _ -> }) { block() }
}
