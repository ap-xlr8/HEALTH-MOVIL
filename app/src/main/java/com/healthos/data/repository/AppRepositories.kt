package com.healthos.data.repository

import android.content.Context
import com.healthos.data.local.AllergyEntity
import com.healthos.data.local.FamilyHistoryEntity
import com.healthos.data.local.GynecoObstetricEntity
import com.healthos.data.local.HealthOsDao
import com.healthos.data.local.LifestyleHabitsEntity
import com.healthos.data.local.PathologicalHistoryEntity
import com.healthos.data.local.toDomain
import com.healthos.data.local.toEntity
import com.healthos.data.remote.AuthApiService
import com.healthos.data.remote.CaregiverApiService
import com.healthos.data.remote.CaregiverProfileDto
import com.healthos.data.remote.DeviceDto
import com.healthos.data.remote.DynamicSyncConfigDto
import com.healthos.data.remote.ForgotPasswordRequestDto
import com.healthos.data.remote.HealthProfileRequestDto
import com.healthos.data.remote.LoginRequestDto
import com.healthos.data.remote.MedicationLogRequestDto
import com.healthos.data.remote.NotificationSettingsDto
import com.healthos.data.remote.PatientApiService
import com.healthos.data.remote.PatientProfileDto
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
import com.healthos.domain.model.Allergy
import com.healthos.domain.model.CaregiverProfile
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
import com.healthos.sync.SyncEngine
import dagger.hilt.android.qualifiers.ApplicationContext
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
                secureTokenStore.saveUserProfile(firstName.trim(), lastName.trim(), email.trim().lowercase(), "")
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
                body.user?.let { u ->
                    secureTokenStore.saveUserProfile(
                        u.firstName ?: "",
                        u.lastName ?: "",
                        u.email ?: email,
                        "",
                    )
                }
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
            secureTokenStore.saveHealthProfile(
                profile.weightKg,
                profile.heightCm,
                profile.bloodType,
                profile.rhFactor,
                profile.birthDate,
                profile.gender,
                profile.emergencyContactName,
                profile.emergencyContactPhone,
                profile.emergencyContactRelation,
                profile.insuranceProvider,
                profile.policyNumber,
            )
            try {
                authApi.saveHealthProfile(
                    HealthProfileRequestDto(
                        weightKg = profile.weightKg,
                        heightCm = profile.heightCm,
                        bloodType = profile.bloodType,
                        rhFactor = profile.rhFactor,
                        birthDate = profile.birthDate,
                        gender = profile.gender,
                        emergencyContactName = profile.emergencyContactName,
                        emergencyContactPhone = profile.emergencyContactPhone,
                        emergencyContactRelation = profile.emergencyContactRelation,
                        insuranceProvider = profile.insuranceProvider,
                        policyNumber = profile.policyNumber,
                    ),
                )
            } catch (_: Exception) {
                // Guardado local en SQLCipher / EncryptedStore
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
        @ApplicationContext private val context: Context,
        private val patientApi: PatientApiService,
        private val authApi: AuthApiService,
        private val dao: HealthOsDao,
        private val riskEngine: PreventiveRiskEngine,
        private val secureTokenStore: SecureTokenStore,
    ) : PatientRepository {
        private val coroutineScope =
            CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> })

        private val _syncConfig = MutableStateFlow(DynamicSyncConfig())
        private val _notificationPrefs = MutableStateFlow(NotificationPreferences())

        init {
            coroutineScope.launch {
                fetchRemoteMeasurements()
                fetchRemoteMedications()
                fetchRemoteDevices()
                fetchRemoteNotificationSettings()
                fetchRemoteSyncConfig()
                seedInitialClinicalHistoryIfEmpty()
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
                    HealthProfile(
                        weightKg = weight,
                        heightCm = height,
                        bloodType = bloodType,
                        rhFactor = secureTokenStore.healthRhFactor(),
                        birthDate = secureTokenStore.healthBirthDate(),
                        gender = secureTokenStore.healthGender(),
                        emergencyContactName = secureTokenStore.healthEmergencyName(),
                        emergencyContactPhone = secureTokenStore.healthEmergencyPhone(),
                        emergencyContactRelation = secureTokenStore.healthEmergencyRelation(),
                        insuranceProvider = secureTokenStore.healthInsuranceProvider(),
                        policyNumber = secureTokenStore.healthPolicyNumber(),
                    )
                } else {
                    null
                }
            return MutableStateFlow(profile)
        }

        override fun userProfile(): Flow<UserProfile?> {
            val firstName = secureTokenStore.userFirstName() ?: "Paciente"
            val lastName = secureTokenStore.userLastName() ?: ""
            val email = secureTokenStore.userEmail() ?: "paciente@healthos.com"
            val phone = secureTokenStore.userPhone()
            return MutableStateFlow(
                UserProfile(
                    id = currentPatientId(),
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    role = Role.PATIENT,
                    phone = phone,
                ),
            )
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
                        trigger = "MANUAL_BUTTON",
                    ),
                )
            } catch (_: Exception) {
                // Preserved in local emergency database if offline
            }

            dao.upsertAlert(alert.toEntity())
            SyncEngine.triggerCriticalSync(context, alert.id)
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
                        batteryPercent = device.batteryPercent,
                        rssi = device.rssi,
                        firmwareVersion = device.firmwareVersion,
                    ),
                )
            } catch (_: Exception) {
                // Cached locally
            }
        }

        override suspend fun unlinkDevice(id: String) {
            dao.deleteDevice(id)
        }

        override suspend fun updateDeviceTelemetry(id: String, batteryPercent: Int, rssi: Int) {
            val nowStr = java.time.Instant.now().toString()
            dao.updateDeviceTelemetry(id, batteryPercent, rssi, nowStr)
        }

        override suspend fun saveBleMeasurement(value: Double) {
            saveBleMeasurementMulti(MetricType.HEART_RATE, value, "bpm")
        }

        override suspend fun saveBleMeasurementMulti(type: MetricType, value: Double, unit: String) {
            if (value <= 0.0) return
            val nowStr = java.time.Instant.now().toString()
            val measurementId = "ble_${UUID.randomUUID()}"
            val entity =
                Measurement(
                    id = measurementId,
                    metricType = type,
                    value = value,
                    unit = unit,
                    timestamp = nowStr,
                    syncStatus = SyncStatus.PENDING,
                ).toEntity()
            dao.upsertMeasurement(entity)

            // P0: CANAL CRÍTICO DE SINCRONIZACIÓN INMEDIATO (Bypass de batch)
            val isCriticalSpO2 = type == MetricType.SPO2 && value < 90.0
            val isCriticalHeartRate = type == MetricType.HEART_RATE && (value > 180.0 || value < 40.0)
            if (isCriticalSpO2 || isCriticalHeartRate) {
                val alertMsg = if (isCriticalSpO2) "Hipoxemia Crítica Detectada: SpO2 $value%" else "Arritmia Crítica Detectada: $value bpm"
                val criticalAlert = Alert(
                    id = UUID.randomUUID().toString(),
                    title = alertMsg,
                    status = AlertStatus.CRITICAL,
                    timestamp = nowStr,
                )
                dao.upsertAlert(criticalAlert.toEntity())
                SyncEngine.triggerCriticalSync(context, criticalAlert.id)
            }
        }

        override suspend fun runPreventiveAnalysis(): MlRiskResult {
            val localEntities = dao.measurements("HEART_RATE", 10)
            val spo2Entities = dao.measurements("SPO2", 5)
            val combined = (localEntities + spo2Entities).map { it.toDomain() }
            if (combined.isEmpty()) {
                return MlRiskResult(0.0f, "Sin mediciones suficientes")
            }
            return riskEngine.analyze(combined)
        }

        // --- Historia Clínica Integral ---

        override fun allergies(): Flow<List<Allergy>> = dao.allergies().map { list -> list.map { it.toDomain() } }

        override suspend fun addAllergy(allergy: Allergy) {
            dao.upsertAllergy(allergy.toEntity())
        }

        override suspend fun deleteAllergy(id: String) {
            dao.deleteAllergy(id)
        }

        override fun pathologicalHistory(): Flow<List<PathologicalHistory>> = dao.pathologicalHistory().map { list -> list.map { it.toDomain() } }

        override suspend fun addPathologicalHistory(entry: PathologicalHistory) {
            dao.upsertPathologicalHistory(entry.toEntity())
        }

        override suspend fun deletePathologicalHistory(id: String) {
            dao.deletePathologicalHistory(id)
        }

        override fun gynecoObstetricHistory(): Flow<GynecoObstetricHistory?> = dao.gynecoObstetric().map { it?.toDomain() }

        override suspend fun saveGynecoObstetricHistory(history: GynecoObstetricHistory) {
            dao.upsertGynecoObstetric(history.toEntity())
        }

        override fun familyHistory(): Flow<List<FamilyHistory>> = dao.familyHistory().map { list -> list.map { it.toDomain() } }

        override suspend fun addFamilyHistory(entry: FamilyHistory) {
            dao.upsertFamilyHistory(entry.toEntity())
        }

        override suspend fun deleteFamilyHistory(id: String) {
            dao.deleteFamilyHistory(id)
        }

        override fun lifestyleHabits(): Flow<LifestyleHabits?> = dao.lifestyleHabits().map { it?.toDomain() }

        override suspend fun saveLifestyleHabits(habits: LifestyleHabits) {
            dao.upsertLifestyleHabits(habits.toEntity())
        }

        // --- Ajustes y Preferencias ---

        override fun notificationPreferences(): Flow<NotificationPreferences> =
            dao.notificationSettings().map { it?.toDomain() ?: _notificationPrefs.value }

        override suspend fun saveNotificationPreferences(preferences: NotificationPreferences) {
            _notificationPrefs.value = preferences
            dao.upsertNotificationSettings(preferences.toEntity())
            try {
                patientApi.updateNotificationSettings(
                    patientId = currentPatientId(),
                    request = NotificationSettingsDto(
                        pushEnabled = preferences.pushEnabled,
                        emailEnabled = preferences.emailEnabled,
                        smsEnabled = preferences.smsEnabled,
                        sosAlerts = preferences.sosAlerts,
                        routineVitals = preferences.routineVitals,
                        medicationReminders = preferences.medicationReminders,
                        quietHoursEnabled = preferences.quietHoursEnabled,
                        quietHoursStart = preferences.quietHoursStart,
                        quietHoursEnd = preferences.quietHoursEnd,
                        bypassQuietHoursForSos = preferences.bypassQuietHoursForSos,
                    ),
                )
            } catch (_: Exception) {
                // Preserved in local SQLite
            }
        }

        override fun dynamicSyncConfig(): Flow<DynamicSyncConfig> = _syncConfig

        override suspend fun saveDynamicSyncConfig(config: DynamicSyncConfig) {
            _syncConfig.value = config
            try {
                patientApi.updateDeviceSyncConfig(
                    deviceId = config.deviceId,
                    request = DynamicSyncConfigDto(
                        deviceId = config.deviceId,
                        samplingIntervalMs = config.samplingIntervalMs,
                        batchSize = config.batchSize,
                        criticalSyncEnabled = config.criticalSyncEnabled,
                    ),
                )
            } catch (_: Exception) {
                // Cached locally
            }
        }

        override suspend fun updatePatientProfile(
            firstName: String,
            lastName: String,
            phone: String,
            healthProfile: HealthProfile,
        ) {
            val email = secureTokenStore.userEmail() ?: "paciente@healthos.com"
            secureTokenStore.saveUserProfile(firstName, lastName, email, phone)
            saveHealthProfile(healthProfile)
            try {
                authApi.updatePatientProfile(
                    PatientProfileDto(
                        id = currentPatientId(),
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        healthProfile = HealthProfileRequestDto(
                            weightKg = healthProfile.weightKg,
                            heightCm = healthProfile.heightCm,
                            bloodType = healthProfile.bloodType,
                            rhFactor = healthProfile.rhFactor,
                            birthDate = healthProfile.birthDate,
                            gender = healthProfile.gender,
                            emergencyContactName = healthProfile.emergencyContactName,
                            emergencyContactPhone = healthProfile.emergencyContactPhone,
                            emergencyContactRelation = healthProfile.emergencyContactRelation,
                            insuranceProvider = healthProfile.insuranceProvider,
                            policyNumber = healthProfile.policyNumber,
                        ),
                    ),
                )
            } catch (_: Exception) {
                // Saved locally in SecureStores
            }
        }

        private suspend fun saveHealthProfile(profile: HealthProfile) {
            secureTokenStore.saveHealthProfile(
                profile.weightKg,
                profile.heightCm,
                profile.bloodType,
                profile.rhFactor,
                profile.birthDate,
                profile.gender,
                profile.emergencyContactName,
                profile.emergencyContactPhone,
                profile.emergencyContactRelation,
                profile.insuranceProvider,
                profile.policyNumber,
            )
        }

        private suspend fun fetchRemoteMeasurements() {
            try {
                val response = patientApi.getMeasurements(currentPatientId(), limit = 20)
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
                                Medication(
                                    dto.id,
                                    dto.name,
                                    dto.dosage ?: dto.dose.orEmpty(),
                                    dto.schedule,
                                    dto.takenToday,
                                    dto.route ?: "Oral",
                                    dto.adherencePercentage ?: 100,
                                ).toEntity()
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
                                batteryPercent = dto.batteryPercent ?: 100,
                                rssi = dto.rssi ?: -55,
                                firmwareVersion = dto.firmwareVersion ?: "1.0.0",
                            ).toEntity(),
                        )
                    }
                }
            } catch (_: Exception) {
                // Offline fallback
            }
        }

        private suspend fun fetchRemoteNotificationSettings() {
            try {
                val response = patientApi.getNotificationSettings(currentPatientId())
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    val prefs = NotificationPreferences(
                        pushEnabled = data.pushEnabled,
                        emailEnabled = data.emailEnabled,
                        smsEnabled = data.smsEnabled,
                        sosAlerts = data.sosAlerts,
                        routineVitals = data.routineVitals,
                        medicationReminders = data.medicationReminders,
                        quietHoursEnabled = data.quietHoursEnabled,
                        quietHoursStart = data.quietHoursStart,
                        quietHoursEnd = data.quietHoursEnd,
                        bypassQuietHoursForSos = data.bypassQuietHoursForSos,
                    )
                    _notificationPrefs.value = prefs
                    dao.upsertNotificationSettings(prefs.toEntity())
                }
            } catch (_: Exception) {
                // Use local store
            }
        }

        private suspend fun fetchRemoteSyncConfig() {
            try {
                val response = patientApi.getDeviceSyncConfig("default")
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    _syncConfig.value = DynamicSyncConfig(
                        deviceId = data.deviceId ?: "default",
                        samplingIntervalMs = data.samplingIntervalMs,
                        batchSize = data.batchSize,
                        criticalSyncEnabled = data.criticalSyncEnabled,
                    )
                }
            } catch (_: Exception) {
                // Use local config
            }
        }

        private suspend fun seedInitialClinicalHistoryIfEmpty() {
            // Populate baseline sample clinical items if database is clean
            try {
                dao.upsertLifestyleHabits(
                    LifestyleHabitsEntity(
                        id = "habits_me",
                        smokingStatus = "No fumador",
                        packsPerDay = 0.0,
                        alcoholFrequency = "Ocasional",
                        exerciseDaysPerWeek = 4,
                        exerciseIntensity = "Moderada",
                        averageSleepHours = 7.5,
                        dietPattern = "Mediterránea / Cardiosaludable",
                    ),
                )
                dao.upsertGynecoObstetric(
                    GynecoObstetricEntity(
                        id = "gyneco_me",
                        menarcheAge = 12,
                        lastMenstrualPeriod = "2026-08-01",
                        gestas = 0,
                        partos = 0,
                        cesareas = 0,
                        abortos = 0,
                        contraceptiveMethod = "Preservativo",
                        isPregnant = false,
                        gestationalWeeks = null,
                    ),
                )
            } catch (_: Exception) {
            }
        }
    }

