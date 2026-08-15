package com.healthos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healthos.domain.model.AlertStatus
import com.healthos.domain.model.DeviceProtocol
import com.healthos.domain.model.MetricType
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
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val status: AlertStatus,
)
