package com.healthos.data.remote

import com.healthos.domain.model.Alert
import com.healthos.domain.model.AlertStatus
import com.healthos.domain.model.DeviceProtocol
import com.healthos.domain.model.Measurement
import com.healthos.domain.model.Medication
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.PatientSummary
import com.healthos.domain.model.Role
import com.healthos.domain.model.Session
import com.healthos.domain.model.SyncStatus
import com.healthos.domain.model.UserProfile
import com.healthos.domain.model.WearableDevice
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockBackendDataSource
    @Inject
    constructor() {
        fun register(
            email: String,
            role: Role,
            firstName: String,
            lastName: String,
        ) = UserProfile(UUID.randomUUID().toString(), email, firstName, lastName, role)

        fun login(email: String): Session {
            val role = if (email.contains("cuidador", ignoreCase = true)) Role.CAREGIVER else Role.PATIENT
            return Session("mock-access-${UUID.randomUUID()}", "mock-refresh-${UUID.randomUUID()}", role)
        }

        fun seedMeasurements() =
            listOf(
                Measurement("m-hr", MetricType.HEART_RATE, 78.0, "bpm", Instant.now().toString(), SyncStatus.SYNCED),
                Measurement("m-spo2", MetricType.SPO2, 97.0, "%", Instant.now().toString(), SyncStatus.SYNCED),
                Measurement("m-temp", MetricType.TEMPERATURE, 36.7, "C", Instant.now().toString(), SyncStatus.SYNCED),
            )

        fun medications() =
            listOf(
                Medication("med-1", "Metformina", "850 mg", "08:00", false),
                Medication("med-2", "Losartan", "50 mg", "21:00", true),
            )

        fun devices() =
            listOf(
                WearableDevice("AA:BB:CC:DD:EE:FF", "Xiaomi Band 8", DeviceProtocol.PROPRIETARY_XIAOMI, "MIIBIjANBgkqhkiG9w0", false),
            )

        fun patients() =
            listOf(
                PatientSummary("p-1", "Carlos", AlertStatus.NORMAL, seedMeasurements().first()),
                PatientSummary("p-2", "Lucia", AlertStatus.ALERT, seedMeasurements()[1]),
            )

        fun sosAlert() = Alert(UUID.randomUUID().toString(), "SOS manual enviado", AlertStatus.CRITICAL, Instant.now().toString())
    }
