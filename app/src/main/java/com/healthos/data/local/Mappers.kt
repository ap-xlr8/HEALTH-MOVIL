package com.healthos.data.local

import com.healthos.domain.model.Alert
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.model.WearableDevice

fun MeasurementEntity.toDomain() = Measurement(id, metricType, value, unit, timestamp, syncStatus)

fun Measurement.toEntity() = MeasurementEntity(id, metricType, value, unit, timestamp, syncStatus)

fun MedicationEntity.toDomain() = Medication(id, name, dose, schedule, takenToday)

fun Medication.toEntity() = MedicationEntity(id, name, dose, schedule, takenToday)

fun AlertEntity.toDomain() = Alert(id, title, status, timestamp)

fun Alert.toEntity() = AlertEntity(id, title, status, timestamp)

fun DeviceEntity.toDomain() = WearableDevice(id, model, protocol, publicKey, connected)

fun WearableDevice.toEntity() = DeviceEntity(id, model, protocol, publicKey, connected)

fun PatientEntity.toDomain(latestMeasurement: Measurement?) = PatientSummary(id, firstName, status, latestMeasurement)
