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
        AllergyEntity::class,
        PathologicalHistoryEntity::class,
        GynecoObstetricEntity::class,
        FamilyHistoryEntity::class,
        LifestyleHabitsEntity::class,
        NotificationSettingsEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class HealthOsDatabase : RoomDatabase() {
    abstract fun dao(): HealthOsDao
}

@Dao
interface HealthOsDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC LIMIT 20")
    fun latestMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE metricType = :metric ORDER BY timestamp DESC LIMIT :limit")
    suspend fun measurements(
        metric: String,
        limit: Int,
    ): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingMeasurements(limit: Int): List<MeasurementEntity>

    @Query("SELECT COUNT(*) FROM measurements WHERE syncStatus = 'PENDING'")
    fun countPendingMeasurements(): Flow<Int>

    @Query("UPDATE measurements SET syncStatus = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(
        ids: List<String>,
        status: com.healthos.domain.model.SyncStatus,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(entity: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurements(entities: List<MeasurementEntity>)

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

    @Query("UPDATE devices SET batteryPercent = :batteryPercent, rssi = :rssi, lastSyncTimestamp = :lastSync WHERE id = :id")
    suspend fun updateDeviceTelemetry(
        id: String,
        batteryPercent: Int,
        rssi: Int,
        lastSync: String,
    )

    @Query("SELECT * FROM patients ORDER BY status DESC, firstName ASC")
    fun patients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun patient(id: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPatients(entities: List<PatientEntity>)

    // --- Historia Clínica DAOs ---

    @Query("SELECT * FROM allergies ORDER BY diagnosedDate DESC")
    fun allergies(): Flow<List<AllergyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllergy(entity: AllergyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllergies(entities: List<AllergyEntity>)

    @Query("DELETE FROM allergies WHERE id = :id")
    suspend fun deleteAllergy(id: String)

    @Query("SELECT * FROM pathological_history ORDER BY yearOrDate DESC")
    fun pathologicalHistory(): Flow<List<PathologicalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPathologicalHistory(entity: PathologicalHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPathologicalHistories(entities: List<PathologicalHistoryEntity>)

    @Query("DELETE FROM pathological_history WHERE id = :id")
    suspend fun deletePathologicalHistory(id: String)

    @Query("SELECT * FROM gyneco_obstetric WHERE id = 'gyneco_me' LIMIT 1")
    fun gynecoObstetric(): Flow<GynecoObstetricEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGynecoObstetric(entity: GynecoObstetricEntity)

    @Query("SELECT * FROM family_history ORDER BY category ASC")
    fun familyHistory(): Flow<List<FamilyHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFamilyHistory(entity: FamilyHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFamilyHistories(entities: List<FamilyHistoryEntity>)

    @Query("DELETE FROM family_history WHERE id = :id")
    suspend fun deleteFamilyHistory(id: String)

    @Query("SELECT * FROM lifestyle_habits WHERE id = 'habits_me' LIMIT 1")
    fun lifestyleHabits(): Flow<LifestyleHabitsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLifestyleHabits(entity: LifestyleHabitsEntity)

    // --- Notification Settings DAO ---

    @Query("SELECT * FROM notification_settings WHERE id = 'default_settings' LIMIT 1")
    fun notificationSettings(): Flow<NotificationSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationSettings(entity: NotificationSettingsEntity)
}

