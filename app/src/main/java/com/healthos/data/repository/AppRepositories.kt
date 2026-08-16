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
import com.healthos.data.remote.SosAlertRequestDto
import com.healthos.data.remote.SosLocationDto
import com.healthos.data.remote.SyncMeasurementItemDto
import com.healthos.data.remote.SyncMeasurementsRequestDto
import com.healthos.data.remote.TwoFactorResendRequestDto
import com.healthos.data.remote.TwoFactorVerifyRequestDto
import com.healthos.data.remote.VerifyEmailTokenDto
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
            require(password.length in 9..128) { "La contraseña debe tener entre 9 y 128 caracteres." }
            require(password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() }) {
                "La contraseña debe incluir al menos un número y un símbolo especial."
            }

            val response =
                authApi.register(
                    RegisterRequestDto(
                        email = email.trim().lowercase(),
                        password = password,
                        role = if (role == Role.CAREGIVER) "caregiver" else "patient",
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                    ),
                )
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                return UserProfile(
                    id = data.userId ?: UUID.randomUUID().toString(),
                    email = email.trim().lowercase(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
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
        ): Boolean {
            require(email.contains("@")) { "Correo inválido." }
            require(password.isNotBlank()) { "Contraseña requerida." }

            val response = authApi.login(LoginRequestDto(email.trim().lowercase(), password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                // El backend emite el desafío 2FA antes de entregar tokens; los
                // tokens solo se obtienen tras verificar el código OTP.
                if (body.status.equals("2fa_required", ignoreCase = true)) {
                    return true
                }
                throw IllegalArgumentException("Respuesta de autenticación inesperada del servidor.")
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                throw IllegalArgumentException("Credenciales inválidas o error de autenticación: $errorMsg")
            }
        }

        override suspend fun verify2FA(
            email: String,
            code: String,
        ): Session {
            require(email.contains("@")) { "Correo inválido." }
            require(code.isNotBlank()) { "Código de 6 dígitos requerido." }

            val response = authApi.verify2FA(TwoFactorVerifyRequestDto(email.trim().lowercase(), code.trim()))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val parsedRole = extractRoleFromJwt(body.accessToken)
                val userId = extractUserIdFromJwt(body.accessToken)
                val session = Session(body.accessToken, body.refreshToken, parsedRole)
                secureTokenStore.save(session.accessToken, session.refreshToken, session.role.name, userId)
                _session.value = session
                return session
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                throw IllegalArgumentException("Código 2FA incorrecto o expirado (válido por 10 min): $errorMsg")
            }
        }

        override suspend fun resend2FA(email: String): Boolean {
            if (!email.contains("@")) return false
            return try {
                val response = authApi.resend2FA(TwoFactorResendRequestDto(email.trim().lowercase()))
                response.isSuccessful
            } catch (_: Exception) {
                false
            }
        }

        override suspend fun verifyEmail(
            email: String,
            code: String,
        ): Boolean {
            if (code.isBlank()) return false
            return try {
                val response = authApi.verifyEmail(VerifyEmailTokenDto(token = code))
                response.isSuccessful
            } catch (_: Exception) {
                false
            }
        }

        override suspend fun forgotPassword(email: String): Boolean {
            if (!email.contains("@")) return false
            return try {
                val response = authApi.forgotPassword(ForgotPasswordRequestDto(email.trim().lowercase()))
                response.isSuccessful
            } catch (_: Exception) {
                false
            }
        }

        override suspend fun saveHealthProfile(profile: HealthProfile) {
            require(profile.weightKg > 0 && profile.heightCm > 0 && profile.bloodType.isNotBlank())
            secureTokenStore.saveHealthProfile(profile.weightKg, profile.heightCm, profile.bloodType)
            try {
                authApi.saveHealthProfile(
                    HealthProfileRequestDto(
                        weightKg = profile.weightKg,
                        heightCm = profile.heightCm,
                        bloodType = profile.bloodType,
                    ),
                )
            } catch (_: Exception) {
                // Guardado local; se reintentará con el backend cuando haya red.
            }
        }

        override suspend fun logout() {
            try {
                authApi.logout()
            } catch (_: Exception) {
                // Ignore network error on logout
            }
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
        private val secureTokenStore: SecureTokenStore,
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

        private fun currentPatientId(): String = secureTokenStore.userId().takeIf { !it.isNullOrBlank() } ?: "me"

        override fun latestMeasurements(): Flow<List<Measurement>> = dao.latestMeasurements().map { list -> list.map { it.toDomain() } }

        override fun medications(): Flow<List<Medication>> = dao.medications().map { list -> list.map { it.toDomain() } }

        override fun activeAlerts(): Flow<List<Alert>> = dao.alerts().map { list -> list.map { it.toDomain() } }

        override fun devices(): Flow<List<WearableDevice>> = dao.devices().map { list -> list.map { it.toDomain() } }

        override fun pendingSyncCount(): Flow<Int> = dao.countPendingMeasurements()

        override fun healthProfile(): Flow<HealthProfile?> {
            val weight = secureTokenStore.healthWeightKg()
            val height = secureTokenStore.healthHeightCm()
            val bloodType = secureTokenStore.healthBloodType()
            val profile =
                if (weight != null && height != null && bloodType != null) {
                    HealthProfile(weight, height, bloodType)
                } else {
                    null
                }
            return MutableStateFlow(profile)
        }

        override suspend fun measurements(
            metric: String,
            days: Int,
        ): List<Measurement> = dao.measurements(metric, days * 24).map { it.toDomain() }

        override suspend fun markMedicationTaken(id: String) {
            dao.markMedicationTaken(id)
            try {
                patientApi.logMedication(
                    patientId = currentPatientId(),
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
                patientApi.triggerSosAlert(
                    SosAlertRequestDto(
                        location = SosLocationDto(lat = location.lat, lng = location.lng),
                        trigger = "MANUAL_BUTTON"
                    )
                )
            } catch (_: Exception) {
                // Preserved in local emergency database if offline
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

        override suspend fun saveBleMeasurement(value: Double) {
            if (value <= 0.0) return
            val entity =
                Measurement(
                    id = "ble_${UUID.randomUUID()}",
                    metricType = MetricType.HEART_RATE,
                    value = value,
                    unit = "bpm",
                    timestamp = java.time.Instant.now().toString(),
                    syncStatus = SyncStatus.PENDING,
                ).toEntity()
            dao.upsertMeasurement(entity)
        }

        override suspend fun runPreventiveAnalysis(): MlRiskResult {
            val localEntities = dao.measurements("HEART_RATE", 10)
            if (localEntities.isEmpty()) {
                return MlRiskResult(0.0f, "Sin mediciones")
            }
            return riskEngine.analyze(localEntities.map { it.toDomain() })
        }

        private suspend fun fetchRemoteMeasurements() {
            try {
                val response = patientApi.getMeasurements(currentPatientId(), limit = 10)
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    data.forEach { dto ->
                        val type =
                            runCatching {
                                MetricType.valueOf(
                                    dto.metricType ?: dto.type?.uppercase() ?: "HEART_RATE",
                                )
                            }.getOrDefault(MetricType.HEART_RATE)
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
                val response = patientApi.getMedications(currentPatientId())
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val entities =
                        data.mapNotNull { dto ->
                            if (dto.id.isNullOrBlank() || dto.name.isNullOrBlank()) {
                                null
                            } else {
                                Medication(dto.id, dto.name, dto.dosage ?: dto.dose.orEmpty(), dto.schedule, dto.takenToday).toEntity()
                            }
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
                        val deviceId = dto.serialNumber ?: dto.deviceId ?: dto.id
                        if (deviceId.isNullOrBlank() || dto.model.isNullOrBlank()) return@forEach
                        val protocol =
                            runCatching {
                                DeviceProtocol.valueOf(
                                    dto.protocol ?: "GATT_STANDARD",
                                )
                            }.getOrDefault(DeviceProtocol.GATT_STANDARD)
                        dao.upsertDevice(
                            WearableDevice(
                                id = deviceId,
                                model = dto.model,
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
                val dto = response.body()
                if (response.isSuccessful && dto != null) {
                    val name = "${dto.firstName} ${dto.lastName ?: ""}".trim()
                    PatientSummary(dto.id, name, AlertStatus.NORMAL, null)
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
                    val response = caregiverApi.getRelationships()
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        val activeRelationships = data.filter { it.status.equals("active", ignoreCase = true) }
                        val remoteList = mutableListOf<PatientSummary>()
                        for (rel in activeRelationships) {
                            try {
                                val profileRes = caregiverApi.getPatientProfile(rel.patientId)
                                if (profileRes.isSuccessful && profileRes.body() != null) {
                                    val profile = profileRes.body()!!
                                    val name = "${profile.firstName} ${profile.lastName ?: ""}".trim()
                                    remoteList.add(PatientSummary(profile.id, name, AlertStatus.NORMAL, null))
                                }
                            } catch (_: Exception) {
                                // Perfil no disponible; se omite la relación en lugar de inventar datos.
                            }
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

private fun extractUserIdFromJwt(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size >= 2) {
            val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val json = String(payloadBytes, Charsets.UTF_8)
            val regex = Regex("\"(uid|sub|user_id)\"\\s*:\\s*\"([^\"]+)\"")
            regex.find(json)?.groupValues?.get(2)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun extractRoleFromJwt(token: String): Role {
    return try {
        val parts = token.split(".")
        if (parts.size >= 2) {
            val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val json = String(payloadBytes, Charsets.UTF_8)
            val regex = Regex("\"role\"\\s*:\\s*\"([^\"]+)\"")
            val roleStr = regex.find(json)?.groupValues?.get(1) ?: "patient"
            if (roleStr.equals("caregiver", ignoreCase = true)) Role.CAREGIVER else Role.PATIENT
        } else {
            Role.PATIENT
        }
    } catch (_: Exception) {
        Role.PATIENT
    }
}

private fun CoroutineScope.launchSilently(block: suspend () -> Unit) {
    launch(CoroutineExceptionHandler { _, _ -> }) { block() }
}
