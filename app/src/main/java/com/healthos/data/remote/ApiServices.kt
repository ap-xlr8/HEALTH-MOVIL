package com.healthos.data.remote

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// --- Envelope Estándar Backend ---

data class ApiResponse<T>(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: ApiErrorDto? = null,
)

data class ApiErrorDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null,
)

// --- DTOs Auth ---

data class RegisterRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "role") val role: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "health_profile") val healthProfile: HealthProfileRequestDto? = null,
    @Json(name = "active_conditions") val activeConditions: List<String>? = null,
)

data class RegisterResponseDataDto(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "message") val message: String? = null,
)

data class UserDto(
    @Json(name = "id") val id: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "role") val role: String?,
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?,
    @Json(name = "created_at") val createdAt: String? = null,
)

data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)

data class LoginChallengeDto(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "expires_in") val expiresIn: Int? = null,
)

data class LoginEnvelopeDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: LoginChallengeDto? = null,
)

data class LoginResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "Bearer",
    @Json(name = "expires_in") val expiresIn: Int = 900,
    @Json(name = "role") val role: String? = null,
    @Json(name = "user") val user: UserDto? = null,
)

data class RefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String,
)

data class VerifyEmailTokenDto(
    @Json(name = "token") val token: String,
)

data class TwoFactorVerifyRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "code") val code: String,
)

data class TwoFactorResendRequestDto(
    @Json(name = "email") val email: String,
)

data class VerifyEmailResponseDataDto(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "message") val message: String? = null,
)

data class ForgotPasswordRequestDto(
    @Json(name = "email") val email: String,
)

data class HealthProfileRequestDto(
    @Json(name = "weight_kg") val weightKg: Double,
    @Json(name = "height_cm") val heightCm: Int,
    @Json(name = "blood_type") val bloodType: String,
    @Json(name = "rh_factor") val rhFactor: String? = "+",
    @Json(name = "birth_date") val birthDate: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "emergency_contact_name") val emergencyContactName: String? = null,
    @Json(name = "emergency_contact_phone") val emergencyContactPhone: String? = null,
    @Json(name = "emergency_contact_relation") val emergencyContactRelation: String? = null,
    @Json(name = "insurance_provider") val insuranceProvider: String? = null,
    @Json(name = "policy_number") val policyNumber: String? = null,
)

// --- DTOs Historia Clínica Integral ---

data class AllergyDto(
    @Json(name = "id") val id: String,
    @Json(name = "allergen") val allergen: String,
    @Json(name = "category") val category: String,
    @Json(name = "severity") val severity: String,
    @Json(name = "manifestations") val manifestations: String,
    @Json(name = "diagnosed_date") val diagnosedDate: String,
)

data class PathologicalHistoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "title") val title: String,
    @Json(name = "icd10_code") val icd10Code: String? = null,
    @Json(name = "year_or_date") val yearOrDate: String,
    @Json(name = "notes") val notes: String? = "",
)

data class GynecoObstetricDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "menarche_age") val menarcheAge: Int? = null,
    @Json(name = "last_menstrual_period") val lastMenstrualPeriod: String? = null,
    @Json(name = "gestas") val gestas: Int = 0,
    @Json(name = "partos") val partos: Int = 0,
    @Json(name = "cesareas") val cesareas: Int = 0,
    @Json(name = "abortos") val abortos: Int = 0,
    @Json(name = "contraceptive_method") val contraceptiveMethod: String = "Ninguno",
    @Json(name = "is_pregnant") val isPregnant: Boolean = false,
    @Json(name = "gestational_weeks") val gestationalWeeks: Int? = null,
)

data class FamilyHistoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "kinship") val kinship: String,
    @Json(name = "condition_name") val conditionName: String,
    @Json(name = "category") val category: String = "Cardiovascular",
    @Json(name = "notes") val notes: String? = "",
)

data class LifestyleHabitsDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "smoking_status") val smokingStatus: String = "No fumador",
    @Json(name = "packs_per_day") val packsPerDay: Double = 0.0,
    @Json(name = "alcohol_frequency") val alcoholFrequency: String = "Ocasional",
    @Json(name = "exercise_days_per_week") val exerciseDaysPerWeek: Int = 3,
    @Json(name = "exercise_intensity") val exerciseIntensity: String = "Moderada",
    @Json(name = "average_sleep_hours") val averageSleepHours: Double = 7.5,
    @Json(name = "diet_pattern") val dietPattern: String = "Balanceada",
)

