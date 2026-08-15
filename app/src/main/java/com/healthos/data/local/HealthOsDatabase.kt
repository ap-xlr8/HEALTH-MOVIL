package com.healthos.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        MeasurementEntity::class,
        MedicationEntity::class,
        AlertEntity::class,
        DeviceEntity::class,
        PatientEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HealthOsDatabase : RoomDatabase() {
    abstract fun dao(): HealthOsDao
}

@Dao
interface HealthOsDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC LIMIT 8")
    fun latestMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE metricType = :metric ORDER BY timestamp DESC LIMIT :limit")
    suspend fun measurements(
        metric: String,
        limit: Int,
    ): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(entity: MeasurementEntity)

    @Query("SELECT * FROM medications ORDER BY schedule ASC")
    fun medications(): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedications(entities: List<MedicationEntity>)

    @Query("UPDATE medications SET takenToday = 1 WHERE id = :id")
    suspend fun markMedicationTaken(id: String)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun alerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlert(entity: AlertEntity)

    @Query("SELECT * FROM devices ORDER BY model ASC")
    fun devices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(entity: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDevice(id: String)

    @Query("SELECT * FROM patients ORDER BY status DESC, firstName ASC")
    fun patients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun patient(id: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPatients(entities: List<PatientEntity>)
}
