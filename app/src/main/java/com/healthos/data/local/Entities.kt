package com.healthos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healthos.domain.model.AlertStatus
import com.healthos.domain.model.AllergyCategory
import com.healthos.domain.model.AllergySeverity
import com.healthos.domain.model.DeviceProtocol
import com.healthos.domain.model.Kinship
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.PathologicalType
import com.healthos.domain.model.SyncStatus

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val metricType: MetricType,
    val value: Double,
    val unit: String,
    val timestamp: String,
    val syncStatus: SyncStatus,
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dose: String,
    val schedule: String,
    val takenToday: Boolean,
    val route: String = "Oral",
    val adherencePercentage: Int = 100,
    val totalDosesGiven: Int = 0,
    val totalDosesScheduled: Int = 0,
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: AlertStatus,
    val timestamp: String,
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val model: String,
    val protocol: DeviceProtocol,
    val publicKey: String,
    val connected: Boolean,
    val batteryPercent: Int = 100,
    val rssi: Int = -55,
    val lastSyncTimestamp: String? = null,
    val firmwareVersion: String = "1.0.0",
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val status: AlertStatus,
)

@Entity(tableName = "allergies")
data class AllergyEntity(
    @PrimaryKey val id: String,
    val allergen: String,
    val category: AllergyCategory,
    val severity: AllergySeverity,
    val manifestations: String,
    val diagnosedDate: String,
)

@Entity(tableName = "pathological_history")
data class PathologicalHistoryEntity(
    @PrimaryKey val id: String,
    val type: PathologicalType,
    val title: String,
    val icd10Code: String?,
    val yearOrDate: String,
    val notes: String,
)

@Entity(tableName = "gyneco_obstetric")
data class GynecoObstetricEntity(
    @PrimaryKey val id: String,
    val menarcheAge: Int?,
    val lastMenstrualPeriod: String?,
    val gestas: Int,
    val partos: Int,
    val cesareas: Int,
    val abortos: Int,
    val contraceptiveMethod: String,
    val isPregnant: Boolean,
    val gestationalWeeks: Int?,
)

@Entity(tableName = "family_history")
data class FamilyHistoryEntity(
    @PrimaryKey val id: String,
    val kinship: Kinship,
    val conditionName: String,
    val category: String,
    val notes: String,
)

@Entity(tableName = "lifestyle_habits")
data class LifestyleHabitsEntity(
    @PrimaryKey val id: String,
    val smokingStatus: String,
    val packsPerDay: Double,
    val alcoholFrequency: String,
    val exerciseDaysPerWeek: Int,
    val exerciseIntensity: String,
    val averageSleepHours: Double,
    val dietPattern: String,
)

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val id: String = "default_settings",
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

