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
        ReminderEntity::class,
        RoutineTemplateEntity::class,
        RoutineCompletionEntity::class,
        DaySummaryEntity::class,
        SummaryDirtyEntity::class,
        MomentLogEntity::class
    ],
    version = 7,
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
    abstract fun routineDao(): RoutineDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun momentDao(): MomentDao

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
                    .addCallback(object : Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) { SummaryTracking.install(db) }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: NewMeDatabase) {
            val initialAchievements = com.example.data.repository.BadgeCatalog.all.mapIndexed { index, badge ->
                AchievementEntity(badge.id, badge.id, badge.id, "badge:$index", isUnlocked = false, unlockedDate = null)
            }
            database.achievementDao().insertAll(initialAchievements)
        }
    }
}
