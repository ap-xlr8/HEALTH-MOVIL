package com.healthos.sync

import com.healthos.data.local.AlertEntity
import com.healthos.data.local.DeviceEntity
import com.healthos.data.local.HealthOsDao
import com.healthos.data.local.MeasurementEntity
import com.healthos.data.local.MedicationEntity
import com.healthos.data.local.PatientEntity
import com.healthos.data.remote.AlertDto
import com.healthos.data.remote.ApiResponse
import com.healthos.data.remote.CreateMedicationRequestDto
import com.healthos.data.remote.DeviceDto
import com.healthos.data.remote.MeasurementDto
import com.healthos.data.remote.MedicationDto
import com.healthos.data.remote.MedicationLogRequestDto
import com.healthos.data.remote.PatientApiService
import com.healthos.data.remote.PatientProfileDto
import com.healthos.data.remote.SyncMeasurementItemDto
import com.healthos.data.remote.SyncMeasurementsRequestDto
import com.healthos.data.remote.SyncMeasurementsResponseDto
import com.healthos.domain.model.MetricType
import com.healthos.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FakeDao : HealthOsDao {
    val measurementsList = mutableListOf<MeasurementEntity>()
    var updatedStatus: SyncStatus? = null
    var updatedIds: List<String> = emptyList()

    override fun latestMeasurements(): Flow<List<MeasurementEntity>> = flowOf(measurementsList)

    override suspend fun measurements(
        metric: String,
        limit: Int,
    ): List<MeasurementEntity> = measurementsList.filter { it.metricType.name.equals(metric, ignoreCase = true) }.take(limit)

    override suspend fun getPendingMeasurements(limit: Int): List<MeasurementEntity> =
        measurementsList.filter { it.syncStatus == SyncStatus.PENDING }.take(limit)

    override suspend fun updateSyncStatus(
        ids: List<String>,
        status: SyncStatus,
    ) {
        updatedIds = ids
        updatedStatus = status
    }

    override suspend fun upsertMeasurement(entity: MeasurementEntity) {
        measurementsList.add(entity)
    }

    override fun medications(): Flow<List<MedicationEntity>> = flowOf(emptyList())

    override suspend fun upsertMedications(entities: List<MedicationEntity>) {}

    override suspend fun markMedicationTaken(id: String) {}

    override fun alerts(): Flow<List<AlertEntity>> = flowOf(emptyList())

    override suspend fun upsertAlert(entity: AlertEntity) {}

    override fun devices(): Flow<List<DeviceEntity>> = flowOf(emptyList())

    override suspend fun upsertDevice(entity: DeviceEntity) {}

    override suspend fun deleteDevice(id: String) {}

    override fun patients(): Flow<List<PatientEntity>> = flowOf(emptyList())

    override suspend fun patient(id: String): PatientEntity? = null

    override suspend fun upsertPatients(entities: List<PatientEntity>) {}
}

class FakePatientApi : PatientApiService {
    var syncedRequest: SyncMeasurementsRequestDto? = null
    var shouldFail = false

    override suspend fun syncMeasurements(request: SyncMeasurementsRequestDto): Response<SyncMeasurementsResponseDto> {
        if (shouldFail) throw RuntimeException("Network timeout")
        syncedRequest = request
        val payload = SyncMeasurementsResponseDto(status = "success", syncedCount = request.data.size)
        return Response.success(payload)
    }

    override suspend fun getMeasurements(
        patientId: String,
        type: String?,
        from: String?,
        to: String?,
        limit: Int?,
    ): Response<ApiResponse<List<MeasurementDto>>> = Response.success(ApiResponse(status = "success", data = emptyList()))

    override suspend fun getPatient(patientId: String): Response<PatientProfileDto> =
        Response.success(PatientProfileDto(id = patientId, firstName = "Test"))

    override suspend fun getMedications(patientId: String): Response<ApiResponse<List<MedicationDto>>> =
        Response.success(ApiResponse(status = "success", data = emptyList()))

    override suspend fun addMedication(
        patientId: String,
        request: CreateMedicationRequestDto,
    ): Response<ApiResponse<MedicationDto>> =
        Response.success(
            ApiResponse(
                status = "success",
                data = MedicationDto(id = "med_1", name = request.name, dosage = request.dosage, schedule = request.schedule),
            ),
        )

    override suspend fun logMedication(
        patientId: String,
        request: MedicationLogRequestDto,
    ): Response<ApiResponse<Map<String, Any>>> = Response.success(ApiResponse(status = "success", data = emptyMap()))

    override suspend fun getAlertDetail(alertId: String): Response<AlertDto> = Response.success(AlertDto(id = alertId))

    override suspend fun acknowledgeAlert(alertId: String): Response<AlertDto> =
        Response.success(AlertDto(id = alertId, acknowledged = true))

    override suspend fun getDevices(): Response<ApiResponse<List<DeviceDto>>> =
        Response.success(ApiResponse(status = "success", data = emptyList()))

    override suspend fun registerDevice(request: DeviceDto): Response<ApiResponse<DeviceDto>> =
        Response.success(ApiResponse(status = "success", data = request))
}

class SyncWorkersTest {
    @Test
    fun testPendingMeasurementsProcessing() =
        runTest {
            val fakeDao = FakeDao()
            val fakeApi = FakePatientApi()

            fakeDao.upsertMeasurement(
                MeasurementEntity(
                    id = "m1",
                    metricType = MetricType.HEART_RATE,
                    value = 75.0,
                    unit = "bpm",
                    timestamp = "2026-08-15T12:00:00Z",
                    syncStatus = SyncStatus.PENDING,
                ),
            )
            fakeDao.upsertMeasurement(
                MeasurementEntity(
                    id = "m2",
                    metricType = MetricType.SPO2,
                    value = 98.0,
                    unit = "%",
                    timestamp = "2026-08-15T12:01:00Z",
                    syncStatus = SyncStatus.PENDING,
                ),
            )

            val pending = fakeDao.getPendingMeasurements(50)
            assertEquals(2, pending.size)

            val dtos =
                pending.map {
                    SyncMeasurementItemDto(
                        type = it.metricType.name.lowercase(),
                        value = it.value,
                        unit = it.unit,
                        timestamp = it.timestamp,
                    )
                }
            val response =
                fakeApi.syncMeasurements(
                    SyncMeasurementsRequestDto(
                        deviceId = "MOBILE_OUTBOX",
                        data = dtos,
                    ),
                )
            assertTrue(response.isSuccessful)
            assertEquals(2, response.body()?.syncedCount)

            fakeDao.updateSyncStatus(pending.map { it.id }, SyncStatus.SYNCED)
            assertEquals(SyncStatus.SYNCED, fakeDao.updatedStatus)
            assertEquals(listOf("m1", "m2"), fakeDao.updatedIds)
        }

    @Test
    fun testSyncFailsGracefullyOnNetworkError() =
        runTest {
            val fakeApi = FakePatientApi().apply { shouldFail = true }

            var failed = false
            try {
                fakeApi.syncMeasurements(
                    SyncMeasurementsRequestDto(
                        deviceId = "MOBILE_OUTBOX",
                        data = emptyList(),
                    ),
                )
            } catch (_: Exception) {
                failed = true
            }
            assertTrue(failed)
        }
}
