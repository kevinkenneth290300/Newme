package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 25,
    val sex: String = "Male", // Male, Female, Other
    val heightCm: Float = 170f,
    val initialWeightKg: Float = 70f,
    val currentWeightKg: Float = 70f,
    val targetWeightKg: Float? = null,
    val bodyFatPercent: Float? = null,
    val skeletalMuscleKg: Float? = null,
    val waistCm: Float? = null,
    val targetBodyFatPercent: Float? = null,
    val targetWaistCm: Float? = null,
    val primaryActivityType: String = "Gym / Strength Training",
    val primaryGoal: String = "Discipline/self-growth",
    val bmr: Int = 1600,
    val tdee: Int = 2200,
    val calorieTarget: Int = 2000,
    val proteinTargetGrams: Int = 90,
    val carbsTargetGrams: Int = 200,
    val fatTargetGrams: Int = 60,
    val waterTargetLiters: Float = 2.5f,
    val sleepTargetHours: Float = 7.5f,
    val weeklyWorkoutsTarget: Int = 3,
    val goalDescription: String = "",
    val selectedGoals: String = "", // Comma-separated list of goals
    val selectedActivities: String = "", // Comma-separated list of activities
    val activityFrequencies: String = "", // Formatted frequencies e.g. "Gym: 4 days/wk; Walking: Daily"
    val trackingLevel: String = "Focused", // Minimal, Habit, Focused, Intensive
    val dietaryPreference: String = "Vegetarian", // Vegetarian, Eggetarian, Non-vegetarian, Vegan, Custom
    val usesSupplements: Boolean = false,
    val usesRecommendedPlan: Boolean = false,
    val usesHealthConnect: Boolean = false,
    val voiceEnabled: Boolean = false,
    val speakMorning: Boolean = true,
    val speakMidday: Boolean = false,
    val speakEvening: Boolean = true,
    val startDate: String = "",
    val isOnboarded: Boolean = false,
    val currentLevel: Int = 1,
    val currentXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val perfectDays: Int = 0,
    val recoveryDays: Int = 0,
    val isRecoveryActive: Boolean = false,
    val currentMomentum: Int = 0,
    val identityTraits: String = "",
    val startingReason: String = "",
    val progressGoalType: String = "General health goal",
    val wakeTimeSlot: String = "6–7",
    val sleepTimeSlot: String = "10–11",
    val workType: String = "Office",
    val trainingFrequency: String = "3 days/week",
    val difficultTimeOfDay: String = "Evening",
    val dietType: String = "Vegetarian",
    val commonFoods: String = "",
    val avoidedFoods: String = "",
    val cookingSituation: String = "I cook",
    val mealPrepHabit: String = "Sometimes",
    val companionTone: String = "BALANCED",
    val friendCode: String = "",
    val hasSeenFirstMorningWelcome: Boolean = false,
    val bmi: Float? = null,
    val inBodyScore: Int? = null,
    val visceralFat: Float? = null
)

@Entity(tableName = "daily_checklist")
data class DailyChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val section: String, // MORNING, LUNCH, ACTIVITY, DINNER, NIGHT
    val title: String,
    val isCompleted: Boolean = false,
    val completionTime: String = "",
    val xpReward: Int = 15,
    val orderIndex: Int = 0
)

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val slot: String, // Morning, Lunch, Pre-workout, Post-workout, Dinner
    val name: String,
    val time: String,
    val foodsDescription: String,
    val approxCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val isCompleted: Boolean = false,
    val source: String = "Recommended Plan" // Recommended Plan, Manual, Health Connect
)

@Entity(tableName = "protein_logs")
data class ProteinLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val foodName: String,
    val portionDesc: String,
    val proteinGrams: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Manual"
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey val date: String,
    val amountMl: Int = 0,
    val source: String = "Manual"
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val date: String,
    val bedtime: String = "11:00 PM",
    val wakeTime: String = "06:30 AM",
    val durationMinutes: Int = 0,
    val qualityRating: Int = 3,
    val source: String = "Manual"
)

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val timing: String, // Morning, Lunch, Dinner, Night
    val category: String = "Personal choice", // Personal choice, Doctor prescribed
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "supplement_logs")
data class SupplementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementId: Long,
    val supplementName: String,
    val date: String,
    val takenTime: String
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String = "Strength Training",
    val durationMinutes: Int = 45,
    val warmUpDone: Boolean = false,
    val strengthDone: Boolean = false,
    val cardioDone: Boolean = false,
    val coolDownDone: Boolean = false,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val source: String = "NewMe"
)

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val category: String,
    val setsSummary: String,
    val previousPerformance: String = ""
)

@Entity(tableName = "cardio_logs")
data class CardioLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val cardioType: String, // Walking, Treadmill, Cycling, Cross trainer, Running, Other
    val durationMinutes: Int,
    val distanceKm: Float = 0f,
    val intensity: String = "Moderate",
    val calories: Int = 0,
    val source: String = "Manual"
)

@Entity(tableName = "activity_sessions")
data class ActivitySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val activityType: String, // Gym, Dance, Running, Cycling, Sports, Yoga, Walking, General Active, Other
    val title: String,
    val durationMinutes: Int,
    val intensity: String = "Moderate", // Light, Moderate, High
    val distanceKm: Float = 0f,
    val steps: Int = 0,
    val paceMinPerKm: String = "",
    val notes: String = "",
    val source: String = "NewMe", // NewMe, Health Connect, Manual
    val externalRecordId: String? = null,
    val isCompleted: Boolean = true
)

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Float,
    val bodyFatPercent: Float? = null,
    val bodyFatMassKg: Float? = null,
    val skeletalMuscleKg: Float? = null,
    val waistCm: Float? = null,
    val bmi: Float? = null,
    val visceralFat: Float? = null,
    val bmr: Int? = null,
    val inBodyScore: Int? = null,
    val notes: String = "",
    val source: String = "Manual"
)

@Entity(tableName = "meal_prep_items")
data class MealPrepItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val portionsAvailable: Int,
    val prepDate: String,
    val expiryDate: String = "",
    val notes: String = ""
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null
)

@Entity(tableName = "recovery_logs")
data class RecoveryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val obstacleReason: String,
    val planSelected: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "health_connect_imports")
data class HealthConnectImportEntity(
    @PrimaryKey val recordId: String,
    val dataType: String, // CALORIES, STEPS, SLEEP, WEIGHT, EXERCISE
    val importedTimestamp: Long,
    val value: String,
    val date: String
)

@Entity(tableName = "environment_tasks")
data class EnvironmentTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String, // MORNING, LEAVING_HOME, AFTER_WORK, BEFORE_SLEEP, WEEKLY
    val title: String,
    val dayOfWeek: String? = null, // Monday, Tuesday, etc. or null for daily
    val isCompleted: Boolean = false,
    val completionTime: String = "",
    val orderIndex: Int = 0
)

@Entity(tableName = "daily_checkins")
data class DailyCheckinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timeOfDay: String, // MORNING, MIDDAY, EVENING
    val mood: String = "",
    val practiceVirtue: String = "",
    val mattersMost: String = "",
    val refuseToLeaveUnfinished: String = "",
    val middayStatus: String = "",
    val completedPercent: Int = 0,
    val reflectionId: String = "",
    val reflectionText: String = "",
    val isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reflection_history")
data class ReflectionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reflectionId: String,
    val date: String,
    val timeOfDay: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_connections")
data class FriendConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendCode: String,
    val friendName: String,
    val status: String = "ACTIVE", // PENDING, ACTIVE, MUTED
    val sharedMomentum: Int = 0,
    val lastActiveDate: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)

