package com.healthos.data.repository

import com.healthos.data.local.HealthOsDao
import com.healthos.data.local.toDomain
import com.healthos.data.local.toEntity
import com.healthos.data.remote.MockBackendDataSource
import com.healthos.domain.model.Alert
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.model.Role
import com.healthos.domain.model.Session
import com.healthos.domain.model.SosLocation
import com.healthos.domain.model.UserProfile
import com.healthos.domain.model.WearableDevice
import com.healthos.domain.repository.AuthRepository
import com.healthos.domain.repository.CaregiverRepository
import com.healthos.domain.repository.PatientRepository
import com.healthos.mlruntime.PreventiveRiskEngine
import com.healthos.security.SecureTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val secureTokenStore: SecureTokenStore,
        private val mockBackend: MockBackendDataSource,
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
            return mockBackend.register(email, role, firstName, lastName)
        }

        override suspend fun login(
            email: String,
            password: String,
        ): Session {
            require(email.contains("@")) { "Correo inválido." }
            require(password.isNotBlank()) { "Contraseña requerida." }
            val session = mockBackend.login(email)
            secureTokenStore.save(session.accessToken, session.refreshToken, session.role.name)
            _session.value = session
            return session
        }

        override suspend fun verifyEmail(
            email: String,
            code: String,
        ) = email.contains("@") && code.length == 6

        override suspend fun forgotPassword(email: String) = email.contains("@")

        override suspend fun saveHealthProfile(profile: HealthProfile) {
            require(profile.weightKg > 0 && profile.heightCm > 0 && profile.bloodType.isNotBlank())
        }

        override suspend fun logout() {
            secureTokenStore.clear()
            _session.value = null
        }

        private fun loadSession(): Session? {
            val access = secureTokenStore.accessToken() ?: return null
            val refresh = secureTokenStore.refreshToken() ?: return null
            val role = secureTokenStore.role()?.let { Role.valueOf(it) } ?: return null
            return Session(access, refresh, role)
        }
    }

@Singleton
class PatientRepositoryImpl
    @Inject
    constructor(
        private val dao: HealthOsDao,
        private val mockBackend: MockBackendDataSource,
        private val riskEngine: PreventiveRiskEngine,
    ) : PatientRepository {
        override fun latestMeasurements(): Flow<List<Measurement>> =
            dao.latestMeasurements().map { rows ->
                if (rows.isEmpty()) mockBackend.seedMeasurements() else rows.map { it.toDomain() }
            }

        override fun medications() =
            dao.medications().map { rows ->
                if (rows.isEmpty()) mockBackend.medications() else rows.map { it.toDomain() }
            }

        override fun activeAlerts() =
            dao.alerts().map { rows ->
                rows.map { it.toDomain() }
            }

        override fun devices() =
            dao.devices().map { rows ->
                if (rows.isEmpty()) mockBackend.devices() else rows.map { it.toDomain() }
            }

        override suspend fun measurements(
            metric: String,
            days: Int,
        ): List<Measurement> {
            val limit = (days.coerceAtLeast(1) * 24).coerceAtMost(500)
            return dao.measurements(metric, limit).map { it.toDomain() }.ifEmpty { mockBackend.seedMeasurements() }
        }

        override suspend fun markMedicationTaken(id: String) {
            dao.upsertMedications(mockBackend.medications().map { it.toEntity() })
            dao.markMedicationTaken(id)
        }

        override suspend fun triggerSos(location: SosLocation): Alert {
            require(location.lat in -90.0..90.0 && location.lng in -180.0..180.0)
            val alert = mockBackend.sosAlert()
            dao.upsertAlert(alert.toEntity())
            return alert
        }

        override suspend fun linkDevice(device: WearableDevice) = dao.upsertDevice(device.toEntity())

        override suspend fun unlinkDevice(id: String) = dao.deleteDevice(id)

        override suspend fun runPreventiveAnalysis() = riskEngine.analyze(mockBackend.seedMeasurements())
    }

@Singleton
class CaregiverRepositoryImpl
    @Inject
    constructor(
        private val mockBackend: MockBackendDataSource,
    ) : CaregiverRepository {
        private val patients = MutableStateFlow(mockBackend.patients())

        override fun patients(): Flow<List<PatientSummary>> = patients

        override suspend fun patientDetail(id: String) = patients.value.firstOrNull { it.id == id }
    }