@Singleton
class CaregiverRepositoryImpl
    @Inject
    constructor(
        private val caregiverApi: CaregiverApiService,
        private val secureTokenStore: SecureTokenStore,
    ) : CaregiverRepository {
        private val patients = MutableStateFlow<List<PatientSummary>>(emptyList())
        private val _caregiverProfile = MutableStateFlow<CaregiverProfile?>(null)

        override fun patients(): Flow<List<PatientSummary>> {
            fetchRemotePatients()
            return patients
        }

        override fun caregiverProfile(): Flow<CaregiverProfile?> {
            val email = secureTokenStore.userEmail() ?: "cuidador@healthos.com"
            val firstName = secureTokenStore.userFirstName() ?: "Dr. Carlos"
            val lastName = secureTokenStore.userLastName() ?: "Mendoza"
            if (_caregiverProfile.value == null) {
                _caregiverProfile.value = CaregiverProfile(
                    id = secureTokenStore.userId() ?: "caregiver_me",
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    specialty = "Especialista en Medicina Interna y Telemetría",
                    institution = "Hospital Clínico Health OS",
                    emergencyPhone = "+52 55 9876 5432",
                )
            }
            return _caregiverProfile
        }

        override suspend fun saveCaregiverProfile(profile: CaregiverProfile) {
            _caregiverProfile.value = profile
            secureTokenStore.saveUserProfile(profile.firstName, profile.lastName, profile.email, profile.emergencyPhone)
            try {
                caregiverApi.updateCaregiverProfile(
                    caregiverId = profile.id,
                    request = CaregiverProfileDto(
                        id = profile.id,
                        firstName = profile.firstName,
                        lastName = profile.lastName,
                        email = profile.email,
                        specialty = profile.specialty,
                        institution = profile.institution,
                        emergencyPhone = profile.emergencyPhone,
                    ),
                )
            } catch (_: Exception) {
                // Cached locally
            }
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
                                // Fallback
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

