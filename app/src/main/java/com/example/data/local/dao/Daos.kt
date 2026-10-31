package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET currentXp = :xp, currentLevel = :level WHERE id = 1")
    suspend fun updateXpAndLevel(xp: Int, level: Int)

    @Query("UPDATE user_profile SET currentStreak = :streak, longestStreak = :longest, perfectDays = :perfect, recoveryDays = :recovery, isRecoveryActive = :recoveryActive WHERE id = 1")
    suspend fun updateStreakStats(streak: Int, longest: Int, perfect: Int, recovery: Int, recoveryActive: Boolean)

    @Query("UPDATE user_profile SET currentMomentum = :momentum WHERE id = 1")
    suspend fun updateMomentum(momentum: Int)
}

@Dao
interface DailyChecklistDao {
    @Query("SELECT * FROM daily_checklist WHERE date = :date ORDER BY orderIndex ASC, id ASC")
    fun getChecklistForDate(date: String): Flow<List<DailyChecklistItemEntity>>

    @Query("SELECT * FROM daily_checklist WHERE date = :date ORDER BY orderIndex ASC, id ASC")
    suspend fun getChecklistForDateDirect(date: String): List<DailyChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DailyChecklistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DailyChecklistItemEntity): Long

    @Update
    suspend fun updateItem(item: DailyChecklistItemEntity)

    @Query("UPDATE daily_checklist SET isCompleted = :isCompleted, completionTime = :time WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean, time: String)

    @Query("DELETE FROM daily_checklist WHERE date = :date")
    suspend fun clearChecklistForDate(date: String)

    @Query("DELETE FROM daily_checklist")
    suspend fun clearAll()

    @Delete
    suspend fun deleteItem(item: DailyChecklistItemEntity)
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY id ASC")
    fun getMealsForDate(date: String): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY id ASC")
    suspend fun getMealsForDateDirect(date: String): List<MealPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealPlanEntity>)

    @Update
    suspend fun updateMeal(meal: MealPlanEntity)

    @Query("UPDATE meal_plans SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setMealCompleted(id: Long, isCompleted: Boolean)

    @Delete
    suspend fun deleteMeal(meal: MealPlanEntity)

    // Quick Protein Logs
    @Query("SELECT * FROM protein_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getProteinLogsForDate(date: String): Flow<List<ProteinLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProteinLog(log: ProteinLogEntity): Long

    @Delete
    suspend fun deleteProteinLog(log: ProteinLogEntity)

    @Query("SELECT SUM(proteinGrams) FROM protein_logs WHERE date = :date")
    fun getTotalLoggedProteinForDate(date: String): Flow<Int?>
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    fun getWaterLogForDate(date: String): Flow<WaterLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWaterLog(log: WaterLogEntity)
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_logs WHERE date = :date LIMIT 1")
    fun getSleepLogForDate(date: String): Flow<SleepLogEntity?>

    @Query("SELECT * FROM sleep_logs ORDER BY date DESC LIMIT 7")
    fun getRecentSleepLogs(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSleepLog(log: SleepLogEntity)
}

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements ORDER BY id ASC")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: SupplementEntity): Long

    @Update
    suspend fun updateSupplement(supplement: SupplementEntity)

    @Delete
    suspend fun deleteSupplement(supplement: SupplementEntity)

    @Query("SELECT * FROM supplement_logs WHERE date = :date")
    fun getSupplementLogsForDate(date: String): Flow<List<SupplementLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplementLog(log: SupplementLogEntity): Long

    @Query("DELETE FROM supplement_logs WHERE supplementId = :supplementId AND date = :date")
    suspend fun deleteSupplementLog(supplementId: Long, date: String)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    fun getWorkoutSessionForDate(date: String): Flow<WorkoutSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWorkoutSession(session: WorkoutSessionEntity): Long

    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getExercisesForSession(sessionId: Long): Flow<List<ExerciseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseLogEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseLogEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseLogEntity)

    // Cardio
    @Query("SELECT * FROM cardio_logs WHERE date = :date")
    fun getCardioLogsForDate(date: String): Flow<List<CardioLogEntity>>

    @Query("SELECT * FROM cardio_logs ORDER BY date DESC LIMIT 30")
    fun getRecentCardioLogs(): Flow<List<CardioLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardioLog(cardio: CardioLogEntity): Long

    @Delete
    suspend fun deleteCardioLog(cardio: CardioLogEntity)

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE isCompleted = 1")
    fun getTotalCompletedWorkouts(): Flow<Int>
}

@Dao
interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY date DESC")
    fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE date = :date LIMIT 1")
    fun getMeasurementForDate(date: String): Flow<BodyMeasurementEntity?>

    @Query("SELECT * FROM body_measurements WHERE date = :date LIMIT 1")
    suspend fun getMeasurementForDateDirect(date: String): BodyMeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity): Long

    @Update
    suspend fun updateMeasurement(measurement: BodyMeasurementEntity)

    @Delete
    suspend fun deleteMeasurement(measurement: BodyMeasurementEntity)
}

