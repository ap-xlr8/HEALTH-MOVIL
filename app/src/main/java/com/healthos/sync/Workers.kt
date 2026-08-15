package com.healthos.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthos.data.local.HealthOsDao
import com.healthos.data.remote.PatientApiService
import com.healthos.data.remote.SyncMeasurementItemDto
import com.healthos.data.remote.SyncMeasurementsRequestDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NormalSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val dao: HealthOsDao,
        private val patientApi: PatientApiService,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            return try {
                val list = dao.getPendingMeasurements(50)
                if (list.isNotEmpty()) {
                    val dtos = list.map {
                        SyncMeasurementItemDto(
                            deviceId = "MOBILE_OUTBOX",
                            type = it.metricType.name.lowercase(),
                            value = it.value,
                            unit = it.unit,
                            timestamp = it.timestamp,
                        )
                    }
                    patientApi.syncMeasurements(SyncMeasurementsRequestDto(measurements = dtos))
                    dao.updateSyncStatus(list.map { it.id }, com.healthos.domain.model.SyncStatus.SYNCED)
                }
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }

@HiltWorker
class CriticalSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val dao: HealthOsDao,
        private val patientApi: PatientApiService,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val eventId = inputData.getString("EVENT_ID") ?: return Result.failure()
            if (eventId.isBlank()) return Result.failure()
            return try {
                val nowStr = java.time.Instant.now().toString()
                val latestHr = dao.measurements("HEART_RATE", 1).firstOrNull()?.value ?: 0.0
                patientApi.syncMeasurements(
                    SyncMeasurementsRequestDto(
                        measurements =
                            listOf(
                                SyncMeasurementItemDto(
                                    deviceId = "CRITICAL_EVENT_$eventId",
                                    type = "heart_rate",
                                    value = latestHr,
                                    unit = "bpm",
                                    timestamp = nowStr,
                                ),
                            ),
                    ),
                )
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }

