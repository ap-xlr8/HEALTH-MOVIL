package com.healthos.data.remote

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

data class LoginResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "Bearer",
    @Json(name = "expires_in") val expiresIn: Int = 900,
)

data class RefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String,
)

data class VerifyEmailRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "code") val code: String,
)

data class ForgotPasswordRequestDto(
    @Json(name = "email") val email: String,
)

data class HealthProfileRequestDto(
    @Json(name = "weight_kg") val weightKg: Double,
    @Json(name = "height_cm") val heightCm: Int,
    @Json(name = "blood_type") val bloodType: String,
)

// --- DTOs Sincronización y Mediciones ---

data class SyncMeasurementsRequestDto(
    @Json(name = "measurements") val measurements: List<SyncMeasurementItemDto>,
)

data class SyncMeasurementItemDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "type") val type: String,
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "timestamp") val timestamp: String,
)

data class SyncMeasurementsResponseDto(
    @Json(name = "inserted_count") val insertedCount: Int = 0,
    @Json(name = "alerts_triggered") val alertsTriggered: Int = 0,
)

data class MeasurementDto(
    @Json(name = "id") val id: String,
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "metric_type") val metricType: String? = null,
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "timestamp") val timestamp: String,
)

// --- DTOs Medicamentos ---

data class MedicationDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "dosage") val dosage: String? = null,
    @Json(name = "dose") val dose: String? = null,
    @Json(name = "schedule") val schedule: String,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "taken_today") val takenToday: Boolean = false,
)

data class CreateMedicationRequestDto(
    @Json(name = "name") val name: String,
    @Json(name = "dosage") val dosage: String,
    @Json(name = "schedule") val schedule: String,
    @Json(name = "active") val active: Boolean = true,
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
)

// --- DTOs Cuidador y Pacientes ---

data class CaregiverPatientDto(
    @Json(name = "id") val id: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "status") val status: String? = "NORMAL",
    @Json(name = "latest_measurement") val latestMeasurement: MeasurementDto? = null,
)

// --- API Service Interfaces ---

interface AuthApiService {
    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponse<UserDto>>

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponse<LoginResponseDto>>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<ApiResponse<LoginResponseDto>>

    @POST("v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequestDto): Response<ApiResponse<Map<String, Any>>>

    @POST("v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): Response<ApiResponse<Map<String, Any>>>

    @PUT("v1/patients/me/health-profile")
    suspend fun saveHealthProfile(@Body request: HealthProfileRequestDto): Response<ApiResponse<Map<String, Any>>>
}

interface PatientApiService {
    @POST("v1/sync/measurements")
    suspend fun syncMeasurements(@Body request: SyncMeasurementsRequestDto): Response<ApiResponse<SyncMeasurementsResponseDto>>

    @GET("v1/patients/{id}/measurements")
    suspend fun getMeasurements(
        @Path("id") patientId: String,
        @Query("type") type: String? = null,
        @Query("days") days: Int? = null,
        @Query("limit") limit: Int? = 100,
    ): Response<ApiResponse<List<MeasurementDto>>>

    @GET("v1/patients/{id}/medications")
    suspend fun getMedications(@Path("id") patientId: String): Response<ApiResponse<List<MedicationDto>>>

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
    suspend fun getAlertDetail(@Path("id") alertId: String): Response<ApiResponse<AlertDto>>

    @POST("v1/alerts/{id}/acknowledge")
    suspend fun acknowledgeAlert(@Path("id") alertId: String): Response<ApiResponse<AlertDto>>

    @GET("v1/devices")
    suspend fun getDevices(): Response<ApiResponse<List<DeviceDto>>>

    @POST("v1/devices")
    suspend fun registerDevice(@Body request: DeviceDto): Response<ApiResponse<DeviceDto>>
}

interface CaregiverApiService {
    @GET("v1/caregiver/patients")
    suspend fun getPatients(): Response<ApiResponse<List<CaregiverPatientDto>>>

    @GET("v1/patients/{id}")
    suspend fun getPatientProfile(@Path("id") patientId: String): Response<ApiResponse<CaregiverPatientDto>>
}
