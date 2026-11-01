package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        DailyChecklistItemEntity::class,
        MealPlanEntity::class,
        ProteinLogEntity::class,
        WaterLogEntity::class,
        SleepLogEntity::class,
        SupplementEntity::class,
        SupplementLogEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        CardioLogEntity::class,
        ActivitySessionEntity::class,
        BodyMeasurementEntity::class,
        MealPrepItemEntity::class,
        AchievementEntity::class,
        RecoveryLogEntity::class,
        HealthConnectImportEntity::class,
        EnvironmentTaskEntity::class,
        DailyCheckinEntity::class,
        ReflectionHistoryEntity::class,
        FriendConnectionEntity::class,
        AppSettingsEntity::class,
        DailyStateEntity::class,
        RewardEventEntity::class,
        LegacyRewardExclusionEntity::class,
        CycleEntryEntity::class,
        ActivityTrackEntity::class,
        ActiveTrackingEntity::class,
        TrackingDayTotalEntity::class,
        RoutePointEntity::class,
        ReminderEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class NewMeDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dailyChecklistDao(): DailyChecklistDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun waterDao(): WaterDao
    abstract fun sleepDao(): SleepDao
    abstract fun supplementDao(): SupplementDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun mealPrepDao(): MealPrepDao
    abstract fun achievementDao(): AchievementDao
    abstract fun recoveryDao(): RecoveryDao
    abstract fun healthConnectImportDao(): HealthConnectImportDao
    abstract fun environmentTaskDao(): EnvironmentTaskDao
    abstract fun dailyCheckinDao(): DailyCheckinDao
    abstract fun reflectionHistoryDao(): ReflectionHistoryDao
    abstract fun friendConnectionDao(): FriendConnectionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun dailyStateDao(): DailyStateDao
    abstract fun rewardDao(): RewardDao
    abstract fun cycleDao(): CycleDao
    abstract fun activityTrackingDao(): ActivityTrackingDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: NewMeDatabase? = null

        fun getInstance(context: Context): NewMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewMeDatabase::class.java,
                    "newme_master.db"
                )
                    .addMigrations(*DatabaseMigrations.ALL)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: NewMeDatabase) {
            val initialAchievements = listOf(
                AchievementEntity("first_step", "FIRST STEP", "Complete your first NewMe action", "ic_flag", isUnlocked = false, unlockedDate = null),
                AchievementEntity("first_day", "FIRST DAY", "Complete your first day", "ic_flag", isUnlocked = false, unlockedDate = null),
                AchievementEntity("seven_day_momentum", "7 DAY MOMENTUM", "Maintain 7 successful days", "ic_fire", isUnlocked = false, unlockedDate = null),
                AchievementEntity("protein_consistency", "PROTEIN CONSISTENCY", "Hit protein target on 7 days", "ic_nutrition", isUnlocked = false, unlockedDate = null),
                AchievementEntity("activity_builder", "ACTIVITY BUILDER", "Complete 10 activity sessions", "ic_gym", isUnlocked = false, unlockedDate = null),
                AchievementEntity("meal_prep_builder", "MEAL PREP BUILDER", "Complete meal preparation multiple times", "ic_kitchen", isUnlocked = false, unlockedDate = null),
                AchievementEntity("recovery_builder", "RECOVERY BUILDER", "Successfully return after an interrupted day", "ic_restart", isUnlocked = false, unlockedDate = null),
                AchievementEntity("thirty_day_momentum", "30 DAY MOMENTUM", "Build 30 successful days", "ic_star", isUnlocked = false, unlockedDate = null)
            )
            database.achievementDao().insertAll(initialAchievements)
        }
    }
}