data class NotificationSettingsDto(
    @Json(name = "push_enabled") val pushEnabled: Boolean = true,
    @Json(name = "email_enabled") val emailEnabled: Boolean = true,
    @Json(name = "sms_enabled") val smsEnabled: Boolean = false,
    @Json(name = "sos_alerts") val sosAlerts: Boolean = true,
    @Json(name = "routine_vitals") val routineVitals: Boolean = true,
    @Json(name = "medication_reminders") val medicationReminders: Boolean = true,
    @Json(name = "quiet_hours_enabled") val quietHoursEnabled: Boolean = false,
    @Json(name = "quiet_hours_start") val quietHoursStart: String = "22:00",
    @Json(name = "quiet_hours_end") val quietHoursEnd: String = "07:00",
    @Json(name = "bypass_quiet_hours_for_sos") val bypassQuietHoursForSos: Boolean = true,
)

data class DynamicSyncConfigDto(
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "sampling_interval_ms") val samplingIntervalMs: Long = 1000L,
    @Json(name = "batch_size") val batchSize: Int = 20,
    @Json(name = "critical_sync_enabled") val criticalSyncEnabled: Boolean = true,
)

data class CaregiverProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    @Json(name = "email") val email: String,
    @Json(name = "specialty") val specialty: String? = null,
    @Json(name = "institution") val institution: String? = null,
    @Json(name = "emergency_phone") val emergencyPhone: String? = null,
)

// --- DTOs Sincronización y Mediciones ---

data class SyncMeasurementsRequestDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "data") val data: List<SyncMeasurementItemDto>,
)

data class SyncMeasurementItemDto(
    @Json(name = "type") val type: String,
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "timestamp") val timestamp: String,
)

data class SyncMeasurementsResponseDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "synced_count") val syncedCount: Int = 0,
    @Json(name = "alerts_triggered") val alertsTriggered: List<String> = emptyList(),
)

data class MeasurementDto(
    @Json(name = "id") val id: String,
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "metric_type") val metricType: String? = null,
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "created_at") val createdAt: String? = null,
)

// --- DTOs Medicamentos ---

data class MedicationDto(
    @Json(name = "id") val id: String,
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "dosage") val dosage: String? = null,
    @Json(name = "dose") val dose: String? = null,
    @Json(name = "schedule") val schedule: String,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "taken_today") val takenToday: Boolean = false,
    @Json(name = "route") val route: String? = "Oral",
    @Json(name = "adherence_percentage") val adherencePercentage: Int? = 100,
)

data class CreateMedicationRequestDto(
    @Json(name = "name") val name: String,
    @Json(name = "dosage") val dosage: String,
    @Json(name = "schedule") val schedule: String,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "route") val route: String? = "Oral",
)

data class MedicationLogRequestDto(
    @Json(name = "medication_id") val medicationId: String,
    @Json(name = "status") val status: String = "taken",
    @Json(name = "taken_at") val takenAt: String? = null,
)

// --- DTOs Alertas ---

data class AlertDto(
    @Json(name = "id") val id: String,
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "severity") val severity: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "measurement_ref") val measurementRef: String? = null,
    @Json(name = "acknowledged") val acknowledged: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
)

data class SosLocationDto(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lng") val lng: Double,
)

data class SosAlertRequestDto(
    @Json(name = "location") val location: SosLocationDto,
    @Json(name = "trigger") val trigger: String = "MANUAL_BUTTON",
)

// --- DTOs Dispositivos ---

data class DeviceDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "serial_number") val serialNumber: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "protocol") val protocol: String? = null,
    @Json(name = "public_key") val publicKey: String? = null,
    @Json(name = "connected") val connected: Boolean = false,
    @Json(name = "battery_percent") val batteryPercent: Int? = 100,
    @Json(name = "rssi") val rssi: Int? = -55,
    @Json(name = "firmware_version") val firmwareVersion: String? = "1.0.0",
)

// --- DTOs Cuidador y Pacientes ---

data class PatientProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "health_profile") val healthProfile: HealthProfileRequestDto? = null,
    @Json(name = "active_conditions") val activeConditions: List<String>? = null,
)

data class CaregiverPatientDto(
    @Json(name = "id") val id: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "status") val status: String? = "NORMAL",
    @Json(name = "latest_measurement") val latestMeasurement: MeasurementDto? = null,
)

data class RelationshipDto(
    @Json(name = "id") val id: String,
    @Json(name = "patient_id") val patientId: String,
    @Json(name = "caregiver_id") val caregiverId: String,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
)

// --- API Service Interfaces ---

