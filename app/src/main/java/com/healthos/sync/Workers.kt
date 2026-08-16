package com.healthos.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthos.data.local.HealthOsDao
import com.healthos.data.remote.PatientApiService
import com.healthos.data.remote.SyncMeasurementItemDto
import com.healthos.data.remote.SyncMeasurementsRequestDto
import com.healthos.domain.model.MetricType
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
                    val dtos =
                        list.map {
                            val (backendType, backendUnit) =
                                when (it.metricType) {
                                    MetricType.SPO2 -> "blood_oxygen" to "%"
                                    MetricType.HEART_RATE -> "heart_rate" to "bpm"
                                    else -> "heart_rate" to "bpm"
                                }
                            SyncMeasurementItemDto(
                                type = backendType,
                                value = it.value,
                                unit = backendUnit,
                                timestamp = it.timestamp,
                            )
                        }
                    val request =
                        SyncMeasurementsRequestDto(
                            deviceId = "MOBILE_OUTBOX",
                            data = dtos,
                        )
                    val response = patientApi.syncMeasurements(request)
                    if (response.isSuccessful) {
                        dao.updateSyncStatus(list.map { it.id }, com.healthos.domain.model.SyncStatus.SYNCED)
                    }
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
                val latestHr = dao.measurements("HEART_RATE", 1).firstOrNull()?.value
                if (latestHr == null) {
                    // Sin medición real no se inventa una frecuencia cardíaca.
                    return Result.success()
                }
                val request =
                    SyncMeasurementsRequestDto(
                        deviceId = "CRITICAL_EVENT_$eventId",
                        data =
                            listOf(
                                SyncMeasurementItemDto(
                                    type = "heart_rate",
                                    value = latestHr,
                                    unit = "bpm",
                                    timestamp = nowStr,
                                ),
                            ),
                    )
                patientApi.syncMeasurements(request)
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }
