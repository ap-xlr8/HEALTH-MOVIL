package com.healthos.domain.model

enum class Role { PATIENT, CAREGIVER }

enum class MetricType {
    HEART_RATE,
    SPO2,
    BLOOD_PRESSURE,
    BLOOD_PRESSURE_SYSTOLIC,
    BLOOD_PRESSURE_DIASTOLIC,
    TEMPERATURE,
    SKIN_TEMPERATURE,
    EDA,
    PTT,
    HRV_RMSSD,
    HRV_SDNN,
    GLUCOSE,
    STRESS_INDEX,
    VO2_MAX,
    RESPIRATORY_RATE,
}

enum class AlertStatus { NORMAL, ALERT, CRITICAL }

enum class DeviceProtocol { GATT_STANDARD, PROPRIETARY_XIAOMI, PROPRIETARY_GARMIN, PROPRIETARY_SAMSUNG }

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
    val phone: String? = null,
    val avatarUrl: String? = null,
)

data class HealthProfile(
    val weightKg: Double,
    val heightCm: Int,
    val bloodType: String,
    val rhFactor: String = "+",
    val birthDate: String = "",
    val gender: String = "No especificado",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelation: String = "",
    val insuranceProvider: String = "",
    val policyNumber: String = "",
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
    val route: String = "Oral",
    val adherencePercentage: Int = 100,
    val totalDosesGiven: Int = 0,
    val totalDosesScheduled: Int = 0,
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
    val batteryPercent: Int = 100,
    val rssi: Int = -55,
    val lastSyncTimestamp: String? = null,
    val firmwareVersion: String = "1.0.0",
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
    val details: String? = null,
    val metabolicRiskScore: Float? = null,
    val sleepQualityScore: Float? = null,
    val stressScore: Float? = null,
    val infectionRiskScore: Float? = null,
    val hypertensionRiskScore: Float? = null,
    val vo2MaxScore: Float? = null,
)

// --- Modelos de Historia Clínica Integral ---

enum class AllergyCategory { MEDICAMENTO, ALIMENTO, AMBIENTAL, OTRO }
enum class AllergySeverity { LEVE, MODERADA, SEVERA_ANAFILAXIA }

data class Allergy(
    val id: String,
    val allergen: String,
    val category: AllergyCategory,
    val severity: AllergySeverity,
    val manifestations: String,
    val diagnosedDate: String,
)

enum class PathologicalType { ENFERMEDAD_CRONICA, CIRUGIA, HOSPITALIZACION, IMPLANTE }

data class PathologicalHistory(
    val id: String,
    val type: PathologicalType,
    val title: String,
    val icd10Code: String? = null,
    val yearOrDate: String,
    val notes: String = "",
)

data class GynecoObstetricHistory(
    val id: String = "gyneco_me",
    val menarcheAge: Int? = null,
    val lastMenstrualPeriod: String? = null,
    val gestas: Int = 0,
    val partos: Int = 0,
    val cesareas: Int = 0,
    val abortos: Int = 0,
    val contraceptiveMethod: String = "Ninguno",
    val isPregnant: Boolean = false,
    val gestationalWeeks: Int? = null,
)

enum class Kinship { PADRE, MADRE, ABUELOS, HERMANOS, OTRO }

data class FamilyHistory(
    val id: String,
    val kinship: Kinship,
    val conditionName: String,
    val category: String = "Cardiovascular",
    val notes: String = "",
)

data class LifestyleHabits(
    val id: String = "habits_me",
    val smokingStatus: String = "No fumador",
    val packsPerDay: Double = 0.0,
    val alcoholFrequency: String = "Ocasional",
    val exerciseDaysPerWeek: Int = 3,
    val exerciseIntensity: String = "Moderada",
    val averageSleepHours: Double = 7.5,
    val dietPattern: String = "Balanceada",
)

// --- Modelos de Configuración y Preferencias ---

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor { TEAL, BLUE, PURPLE, EMERALD, AMBER, CORAL }

data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val sosAlerts: Boolean = true,
    val routineVitals: Boolean = true,
    val medicationReminders: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val bypassQuietHoursForSos: Boolean = true,
)

data class DynamicSyncConfig(
    val deviceId: String = "default",
    val samplingIntervalMs: Long = 1000L,
    val batchSize: Int = 20,
    val criticalSyncEnabled: Boolean = true,
)

data class CaregiverProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val specialty: String = "Medicina General / Cuidados Continuos",
    val institution: String = "Red de Monitoreo Clínico Health OS",
    val emergencyPhone: String = "",
)
