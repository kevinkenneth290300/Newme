package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        FriendConnectionEntity::class
    ],
    version = 4,
    exportSchema = false
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
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial default data in background
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: NewMeDatabase) {
            if (database.userProfileDao().getUserProfileDirect() != null) {
                return
            }

            // 1. Clean Brand New User Profile State (Zero fake history, Day 1, 0 XP, 0 Streak)
            database.userProfileDao().insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1,
                    name = "",
                    age = 25,
                    sex = "Male",
                    heightCm = 170f,
                    initialWeightKg = 70.0f,
                    currentWeightKg = 70.0f,
                    targetWeightKg = null,
                    bodyFatPercent = null,
                    skeletalMuscleKg = null,
                    waistCm = null,
                    primaryActivityType = "Gym / Strength Training",
                    primaryGoal = "Discipline/self-growth",
                    bmr = 1600,
                    tdee = 2200,
                    calorieTarget = 2000,
                    proteinTargetGrams = 90,
                    carbsTargetGrams = 200,
                    fatTargetGrams = 60,
                    waterTargetLiters = 2.5f,
                    sleepTargetHours = 7.5f,
                    weeklyWorkoutsTarget = 3,
                    goalDescription = "",
                    selectedGoals = "",
                    selectedActivities = "",
                    activityFrequencies = "",
                    trackingLevel = "Focused",
                    dietaryPreference = "Vegetarian",
                    usesSupplements = false,
                    usesRecommendedPlan = false,
                    usesHealthConnect = false,
                    voiceEnabled = false,
                    speakMorning = true,
                    speakMidday = false,
                    speakEvening = true,
                    startDate = "",
                    isOnboarded = false,
                    currentLevel = 1,
                    currentXp = 0,
                    currentStreak = 0,
                    longestStreak = 0,
                    perfectDays = 0,
                    recoveryDays = 0,
                    isRecoveryActive = false,
                    currentMomentum = 0
                )
            )

            // 2. Achievements initialized at ZERO - none unlocked
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
