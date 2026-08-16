package com.healthos.domain.repository

import com.healthos.domain.model.Alert
import com.healthos.domain.model.Allergy
import com.healthos.domain.model.CaregiverProfile
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
    ): Boolean

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

    fun pendingSyncCount(): Flow<Int>

    fun healthProfile(): Flow<HealthProfile?>

    fun userProfile(): Flow<UserProfile?>

    suspend fun measurements(
        metric: String,
        days: Int,
    ): List<Measurement>

    suspend fun markMedicationTaken(id: String)

    suspend fun triggerSos(location: SosLocation): Alert

    suspend fun linkDevice(device: WearableDevice)

    suspend fun unlinkDevice(id: String)

    suspend fun updateDeviceTelemetry(id: String, batteryPercent: Int, rssi: Int)

    suspend fun saveBleMeasurement(value: Double)

    suspend fun saveBleMeasurementMulti(type: MetricType, value: Double, unit: String)

    suspend fun runPreventiveAnalysis(): MlRiskResult

    // --- Historia Clínica Integral ---
    fun allergies(): Flow<List<Allergy>>
    suspend fun addAllergy(allergy: Allergy)
    suspend fun deleteAllergy(id: String)

    fun pathologicalHistory(): Flow<List<PathologicalHistory>>
    suspend fun addPathologicalHistory(entry: PathologicalHistory)
    suspend fun deletePathologicalHistory(id: String)

    fun gynecoObstetricHistory(): Flow<GynecoObstetricHistory?>
    suspend fun saveGynecoObstetricHistory(history: GynecoObstetricHistory)

    fun familyHistory(): Flow<List<FamilyHistory>>
    suspend fun addFamilyHistory(entry: FamilyHistory)
    suspend fun deleteFamilyHistory(id: String)

    fun lifestyleHabits(): Flow<LifestyleHabits?>
    suspend fun saveLifestyleHabits(habits: LifestyleHabits)

    // --- Ajustes y Preferencias ---
    fun notificationPreferences(): Flow<NotificationPreferences>
    suspend fun saveNotificationPreferences(preferences: NotificationPreferences)

    fun dynamicSyncConfig(): Flow<DynamicSyncConfig>
    suspend fun saveDynamicSyncConfig(config: DynamicSyncConfig)

    suspend fun updatePatientProfile(
        firstName: String,
        lastName: String,
        phone: String,
        healthProfile: HealthProfile,
    )
}

interface CaregiverRepository {
    fun patients(): Flow<List<PatientSummary>>

    suspend fun patientDetail(id: String): PatientSummary?

    fun caregiverProfile(): Flow<CaregiverProfile?>

    suspend fun saveCaregiverProfile(profile: CaregiverProfile)
}

