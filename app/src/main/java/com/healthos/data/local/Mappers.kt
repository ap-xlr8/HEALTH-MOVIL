package com.healthos.data.local

import com.healthos.domain.model.Alert
import com.healthos.domain.model.Allergy
import com.healthos.domain.model.FamilyHistory
import com.healthos.domain.model.GynecoObstetricHistory
import com.healthos.domain.model.LifestyleHabits
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.NotificationPreferences
import com.healthos.domain.model.PathologicalHistory
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.model.WearableDevice

fun MeasurementEntity.toDomain() = Measurement(id, metricType, value, unit, timestamp, syncStatus)

fun Measurement.toEntity() = MeasurementEntity(id, metricType, value, unit, timestamp, syncStatus)

fun MedicationEntity.toDomain() =
    Medication(id, name, dose, schedule, takenToday, route, adherencePercentage, totalDosesGiven, totalDosesScheduled)

fun Medication.toEntity() =
    MedicationEntity(id, name, dose, schedule, takenToday, route, adherencePercentage, totalDosesGiven, totalDosesScheduled)

fun AlertEntity.toDomain() = Alert(id, title, status, timestamp)

fun Alert.toEntity() = AlertEntity(id, title, status, timestamp)

fun DeviceEntity.toDomain() =
    WearableDevice(id, model, protocol, publicKey, connected, batteryPercent, rssi, lastSyncTimestamp, firmwareVersion)

fun WearableDevice.toEntity() =
    DeviceEntity(id, model, protocol, publicKey, connected, batteryPercent, rssi, lastSyncTimestamp, firmwareVersion)

fun PatientEntity.toDomain(latestMeasurement: Measurement?) =
    PatientSummary(id, firstName, status, latestMeasurement)

fun AllergyEntity.toDomain() =
    Allergy(id, allergen, category, severity, manifestations, diagnosedDate)

fun Allergy.toEntity() =
    AllergyEntity(id, allergen, category, severity, manifestations, diagnosedDate)

fun PathologicalHistoryEntity.toDomain() =
    PathologicalHistory(id, type, title, icd10Code, yearOrDate, notes)

fun PathologicalHistory.toEntity() =
    PathologicalHistoryEntity(id, type, title, icd10Code, yearOrDate, notes)

fun GynecoObstetricEntity.toDomain() =
    GynecoObstetricHistory(
        id,
        menarcheAge,
        lastMenstrualPeriod,
        gestas,
        partos,
        cesareas,
        abortos,
        contraceptiveMethod,
        isPregnant,
        gestationalWeeks,
    )

fun GynecoObstetricHistory.toEntity() =
    GynecoObstetricEntity(
        id,
        menarcheAge,
        lastMenstrualPeriod,
        gestas,
        partos,
        cesareas,
        abortos,
        contraceptiveMethod,
        isPregnant,
        gestationalWeeks,
    )

fun FamilyHistoryEntity.toDomain() =
    FamilyHistory(id, kinship, conditionName, category, notes)

fun FamilyHistory.toEntity() =
    FamilyHistoryEntity(id, kinship, conditionName, category, notes)

fun LifestyleHabitsEntity.toDomain() =
    LifestyleHabits(
        id,
        smokingStatus,
        packsPerDay,
        alcoholFrequency,
        exerciseDaysPerWeek,
        exerciseIntensity,
        averageSleepHours,
        dietPattern,
    )

fun LifestyleHabits.toEntity() =
    LifestyleHabitsEntity(
        id,
        smokingStatus,
        packsPerDay,
        alcoholFrequency,
        exerciseDaysPerWeek,
        exerciseIntensity,
        averageSleepHours,
        dietPattern,
    )

fun NotificationSettingsEntity.toDomain() =
    NotificationPreferences(
        pushEnabled,
        emailEnabled,
        smsEnabled,
        sosAlerts,
        routineVitals,
        medicationReminders,
        quietHoursEnabled,
        quietHoursStart,
        quietHoursEnd,
        bypassQuietHoursForSos,
    )

fun NotificationPreferences.toEntity() =
    NotificationSettingsEntity(
        "default_settings",
        pushEnabled,
        emailEnabled,
        smsEnabled,
        sosAlerts,
        routineVitals,
        medicationReminders,
        quietHoursEnabled,
        quietHoursStart,
        quietHoursEnd,
        bypassQuietHoursForSos,
    )

