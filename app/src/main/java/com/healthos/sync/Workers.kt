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
                                    MetricType.SKIN_TEMPERATURE, MetricType.TEMPERATURE -> "skin_temperature" to "°C"
                                    MetricType.EDA -> "electrodermal_activity" to "µS"
                                    MetricType.BLOOD_PRESSURE_SYSTOLIC, MetricType.BLOOD_PRESSURE -> "blood_pressure" to "mmHg"
                                    MetricType.PTT -> "pulse_transit_time" to "ms"
                                    MetricType.HRV_RMSSD, MetricType.HRV_SDNN -> "hrv" to "ms"
                                    MetricType.GLUCOSE -> "glucose" to "mg/dL"
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
                val latestHr = dao.measurements("HEART_RATE", 1).firstOrNull()?.value ?: 0.0
                val latestSpo2 = dao.measurements("SPO2", 1).firstOrNull()?.value ?: 0.0

                val items = mutableListOf<SyncMeasurementItemDto>()
                if (latestHr > 0.0) {
                    items.add(
                        SyncMeasurementItemDto(
                            type = "heart_rate",
                            value = latestHr,
                            unit = "bpm",
                            timestamp = nowStr,
                        ),
                    )
                }
                if (latestSpo2 > 0.0) {
                    items.add(
                        SyncMeasurementItemDto(
                            type = "blood_oxygen",
                            value = latestSpo2,
                            unit = "%",
                            timestamp = nowStr,
                        ),
                    )
                }

                if (items.isNotEmpty()) {
                    val request =
                        SyncMeasurementsRequestDto(
                            deviceId = "CRITICAL_EVENT_$eventId",
                            data = items,
                        )
                    patientApi.syncMeasurements(request)
                }
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }

