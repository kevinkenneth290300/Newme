package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.content.ReflectionLibrary
import com.example.data.local.content.ReflectionItem
import com.example.data.local.entity.*
import com.example.data.model.OnboardingPreferences
import com.example.data.model.DailyGoalKind
import com.example.data.model.ActivityKind
import com.example.util.NutritionCalculator
import com.example.util.DeviceClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.Clock
import java.time.format.DateTimeFormatter

data class DisciplineScoreBreakdown(
    val totalScore: Int = 0,
    val nutritionScore: Int = 0,
    val nutritionMax: Int = 25,
    val proteinScore: Int = 0,
    val proteinMax: Int = 15,
    val workoutScore: Int = 0,
    val workoutMax: Int = 20,
    val waterScore: Int = 0,
    val waterMax: Int = 10,
    val sleepScore: Int = 0,
    val sleepMax: Int = 15,
    val supplementScore: Int = 0,
    val supplementMax: Int = 5,
    val mealPrepScore: Int = 0,
    val mealPrepMax: Int = 5,
    val habitScore: Int = 0,
    val habitMax: Int = 5,
    val hasTargetHistory: Boolean = true
)

data class LevelInfo(
    val level: Int,
    val currentXp: Int,
    val currentLevelBaseXp: Int,
    val nextLevelTargetXp: Int,
    val progress: Float
)

class NewMeRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    private val gamification = GamificationRepository(database, clock)

    private val userProfileDao = database.userProfileDao()
    private val dailyChecklistDao = database.dailyChecklistDao()
    private val nutritionDao = database.nutritionDao()
    private val waterDao = database.waterDao()
    private val sleepDao = database.sleepDao()
    private val supplementDao = database.supplementDao()
    private val workoutDao = database.workoutDao()
    private val bodyMeasurementDao = database.bodyMeasurementDao()
    private val mealPrepDao = database.mealPrepDao()
    private val achievementDao = database.achievementDao()
    private val recoveryDao = database.recoveryDao()
    private val activitySessionDao = database.activitySessionDao()
    private val healthConnectImportDao = database.healthConnectImportDao()
    private val environmentTaskDao = database.environmentTaskDao()
    private val dailyCheckinDao = database.dailyCheckinDao()
    private val reflectionHistoryDao = database.reflectionHistoryDao()
    private val friendConnectionDao = database.friendConnectionDao()

    // User Profile & Gamification
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    suspend fun saveProfile(profile: UserProfileEntity) = database.withTransaction {
        NutritionCalculator.validateMetrics(profile.age, profile.heightCm, profile.currentWeightKg,
            profile.targetWeightKg, profile.bodyFatPercent, profile.skeletalMuscleKg, profile.waistCm, profile.inBodyScore)
        val existing = userProfileDao.getUserProfileDirect()
        userProfileDao.insertOrUpdateProfile(if (existing == null) profile else profile.copy(
            startDate = existing.startDate, initialWeightKg = existing.initialWeightKg,
            currentXp = existing.currentXp, currentLevel = existing.currentLevel,
            currentStreak = existing.currentStreak, longestStreak = existing.longestStreak,
            perfectDays = existing.perfectDays, recoveryDays = existing.recoveryDays,
            currentMomentum = existing.currentMomentum, isRecoveryActive = existing.isRecoveryActive,
            isOnboarded = existing.isOnboarded
        ))
        updateTodayTargets(profile)
    }

    suspend fun completeOnboarding(profile: UserProfileEntity, preferences: OnboardingPreferences? = null) = database.withTransaction {
        require(profile.name.trim().isNotEmpty() && profile.name.length <= 80) { "Enter a name of 1 to 80 characters." }
        NutritionCalculator.validateMetrics(profile.age, profile.heightCm, profile.currentWeightKg,
            profile.targetWeightKg, profile.bodyFatPercent, profile.skeletalMuscleKg, profile.waistCm, profile.inBodyScore)
        val today = LocalDate.now(clock).toString()
        val existing = userProfileDao.getUserProfileDirect()
        val isRecalibration = existing?.let { it.isOnboarded || it.startDate.isNotBlank() } == true
        val onboardedProfile = profile.copy(
            isOnboarded = true,
            hasSeenFirstMorningWelcome = existing?.hasSeenFirstMorningWelcome ?: false,
            startDate = existing?.startDate?.takeIf { it.isNotBlank() }
                ?: profile.startDate.ifBlank { today },
            initialWeightKg = if (isRecalibration) existing!!.initialWeightKg else profile.initialWeightKg,
            currentLevel = existing?.currentLevel ?: 1,
            currentXp = existing?.currentXp ?: 0,
            currentStreak = existing?.currentStreak ?: 0,
            longestStreak = existing?.longestStreak ?: 0,
            perfectDays = existing?.perfectDays ?: 0,
            recoveryDays = existing?.recoveryDays ?: 0,
            isRecoveryActive = existing?.isRecoveryActive ?: false,
            currentMomentum = existing?.currentMomentum ?: 0
        )
        userProfileDao.insertOrUpdateProfile(onboardedProfile)
        if (preferences != null) {
            preferences.dailyGoalKind.validate(preferences.dailyGoalTarget)
            val settings = database.settingsDao().get() ?: AppSettingsEntity()
            database.settingsDao().save(settings.copy(dailyGoalType = preferences.dailyGoalKind.name,
                dailyGoalTarget = preferences.dailyGoalTarget, goalChosen = true, cycleEnabled = preferences.cycleEnabled))
            database.dailyStateDao().updateGoal(today, preferences.dailyGoalKind.name, preferences.dailyGoalTarget)
        }
        NewMeDatabase.seedInitialData(database)
        updateTodayTargets(profile)
        if (!isRecalibration) {
            if (bodyMeasurementDao.getMeasurementForDateDirect(today) == null) {
                bodyMeasurementDao.insertMeasurement(
                    BodyMeasurementEntity(
                        date = today,
                        weightKg = profile.currentWeightKg,
                        bodyFatPercent = profile.bodyFatPercent,
                        skeletalMuscleKg = profile.skeletalMuscleKg,
                        waistCm = profile.waistCm,
                        notes = "Onboarding baseline"
                    )
                )
            }
            ensureDateInitialized(today)
        }
    }

    suspend fun setFirstMorningWelcomeSeen(seen: Boolean = true) = database.withTransaction {
        val current = userProfileDao.getUserProfileDirect()
        if (current != null) {
            userProfileDao.insertOrUpdateProfile(current.copy(hasSeenFirstMorningWelcome = seen))
        }
    }

    suspend fun saveWelcomeIntention(mood: String, intention: String) = database.withTransaction {
        require(mood.isNotBlank() && intention.isNotBlank()) { "Choose your mood and intention." }
        val date = LocalDate.now(clock).toString()
        val existing = dailyCheckinDao.getCheckinDirect(date, "MORNING")
        saveDailyCheckin((existing ?: DailyCheckinEntity(date = date, timeOfDay = "MORNING"))
            .copy(mood = mood, mattersMost = intention, isCompleted = true))
        setFirstMorningWelcomeSeen()
    }

    suspend fun resetOnboarding() = database.withTransaction {
        val current = userProfileDao.getUserProfileDirect()
        if (current != null) {
            userProfileDao.insertOrUpdateProfile(current.copy(isOnboarded = false))
        }
    }

    fun calculateLevelInfo(totalXp: Int): LevelInfo = Levels.info(totalXp)

    // Daily Checklist
    fun getChecklistForDate(date: String): Flow<List<DailyChecklistItemEntity>> =
        dailyChecklistDao.getChecklistForDate(date)

    suspend fun toggleChecklistItem(item: DailyChecklistItemEntity) = database.withTransaction {
        val stored = dailyChecklistDao.getItem(item.id) ?: return@withTransaction
        require(!LocalDate.parse(stored.date).isAfter(LocalDate.now(clock))) { "Only current or past actions can be completed." }
        val now = LocalTime.now(clock).format(DateTimeFormatter.ofPattern("hh:mm a"))
        val newState = !item.isCompleted
        if (stored.isCompleted == newState) return@withTransaction
        dailyChecklistDao.setCompleted(stored.id, newState, if (newState) now else "")
        if (newState) {
            gamification.reward("task:${stored.id}", stored.date, stored.xpReward.coerceIn(1, 100))
        }
    }

    suspend fun addChecklistItem(item: DailyChecklistItemEntity) {
        require(item.title.isNotBlank() && item.title.length <= 80) { "Enter a task title of 1 to 80 characters." }
        LocalDate.parse(item.date)
        dailyChecklistDao.insertItem(item)
    }

    suspend fun deleteChecklistItem(item: DailyChecklistItemEntity) {
        dailyChecklistDao.deleteItem(item)
    }

    suspend fun ensureDateInitialized(date: String) = database.withTransaction {
        val profile = userProfileDao.getUserProfileDirect()?.takeIf { it.isOnboarded } ?: return@withTransaction
        if (database.dailyStateDao().get(date) != null) return@withTransaction
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        database.dailyStateDao().insert(DailyStateEntity(
            date = date, goalType = settings.dailyGoalType, goalTarget = settings.dailyGoalTarget,
            goalEnabled = settings.goalChosen && date == LocalDate.now(clock).toString(), calorieTarget = profile.calorieTarget,
            proteinTargetGrams = profile.proteinTargetGrams, waterTargetMl = (profile.waterTargetLiters * 1000).toInt(),
            sleepTargetMinutes = (profile.sleepTargetHours * 60).toInt(),
            targetsKnown = date == LocalDate.now(clock).toString(),
            activityEnabled = profile.selectedActivities.isNotBlank()
        ))
        val existingMeals = nutritionDao.getMealsForDateDirect(date)
        if (profile.usesRecommendedPlan && existingMeals.isEmpty()) {
            nutritionDao.insertMeals(MealSuggestions.forDate(profile, date))
        }
    }

    // Nutrition & Protein
    fun getMealsForDate(date: String): Flow<List<MealPlanEntity>> = nutritionDao.getMealsForDate(date)

    suspend fun toggleMealCompleted(meal: MealPlanEntity) = database.withTransaction {
        val stored = nutritionDao.getMeal(meal.id) ?: return@withTransaction
        validateLoggedDate(stored.date)
        val newState = !meal.isCompleted
        if (stored.isCompleted == newState) return@withTransaction
        nutritionDao.setMealCompleted(stored.id, newState)
        if (newState) {
            gamification.reward("meal:${stored.id}", stored.date, 10)
        }
    }

    suspend fun addMeal(meal: MealPlanEntity) = nutritionDao.insertMeal(meal)
    suspend fun updateMeal(meal: MealPlanEntity) = nutritionDao.updateMeal(meal)
    suspend fun deleteMeal(meal: MealPlanEntity) = nutritionDao.deleteMeal(meal)

    fun getProteinLogsForDate(date: String): Flow<List<ProteinLogEntity>> =
        nutritionDao.getProteinLogsForDate(date)

    suspend fun logProteinFood(date: String, foodName: String, portionDesc: String, proteinGrams: Int) = database.withTransaction {
        validateLoggedDate(date)
        require(foodName.isNotBlank() && proteinGrams in 1..500) { "Enter a food name and 1 to 500 grams of protein." }
        ensureDateInitialized(date)
        val id = nutritionDao.insertProteinLog(
            ProteinLogEntity(date = date, foodName = foodName, portionDesc = portionDesc, proteinGrams = proteinGrams)
        )
        gamification.reward("protein:$id", date, 10)
    }

    suspend fun deleteProteinLog(log: ProteinLogEntity) = nutritionDao.deleteProteinLog(log)

    // Water
    fun getWaterLogForDate(date: String): Flow<WaterLogEntity?> = waterDao.getWaterLogForDate(date)

    suspend fun addWater(date: String, amountDeltaMl: Int) = database.withTransaction {
        validateLoggedDate(date)
        ensureDateInitialized(date)
        val current = waterDao.getLog(date)?.amountMl ?: 0
        val newAmount = (current.toLong() + amountDeltaMl).coerceAtLeast(0L)
        require(newAmount <= 20_000) { "Enter a daily water total no greater than 20,000 ml." }
        waterDao.insertOrUpdateWaterLog(WaterLogEntity(date = date, amountMl = newAmount.toInt()))
        val target = database.dailyStateDao().get(date)?.waterTargetMl ?: 0
        if (target > 0 && newAmount >= target) {
            gamification.reward("water:$date", date, 15)
        }
    }

    suspend fun setWater(date: String, amountMl: Int) = database.withTransaction {
        validateLoggedDate(date)
        require(amountMl in 0..20_000) { "Enter water from 0 to 20,000 ml." }
        ensureDateInitialized(date)
        waterDao.insertOrUpdateWaterLog(WaterLogEntity(date, amountMl))
        val target = database.dailyStateDao().get(date)?.waterTargetMl ?: 0
        if (target > 0 && amountMl >= target) gamification.reward("water:$date", date, 15)
    }

    // Sleep
    fun getSleepLogForDate(date: String): Flow<SleepLogEntity?> = sleepDao.getSleepLogForDate(date)
    fun getRecentSleepLogs(): Flow<List<SleepLogEntity>> = sleepDao.getRecentSleepLogs()

    suspend fun logSleep(date: String, bedtime: String, wakeTime: String, durationMinutes: Int, quality: Int) = database.withTransaction {
        validateLoggedDate(date)
        require(durationMinutes in 1..1440 && quality in 1..5) { "Enter a sleep duration of 1 to 1440 minutes and a quality of 1 to 5." }
        ensureDateInitialized(date)
        sleepDao.insertOrUpdateSleepLog(
            SleepLogEntity(date = date, bedtime = bedtime, wakeTime = wakeTime, durationMinutes = durationMinutes, qualityRating = quality)
        )
        val target = database.dailyStateDao().get(date)?.sleepTargetMinutes ?: 0
        if (target > 0 && durationMinutes >= target) {
            gamification.reward("sleep:$date", date, 20)
        }
    }

    // Supplements
    fun getAllSupplements(): Flow<List<SupplementEntity>> = supplementDao.getAllSupplements()
    fun getSupplementLogsForDate(date: String): Flow<List<SupplementLogEntity>> = supplementDao.getSupplementLogsForDate(date)

    suspend fun toggleSupplementTaken(supplement: SupplementEntity, date: String, isTaken: Boolean) = database.withTransaction {
        validateLoggedDate(date)
        val existing = supplementDao.getLog(supplement.id, date)
        if (isTaken) {
            supplementDao.deleteSupplementLog(supplement.id, date)
        } else if (existing == null) {
            val now = LocalTime.now(clock).format(DateTimeFormatter.ofPattern("hh:mm a"))
            supplementDao.insertSupplementLog(
                SupplementLogEntity(supplementId = supplement.id, supplementName = supplement.name, date = date, takenTime = now)
            )
            gamification.reward("supplement:${supplement.id}:$date", date, 5, "supplement-definition:${supplement.id}")
        }
    }

    suspend fun addSupplement(supplement: SupplementEntity) = supplementDao.insertSupplement(supplement)
    suspend fun updateSupplement(supplement: SupplementEntity) = supplementDao.updateSupplement(supplement)
    suspend fun deleteSupplement(supplement: SupplementEntity) = supplementDao.deleteSupplement(supplement)

    // Workout & Cardio
    fun getWorkoutSessionForDate(date: String): Flow<WorkoutSessionEntity?> = workoutDao.getWorkoutSessionForDate(date)
    fun getExercisesForSession(sessionId: Long): Flow<List<ExerciseLogEntity>> = workoutDao.getExercisesForSession(sessionId)

    suspend fun saveWorkoutSession(session: WorkoutSessionEntity): Long = database.withTransaction {
        validateLoggedDate(session.date)
        require(session.durationMinutes in 0..1440 && (!session.isCompleted || session.durationMinutes > 0)) { "Enter a positive completed workout duration." }
        ensureDateInitialized(session.date)
        val existing = workoutDao.getSession(session.date)
        val id = workoutDao.insertOrUpdateWorkoutSession(session.copy(id = existing?.id ?: session.id))
        if (session.isCompleted) {
            gamification.reward("workout:${session.date}", session.date, 30)
        }
        id
    }

    suspend fun addExercise(exercise: ExerciseLogEntity) = workoutDao.insertExercise(exercise)
    suspend fun updateExercise(exercise: ExerciseLogEntity) = workoutDao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: ExerciseLogEntity) = workoutDao.deleteExercise(exercise)

    fun getCardioLogsForDate(date: String): Flow<List<CardioLogEntity>> = workoutDao.getCardioLogsForDate(date)
    fun getRecentCardioLogs(): Flow<List<CardioLogEntity>> = workoutDao.getRecentCardioLogs()

    suspend fun logCardio(cardio: CardioLogEntity) = database.withTransaction {
        validateLoggedDate(cardio.date)
        require(cardio.durationMinutes in 1..1440 && cardio.distanceKm.isFinite() && cardio.distanceKm >= 0f) { "Enter a valid duration and distance." }
        ensureDateInitialized(cardio.date)
        val id = workoutDao.insertCardioLog(cardio)
        gamification.reward("cardio:$id", cardio.date, 10)
    }

    suspend fun deleteCardio(cardio: CardioLogEntity) = workoutDao.deleteCardioLog(cardio)

    // Body Measurements
    fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>> = bodyMeasurementDao.getAllMeasurements()
    fun getMeasurementForDate(date: String): Flow<BodyMeasurementEntity?> = bodyMeasurementDao.getMeasurementForDate(date)

    suspend fun saveMeasurement(measurement: BodyMeasurementEntity) = database.withTransaction {
        validateLoggedDate(measurement.date)
        val currentProfile = userProfileDao.getUserProfileDirect()
            ?: throw IllegalStateException("Complete setup before recording measurements.")
        NutritionCalculator.validateMetrics(currentProfile.age, currentProfile.heightCm, measurement.weightKg,
            bodyFatPercent = measurement.bodyFatPercent, skeletalMuscleKg = measurement.skeletalMuscleKg,
            waistCm = measurement.waistCm, inBodyScore = measurement.inBodyScore)
        require(measurement.bmi == null || (measurement.bmi.isFinite() && measurement.bmi > 0f)) { "Enter a valid BMI or leave it unknown." }
        require(measurement.visceralFat == null || (measurement.visceralFat.isFinite() && measurement.visceralFat >= 0f)) { "Enter a valid optional measurement." }
        val existing = bodyMeasurementDao.getMeasurementForDateDirect(measurement.date)
        bodyMeasurementDao.insertMeasurement(measurement.copy(id = measurement.id.takeIf { it > 0 } ?: existing?.id ?: 0))
        val latest = bodyMeasurementDao.getLatestMeasurement()
        if (latest?.date == measurement.date) {
            userProfileDao.insertOrUpdateProfile(
                currentProfile.copy(
                    currentWeightKg = measurement.weightKg,
                    bodyFatPercent = measurement.bodyFatPercent ?: currentProfile.bodyFatPercent,
                    skeletalMuscleKg = measurement.skeletalMuscleKg ?: currentProfile.skeletalMuscleKg
                )
            )
        }
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurementEntity) =
        bodyMeasurementDao.deleteMeasurement(measurement)

    // Meal Prep Inventory
    fun getAllMealPrepItems(): Flow<List<MealPrepItemEntity>> = mealPrepDao.getAllMealPrepItems()

    suspend fun addMealPrepItem(item: MealPrepItemEntity) = database.withTransaction {
        validateLoggedDate(item.prepDate)
        require(item.foodName.isNotBlank() && item.portionsAvailable in 1..100) { "Enter a food and 1 to 100 prepared portions." }
        val id = mealPrepDao.insertMealPrepItem(item)
        gamification.reward("mealprep:$id", item.prepDate, 15)
    }

    suspend fun consumeMealPrepPortion(item: MealPrepItemEntity) = database.withTransaction {
        val stored = mealPrepDao.getItem(item.id) ?: return@withTransaction
        if (stored.portionsAvailable != item.portionsAvailable) return@withTransaction
        if (stored.portionsAvailable > 1) {
            mealPrepDao.updateMealPrepItem(stored.copy(portionsAvailable = stored.portionsAvailable - 1))
        } else {
            mealPrepDao.deleteMealPrepItem(stored)
        }
    }

    suspend fun deleteMealPrepItem(item: MealPrepItemEntity) = mealPrepDao.deleteMealPrepItem(item)

    // Recovery & Falling Off
    fun getRecoveryForDate(date: String): Flow<RecoveryLogEntity?> = recoveryDao.getRecoveryForDate(date)

    suspend fun applyRecoveryPlan(date: String, obstacle: String, planText: String) = database.withTransaction {
        validateLoggedDate(date)
        val existing = recoveryDao.getLog(date)
        recoveryDao.insertOrUpdateRecovery(
            RecoveryLogEntity(id = existing?.id ?: 0, date = date, obstacleReason = obstacle, planSelected = planText, isCompleted = false)
        )
        val profile = userProfileDao.getUserProfileDirect() ?: return@withTransaction
        userProfileDao.updateStreakStats(
            streak = profile.currentStreak,
            longest = profile.longestStreak,
            perfect = profile.perfectDays,
            recovery = profile.recoveryDays,
            recoveryActive = true
        )
    }

    suspend fun restoreMomentum() = database.withTransaction {
        val profile = userProfileDao.getUserProfileDirect() ?: return@withTransaction
        userProfileDao.updateStreakStats(
            streak = profile.currentStreak,
            longest = profile.longestStreak,
            perfect = profile.perfectDays,
            recovery = profile.recoveryDays,
            recoveryActive = false
        )
    }

    // Activity Sessions (Gym, Walking, Running, Yoga, Dance, Sports, etc.)
    val allActivitySessions: Flow<List<ActivitySessionEntity>> = activitySessionDao.getAllActivitySessions()
    fun getActivitySessionsForDate(date: String): Flow<List<ActivitySessionEntity>> = activitySessionDao.getActivitySessionsForDate(date)

    suspend fun logActivitySession(session: ActivitySessionEntity): Long = database.withTransaction {
        validateLoggedDate(session.date)
        require(session.durationMinutes in 1..1440 && session.distanceKm.isFinite() && session.distanceKm >= 0f && session.steps >= 0) { "Enter a valid activity duration, distance, and step count." }
        ensureDateInitialized(session.date)
        val existing = if (session.id > 0) activitySessionDao.getSession(session.id)
            else session.externalRecordId?.let { activitySessionDao.getSessionByKey(it) }
        val id = activitySessionDao.insertActivitySession(session.copy(id = existing?.id ?: session.id))
        if (session.isCompleted) gamification.reward("activity:$id", session.date, 25)
        id
    }

    suspend fun deleteActivitySession(session: ActivitySessionEntity) =
        activitySessionDao.deleteActivitySession(session)

    // Health Connect
    fun getRecentHealthConnectImports(): Flow<List<HealthConnectImportEntity>> =
        healthConnectImportDao.getRecentImports()

    suspend fun importHealthConnectRecord(recordId: String, dataType: String, value: String, date: String): Boolean {
        if (healthConnectImportDao.isRecordImported(recordId)) return false
        val item = HealthConnectImportEntity(
            recordId = recordId,
            dataType = dataType,
            importedTimestamp = System.currentTimeMillis(),
            value = value,
            date = date
        )
        healthConnectImportDao.insertImport(item)
        return true
    }

    // Achievements
    fun getAllAchievements(): Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()

    // Environment & Life Maintenance
    fun getEnvironmentTasksForDate(date: String): Flow<List<EnvironmentTaskEntity>> =
        environmentTaskDao.getTasksForDate(date)

    suspend fun toggleEnvironmentTask(item: EnvironmentTaskEntity) = database.withTransaction {
        val stored = environmentTaskDao.getTask(item.id) ?: return@withTransaction
        validateLoggedDate(stored.date)
        val now = LocalTime.now(clock).format(DateTimeFormatter.ofPattern("hh:mm a"))
        val newState = !item.isCompleted
        if (stored.isCompleted == newState) return@withTransaction
        environmentTaskDao.setTaskCompleted(stored.id, newState, if (newState) now else "")
        if (newState) {
            gamification.reward("environment:${stored.id}", stored.date, 10)
        }
    }

    suspend fun addEnvironmentTask(task: EnvironmentTaskEntity) {
        environmentTaskDao.insertTask(task)
    }

    suspend fun deleteEnvironmentTask(task: EnvironmentTaskEntity) {
        environmentTaskDao.deleteTask(task)
    }

    suspend fun ensureEnvironmentTasksForDate(date: String) = ensureDateInitialized(date)

    // Daily Companion Check-ins (Morning, Midday, Evening)
    fun getDailyCheckin(date: String, timeOfDay: String): Flow<DailyCheckinEntity?> =
        dailyCheckinDao.getCheckin(date, timeOfDay)

    fun getAllCheckinsForDate(date: String): Flow<List<DailyCheckinEntity>> =
        dailyCheckinDao.getAllCheckinsForDate(date)

    suspend fun saveDailyCheckin(checkin: DailyCheckinEntity) = database.withTransaction {
        validateLoggedDate(checkin.date)
        require(checkin.timeOfDay in setOf("MORNING", "MIDDAY", "EVENING")) { "Choose morning, midday, or evening." }
        val existing = dailyCheckinDao.getCheckinDirect(checkin.date, checkin.timeOfDay)
        dailyCheckinDao.insertOrUpdateCheckin(checkin.copy(id = existing?.id ?: 0))
        if (checkin.isCompleted) gamification.reward("checkin:${checkin.date}:${checkin.timeOfDay}", checkin.date, 20)
    }

    // Contextual Reflection Engine (80-120 Stoic Items with repetition prevention)
    suspend fun getReflectionForContext(timeOfDay: String, mood: String? = null): ReflectionItem {
        val recent = reflectionHistoryDao.getRecentReflectionIds(20)
        return ReflectionLibrary.getReflection(timeOfDay, mood, recent)
    }

    suspend fun getDailyReflection(timeOfDay: String, mood: String? = null): ReflectionItem =
        getReflectionForContext(timeOfDay, mood)

    suspend fun recordReflectionShown(reflectionId: String, date: String, timeOfDay: String) = database.withTransaction {
        if (reflectionHistoryDao.wasShown(reflectionId, date, timeOfDay)) return@withTransaction
        reflectionHistoryDao.recordReflectionShown(
            ReflectionHistoryEntity(
                reflectionId = reflectionId,
                date = date,
                timeOfDay = timeOfDay,
                timestamp = clock.millis()
            )
        )
    }

    // Friend Connections (Architecture ready for V2, zero mock data)
    fun getAllFriends(): Flow<List<FriendConnectionEntity>> = friendConnectionDao.getAllFriends()

    suspend fun addFriend(code: String, name: String) {
        friendConnectionDao.insertFriend(
            FriendConnectionEntity(
                friendCode = code.trim().uppercase(),
                friendName = name.trim().ifBlank { "Discipline Ally" }
            )
        )
    }

    suspend fun addFriend(friend: FriendConnectionEntity) {
        friendConnectionDao.insertFriend(friend)
    }

    suspend fun updateFriendStatus(id: Long, momentum: Int, lastActive: String, status: String) {
        friendConnectionDao.updateFriendStatus(id, momentum, lastActive, status)
    }

    suspend fun deleteFriend(friend: FriendConnectionEntity) {
        friendConnectionDao.deleteFriend(friend)
    }

    suspend fun deleteFriend(id: Long) {
        friendConnectionDao.deleteFriendById(id)
    }

    // Reset Data - Complete clean slate (no fake history)
    suspend fun resetAllData() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    private fun validateLoggedDate(date: String) {
        val parsed = LocalDate.parse(date)
        require(parsed.toString() == date && parsed.year >= 1900 && !parsed.isAfter(LocalDate.now(clock))) { "Choose a valid date no later than today." }
    }

    suspend fun setDailyGoal(kind: DailyGoalKind, target: Int) = database.withTransaction {
        kind.validate(target)
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(settings.copy(dailyGoalType = kind.name, dailyGoalTarget = target, goalChosen = true))
        val today = LocalDate.now(clock).toString()
        ensureDateInitialized(today)
        database.dailyStateDao().updateGoal(today, kind.name, target)
    }

    private suspend fun updateTodayTargets(profile: UserProfileEntity) {
        database.dailyStateDao().updateTargets(LocalDate.now(clock).toString(), profile.calorieTarget, profile.proteinTargetGrams,
            (profile.waterTargetLiters * 1000).toInt(), (profile.sleepTargetHours * 60).toInt(), profile.selectedActivities.isNotBlank())
    }
}
