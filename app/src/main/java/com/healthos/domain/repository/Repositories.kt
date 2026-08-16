package com.healthos.domain.repository

import com.healthos.domain.model.Alert
import com.healthos.domain.model.HealthProfile
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MlRiskResult
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.model.Role
import com.healthos.domain.model.Session
import com.healthos.domain.model.SosLocation
import com.healthos.domain.model.UserProfile
import com.healthos.domain.model.WearableDevice
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<Session?>

    suspend fun register(
        email: String,
        password: String,
        role: Role,
        firstName: String,
        lastName: String,
    ): UserProfile

    suspend fun login(
        email: String,
        password: String,
    ): Session

    suspend fun verifyEmail(
        email: String,
        code: String,
    ): Boolean

    suspend fun verify2FA(
        email: String,
        code: String,
    ): Session

    suspend fun resend2FA(email: String): Boolean

    suspend fun forgotPassword(email: String): Boolean

    suspend fun saveHealthProfile(profile: HealthProfile)

    suspend fun logout()
}

interface PatientRepository {
    fun latestMeasurements(): Flow<List<Measurement>>

    fun medications(): Flow<List<Medication>>

    fun activeAlerts(): Flow<List<Alert>>

    fun devices(): Flow<List<WearableDevice>>

    suspend fun measurements(
        metric: String,
        days: Int,
    ): List<Measurement>

    suspend fun markMedicationTaken(id: String)

    suspend fun triggerSos(location: SosLocation): Alert

    suspend fun linkDevice(device: WearableDevice)

    suspend fun unlinkDevice(id: String)

    suspend fun runPreventiveAnalysis(): MlRiskResult
}

interface CaregiverRepository {
    fun patients(): Flow<List<PatientSummary>>

    suspend fun patientDetail(id: String): PatientSummary?
}