@Dao
interface MealPrepDao {
    @Query("SELECT * FROM meal_prep_items ORDER BY id DESC")
    fun getAllMealPrepItems(): Flow<List<MealPrepItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPrepItem(item: MealPrepItemEntity): Long

    @Update
    suspend fun updateMealPrepItem(item: MealPrepItemEntity)

    @Delete
    suspend fun deleteMealPrepItem(item: MealPrepItemEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedDate = :date WHERE id = :id")
    suspend fun unlockAchievement(id: String, date: String)
}

@Dao
interface RecoveryDao {
    @Query("SELECT * FROM recovery_logs WHERE date = :date LIMIT 1")
    fun getRecoveryForDate(date: String): Flow<RecoveryLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecovery(recovery: RecoveryLogEntity): Long
}

@Dao
interface ActivitySessionDao {
    @Query("SELECT * FROM activity_sessions WHERE date = :date ORDER BY id DESC")
    fun getActivitySessionsForDate(date: String): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE date = :date ORDER BY id DESC")
    suspend fun getActivitySessionsForDateDirect(date: String): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions ORDER BY date DESC, id DESC")
    fun getAllActivitySessions(): Flow<List<ActivitySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivitySession(session: ActivitySessionEntity): Long

    @Update
    suspend fun updateActivitySession(session: ActivitySessionEntity)

    @Delete
    suspend fun deleteActivitySession(session: ActivitySessionEntity)

    @Query("SELECT COUNT(*) FROM activity_sessions WHERE isCompleted = 1")
    fun getTotalCompletedActivities(): Flow<Int>
}

@Dao
interface HealthConnectImportDao {
    @Query("SELECT COUNT(*) > 0 FROM health_connect_imports WHERE recordId = :recordId")
    suspend fun isRecordImported(recordId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImport(importItem: HealthConnectImportEntity): Long

    @Query("SELECT * FROM health_connect_imports ORDER BY importedTimestamp DESC LIMIT 50")
    fun getRecentImports(): Flow<List<HealthConnectImportEntity>>

    @Query("SELECT * FROM health_connect_imports WHERE date = :date")
    suspend fun getImportsForDate(date: String): List<HealthConnectImportEntity>
}

@Dao
interface EnvironmentTaskDao {
    @Query("SELECT * FROM environment_tasks WHERE date = :date ORDER BY orderIndex ASC, id ASC")
    fun getTasksForDate(date: String): Flow<List<EnvironmentTaskEntity>>

    @Query("SELECT * FROM environment_tasks WHERE date = :date ORDER BY orderIndex ASC, id ASC")
    suspend fun getTasksForDateDirect(date: String): List<EnvironmentTaskEntity>

    @Query("SELECT * FROM environment_tasks WHERE date = :date AND category = :category ORDER BY orderIndex ASC")
    fun getTasksByCategory(date: String, category: String): Flow<List<EnvironmentTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: EnvironmentTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<EnvironmentTaskEntity>)

    @Update
    suspend fun updateTask(task: EnvironmentTaskEntity)

    @Query("UPDATE environment_tasks SET isCompleted = :isCompleted, completionTime = :time WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean, time: String)

    @Delete
    suspend fun deleteTask(task: EnvironmentTaskEntity)

    @Query("DELETE FROM environment_tasks WHERE date = :date")
    suspend fun clearTasksForDate(date: String)
}

@Dao
interface DailyCheckinDao {
    @Query("SELECT * FROM daily_checkins WHERE date = :date AND timeOfDay = :timeOfDay LIMIT 1")
    fun getCheckin(date: String, timeOfDay: String): Flow<DailyCheckinEntity?>

    @Query("SELECT * FROM daily_checkins WHERE date = :date AND timeOfDay = :timeOfDay LIMIT 1")
    suspend fun getCheckinDirect(date: String, timeOfDay: String): DailyCheckinEntity?

    @Query("SELECT * FROM daily_checkins WHERE date = :date")
    fun getAllCheckinsForDate(date: String): Flow<List<DailyCheckinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCheckin(checkin: DailyCheckinEntity): Long
}

@Dao
interface ReflectionHistoryDao {
    @Query("SELECT reflectionId FROM reflection_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReflectionIds(limit: Int = 30): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordReflectionShown(history: ReflectionHistoryEntity): Long
}

@Dao
interface FriendConnectionDao {
    @Query("SELECT * FROM friend_connections ORDER BY createdTimestamp DESC")
    fun getAllFriends(): Flow<List<FriendConnectionEntity>>

    @Query("SELECT * FROM friend_connections WHERE id = :id LIMIT 1")
    suspend fun getFriendById(id: Long): FriendConnectionEntity?

    @Query("DELETE FROM friend_connections WHERE id = :id")
    suspend fun deleteFriendById(id: Long)

    @Query("UPDATE friend_connections SET sharedMomentum = :momentum, lastActiveDate = :lastActive, status = :status WHERE id = :id")
    suspend fun updateFriendStatus(id: Long, momentum: Int, lastActive: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendConnectionEntity): Long

    @Delete
    suspend fun deleteFriend(friend: FriendConnectionEntity)
}