interface AuthApiService {
    @POST("v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto,
    ): Response<ApiResponse<RegisterResponseDataDto>>

    @POST("v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): Response<LoginEnvelopeDto>

    @POST("v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto,
    ): Response<LoginResponseDto>

    @POST("v1/auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailTokenDto,
    ): Response<ApiResponse<VerifyEmailResponseDataDto>>

    @POST("v1/auth/2fa/verify")
    suspend fun verify2FA(
        @Body request: TwoFactorVerifyRequestDto,
    ): Response<LoginResponseDto>

    @POST("v1/auth/2fa/resend")
    suspend fun resend2FA(
        @Body request: TwoFactorResendRequestDto,
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("v1/auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    @POST("v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto,
    ): Response<ApiResponse<Map<String, Any>>>

    @PUT("v1/patients/me/health-profile")
    suspend fun saveHealthProfile(
        @Body request: HealthProfileRequestDto,
    ): Response<ApiResponse<Map<String, Any>>>

    @PUT("v1/patients/me/profile")
    suspend fun updatePatientProfile(
        @Body request: PatientProfileDto,
    ): Response<ApiResponse<PatientProfileDto>>
}

interface PatientApiService {
    @POST("v1/alerts/sos")
    suspend fun triggerSosAlert(
        @Body request: SosAlertRequestDto,
    ): Response<AlertDto>

    @POST("v1/sync/measurements")
    suspend fun syncMeasurements(
        @Body request: SyncMeasurementsRequestDto,
    ): Response<SyncMeasurementsResponseDto>

    @GET("v1/patients/{id}/measurements")
    suspend fun getMeasurements(
        @Path("id") patientId: String,
        @Query("type") type: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int? = 100,
    ): Response<ApiResponse<List<MeasurementDto>>>

    @GET("v1/patients/{id}")
    suspend fun getPatient(
        @Path("id") patientId: String,
    ): Response<PatientProfileDto>

    @GET("v1/patients/{id}/medications")
    suspend fun getMedications(
        @Path("id") patientId: String,
    ): Response<ApiResponse<List<MedicationDto>>>

    @POST("v1/patients/{id}/medications")
    suspend fun addMedication(
        @Path("id") patientId: String,
        @Body request: CreateMedicationRequestDto,
    ): Response<ApiResponse<MedicationDto>>

    @POST("v1/patients/{id}/medication-logs")
    suspend fun logMedication(
        @Path("id") patientId: String,
        @Body request: MedicationLogRequestDto,
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("v1/alerts/{id}")
    suspend fun getAlertDetail(
        @Path("id") alertId: String,
    ): Response<AlertDto>

    @POST("v1/alerts/{id}/acknowledge")
    suspend fun acknowledgeAlert(
        @Path("id") alertId: String,
    ): Response<AlertDto>

    @GET("v1/devices")
    suspend fun getDevices(): Response<ApiResponse<List<DeviceDto>>>

    @POST("v1/devices")
    suspend fun registerDevice(
        @Body request: DeviceDto,
    ): Response<ApiResponse<DeviceDto>>

    @GET("v1/devices/{id}/sync-config")
    suspend fun getDeviceSyncConfig(
        @Path("id") deviceId: String,
    ): Response<ApiResponse<DynamicSyncConfigDto>>

    @PUT("v1/devices/{id}/sync-config")
    suspend fun updateDeviceSyncConfig(
        @Path("id") deviceId: String,
        @Body request: DynamicSyncConfigDto,
    ): Response<ApiResponse<DynamicSyncConfigDto>>

    @GET("v1/patients/{id}/notification-settings")
    suspend fun getNotificationSettings(
        @Path("id") patientId: String,
    ): Response<ApiResponse<NotificationSettingsDto>>

    @PUT("v1/patients/{id}/notification-settings")
    suspend fun updateNotificationSettings(
        @Path("id") patientId: String,
        @Body request: NotificationSettingsDto,
    ): Response<ApiResponse<NotificationSettingsDto>>
}

interface CaregiverApiService {
    @GET("v1/relationships")
    suspend fun getRelationships(): Response<ApiResponse<List<RelationshipDto>>>

    @GET("v1/patients/{id}")
    suspend fun getPatientProfile(
        @Path("id") patientId: String,
    ): Response<PatientProfileDto>

    @PUT("v1/caregivers/{id}/profile")
    suspend fun updateCaregiverProfile(
        @Path("id") caregiverId: String,
        @Body request: CaregiverProfileDto,
    ): Response<ApiResponse<CaregiverProfileDto>>
}

