package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AppSettingsEntity)
}

@Dao
interface DailyStateDao {
    @Query("SELECT * FROM daily_state WHERE date = :date")
    fun observe(date: String): Flow<DailyStateEntity?>

    @Query("SELECT * FROM daily_state WHERE date = :date")
    suspend fun get(date: String): DailyStateEntity?

    @Query("SELECT * FROM daily_state ORDER BY date DESC")
    suspend fun getAll(): List<DailyStateEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: DailyStateEntity): Long

    @Query("UPDATE daily_state SET goalType = :type, goalTarget = :target, goalEnabled = 1 WHERE date = :date")
    suspend fun updateGoal(date: String, type: String, target: Int)

    @Query("UPDATE daily_state SET calorieTarget = :calories, proteinTargetGrams = :protein, waterTargetMl = :water, sleepTargetMinutes = :sleep, targetsKnown = 1, activityEnabled = :activity WHERE date = :date")
    suspend fun updateTargets(date: String, calories: Int, protein: Int, water: Int, sleep: Int, activity: Boolean)
}

@Dao
interface RewardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: RewardEventEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM legacy_reward_exclusions WHERE eventKey = :key)")
    suspend fun isExcluded(key: String): Boolean

    @Query("SELECT COUNT(*) FROM reward_ledger")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM reward_ledger WHERE eventKey LIKE :pattern")
    suspend fun countMatching(pattern: String): Int

    @Query("SELECT * FROM reward_ledger ORDER BY createdAt DESC")
    fun observe(): Flow<List<RewardEventEntity>>
}

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC")
    fun observe(): Flow<List<CycleEntryEntity>>

    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC")
    suspend fun getAll(): List<CycleEntryEntity>

    @Query("SELECT * FROM cycle_entries WHERE startDate = :date LIMIT 1")
    suspend fun getByStart(date: String): CycleEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: CycleEntryEntity): Long

    @Update
    suspend fun update(entry: CycleEntryEntity)

    @Delete
    suspend fun delete(entry: CycleEntryEntity)
}