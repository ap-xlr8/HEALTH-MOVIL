package com.healthos.domain.model

enum class Role { PATIENT, CAREGIVER }

enum class MetricType { HEART_RATE, SPO2, BLOOD_PRESSURE, TEMPERATURE }

enum class AlertStatus { NORMAL, ALERT, CRITICAL }

enum class DeviceProtocol { GATT_STANDARD, PROPRIETARY_XIAOMI, PROPRIETARY_GARMIN }

enum class SyncStatus { PENDING, SYNCED, FAILED }

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val role: Role,
)

data class UserProfile(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: Role,
)

data class HealthProfile(
    val weightKg: Double,
    val heightCm: Int,
    val bloodType: String,
)

data class Measurement(
    val id: String,
    val metricType: MetricType,
    val value: Double,
    val unit: String,
    val timestamp: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)

data class Medication(
    val id: String,
    val name: String,
    val dose: String,
    val schedule: String,
    val takenToday: Boolean,
)

data class Alert(
    val id: String,
    val title: String,
    val status: AlertStatus,
    val timestamp: String,
)

data class WearableDevice(
    val id: String,
    val model: String,
    val protocol: DeviceProtocol,
    val publicKey: String,
    val connected: Boolean,
)

data class PatientSummary(
    val id: String,
    val firstName: String,
    val status: AlertStatus,
    val latestMeasurement: Measurement?,
)

data class SosLocation(
    val lat: Double,
    val lng: Double,
)

data class MlRiskResult(
    val score: Float,
    val label: String,
)
