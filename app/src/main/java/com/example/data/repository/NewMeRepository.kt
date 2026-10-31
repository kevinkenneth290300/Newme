package com.example.data.repository

import com.example.data.local.NewMeDatabase
import com.example.data.local.content.ReflectionLibrary
import com.example.data.local.content.ReflectionItem
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
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
    val habitMax: Int = 5
)

data class LevelInfo(
    val level: Int,
    val currentXp: Int,
    val currentLevelBaseXp: Int,
    val nextLevelTargetXp: Int,
    val progress: Float
)

class NewMeRepository(private val database: NewMeDatabase) {

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

    suspend fun saveProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun completeOnboarding(profile: UserProfileEntity) {
        val today = LocalDate.now().toString()
        val onboardedProfile = profile.copy(
            isOnboarded = true,
            hasSeenFirstMorningWelcome = false,
            startDate = if (profile.startDate.isBlank()) today else profile.startDate,
            currentLevel = 1,
            currentXp = 0,
            currentStreak = 0,
            longestStreak = 0,
            perfectDays = 0,
            recoveryDays = 0,
            isRecoveryActive = false,
            currentMomentum = 0
        )
        userProfileDao.insertOrUpdateProfile(onboardedProfile)
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
        // Clear any previous stale items for today to generate fresh personalized plan
        dailyChecklistDao.clearChecklistForDate(today)
        ensureDateInitialized(today)
        ensureEnvironmentTasksForDate(today)
    }

    suspend fun setFirstMorningWelcomeSeen(seen: Boolean = true) {
        val current = userProfileDao.getUserProfileDirect()
        if (current != null) {
            userProfileDao.insertOrUpdateProfile(current.copy(hasSeenFirstMorningWelcome = seen))
        }
    }

    suspend fun resetOnboarding() {
        val current = userProfileDao.getUserProfileDirect()
        if (current != null) {
            userProfileDao.insertOrUpdateProfile(current.copy(isOnboarded = false))
        }
    }

    suspend fun addXp(amount: Int) {
        val currentProfile = userProfileDao.getUserProfileDirect() ?: return
        val newXp = (currentProfile.currentXp + amount).coerceAtLeast(0)
        val newLevel = calculateLevelForXp(newXp)
        userProfileDao.updateXpAndLevel(newXp, newLevel)
    }

    fun calculateLevelInfo(totalXp: Int): LevelInfo {
        val thresholds = listOf(0, 500, 1200, 2000, 3000, 4500, 6500, 9000, 12000, 16000)
        var level = 1
        for (i in thresholds.indices) {
            if (totalXp >= thresholds[i]) {
                level = i + 1
            } else {
                break
            }
        }
        val currentBase = if (level - 1 < thresholds.size) thresholds[level - 1] else thresholds.last()
        val nextTarget = if (level < thresholds.size) thresholds[level] else currentBase + 4000
        val range = (nextTarget - currentBase).coerceAtLeast(1)
        val progress = ((totalXp - currentBase).toFloat() / range).coerceIn(0f, 1f)
        return LevelInfo(level, totalXp, currentBase, nextTarget, progress)
    }

    private fun calculateLevelForXp(totalXp: Int): Int {
        val thresholds = listOf(0, 500, 1200, 2000, 3000, 4500, 6500, 9000, 12000, 16000)
        var level = 1
        for (i in thresholds.indices) {
            if (totalXp >= thresholds[i]) {
                level = i + 1
            } else {
                break
            }
        }
        return level
    }

    // Daily Checklist
    fun getChecklistForDate(date: String): Flow<List<DailyChecklistItemEntity>> =
        dailyChecklistDao.getChecklistForDate(date)

    suspend fun toggleChecklistItem(item: DailyChecklistItemEntity) {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        val newState = !item.isCompleted
        dailyChecklistDao.setCompleted(item.id, newState, if (newState) now else "")
        if (newState) {
            addXp(item.xpReward)
        }
    }

    suspend fun addChecklistItem(item: DailyChecklistItemEntity) {
        dailyChecklistDao.insertItem(item)
    }

    suspend fun deleteChecklistItem(item: DailyChecklistItemEntity) {
        dailyChecklistDao.deleteItem(item)
    }

    suspend fun ensureDateInitialized(date: String) {
        val profile = userProfileDao.getUserProfileDirect()
        val activities = profile?.selectedActivities ?: ""
        val hasGym = activities.contains("Gym", ignoreCase = true)
        val hasWalking = activities.contains("Walking", ignoreCase = true)
        val hasRunning = activities.contains("Running", ignoreCase = true)
        val hasYoga = activities.contains("Yoga", ignoreCase = true)
        val hasDance = activities.contains("Dance", ignoreCase = true)
        val hasSports = activities.contains("Sports", ignoreCase = true)
        val hasGeneralActive = activities.contains("Active", ignoreCase = true)
        val usesSupps = profile?.usesSupplements == true
        val usesRecommendedPlan = profile?.usesRecommendedPlan == true

        val existing = dailyChecklistDao.getChecklistForDateDirect(date)
        if (existing.isEmpty()) {
            val items = mutableListOf<DailyChecklistItemEntity>()
            var order = 0

            // MORNING
            items.add(DailyChecklistItemEntity(date = date, section = "MORNING", title = "Morning Hydration (500ml water)", isCompleted = false, xpReward = 10, orderIndex = order++))
            items.add(DailyChecklistItemEntity(date = date, section = "MORNING", title = if (usesRecommendedPlan) "High-Protein Breakfast (shake, eggs, or chilla)" else "Healthy Breakfast", isCompleted = false, xpReward = 15, orderIndex = order++))
            if (usesSupps) {
                items.add(DailyChecklistItemEntity(date = date, section = "MORNING", title = "Morning Multivitamin / Supplements", isCompleted = false, xpReward = 10, orderIndex = order++))
            }

            // LUNCH
            items.add(DailyChecklistItemEntity(date = date, section = "LUNCH", title = if (usesRecommendedPlan) "Indian Foundation Lunch + 200g Vegetables" else "Balanced Lunch with Protein", isCompleted = false, xpReward = 15, orderIndex = order++))
            items.add(DailyChecklistItemEntity(date = date, section = "LUNCH", title = "1,000ml Water reached by 2:00 PM", isCompleted = false, xpReward = 10, orderIndex = order++))
            items.add(DailyChecklistItemEntity(date = date, section = "LUNCH", title = "No processed junk or sugary drinks", isCompleted = false, xpReward = 15, orderIndex = order++))

            // ACTIVITY / MOVEMENT
            if (hasGym) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Gym warm-up & mobility", isCompleted = false, xpReward = 10, orderIndex = order++))
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Strength training session completed", isCompleted = false, xpReward = 30, orderIndex = order++))
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Log exercise weights & reps", isCompleted = false, xpReward = 10, orderIndex = order++))
            }
            if (hasWalking) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Brisk Walk (30-45 mins / step target)", isCompleted = false, xpReward = 20, orderIndex = order++))
            }
            if (hasRunning) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Running session completed", isCompleted = false, xpReward = 25, orderIndex = order++))
            }
            if (hasYoga) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Yoga flow & posture alignment", isCompleted = false, xpReward = 20, orderIndex = order++))
            }
            if (hasDance) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Dance movement practice session", isCompleted = false, xpReward = 20, orderIndex = order++))
            }
            if (hasSports) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Sports practice / match", isCompleted = false, xpReward = 25, orderIndex = order++))
            }
            if (hasGeneralActive && !hasGym && !hasWalking && !hasRunning) {
                items.add(DailyChecklistItemEntity(date = date, section = "ACTIVITY", title = "Active movement (30+ mins)", isCompleted = false, xpReward = 20, orderIndex = order++))
            }

            // DINNER
            items.add(DailyChecklistItemEntity(date = date, section = "DINNER", title = if (usesRecommendedPlan) "Foundation Dinner (Paneer/Eggs/Shake) + 200g Veg" else "Clean Dinner with Protein", isCompleted = false, xpReward = 15, orderIndex = order++))
            items.add(DailyChecklistItemEntity(date = date, section = "DINNER", title = "Hit daily protein target (${profile?.proteinTargetGrams ?: 90}g)", isCompleted = false, xpReward = 25, orderIndex = order++))

            // NIGHT
            items.add(DailyChecklistItemEntity(date = date, section = "NIGHT", title = "Daily Water target achieved (${profile?.waterTargetLiters ?: 2.5f} L)", isCompleted = false, xpReward = 15, orderIndex = order++))
            items.add(DailyChecklistItemEntity(date = date, section = "NIGHT", title = "Wind down & prepare for ${profile?.sleepTargetHours ?: 7.5f}h sleep", isCompleted = false, xpReward = 15, orderIndex = order++))

            dailyChecklistDao.insertAll(items)
        }

        val existingMeals = nutritionDao.getMealsForDateDirect(date)
        if (existingMeals.isEmpty()) {
            val diet = profile?.dietaryPreference ?: "Vegetarian"
            val defaultMeals = if (usesRecommendedPlan) {
                listOf(
                    MealPlanEntity(
                        date = date,
                        slot = "Morning",
                        name = if (diet == "Eggetarian" || diet == "Non-vegetarian") "3 Whole Eggs + 1 Fruit" else "High-Protein Shake / Besan Chilla",
                        time = "08:00 AM",
                        foodsDescription = if (diet == "Eggetarian" || diet == "Non-vegetarian") "3 farm eggs (boiled or scrambled) + 1 banana/apple" else "Whey scoop in water + 1 banana or 2 moong/besan chillas",
                        approxCalories = 380,
                        proteinGrams = 24,
                        carbsGrams = 35,
                        fatsGrams = 12,
                        source = "Recommended Plan"
                    ),
                    MealPlanEntity(
                        date = date,
                        slot = "Lunch",
                        name = if (diet == "Non-vegetarian") "Grilled Chicken + 200g Veg Bowl" else "Dal Bowl + 200g Vegetables (Palak/Beans)",
                        time = "01:00 PM",
                        foodsDescription = "1 bowl thick home dal, 200g mixed vegetables (gajar, beans, palak) in 1 tsp mustard oil, optional 1 roti or 100g rice",
                        approxCalories = 420,
                        proteinGrams = if (diet == "Non-vegetarian") 36 else 22,
                        carbsGrams = 52,
                        fatsGrams = 10,
                        source = "Recommended Plan"
                    ),
                    MealPlanEntity(
                        date = date,
                        slot = "Pre-workout",
                        name = "Hydration & Energy",
                        time = "04:30 PM",
                        foodsDescription = "1 ripe banana + 300ml water",
                        approxCalories = 105,
                        proteinGrams = 1,
                        carbsGrams = 27,
                        fatsGrams = 0,
                        source = "Recommended Plan"
                    ),
                    MealPlanEntity(
                        date = date,
                        slot = "Post-workout",
                        name = "Protein Recovery",
                        time = "06:30 PM",
                        foodsDescription = "1 scoop whey protein shaken in cold water",
                        approxCalories = 125,
                        proteinGrams = 24,
                        carbsGrams = 2,
                        fatsGrams = 1,
                        source = "Recommended Plan"
                    ),
                    MealPlanEntity(
                        date = date,
                        slot = "Dinner",
                        name = if (diet == "Non-vegetarian") "Grilled Chicken / Fish + Steamed Greens" else if (diet == "Eggetarian") "2 Whole Eggs + 200g Sautéed Veg" else "100g Fresh Paneer + 200g Veggies",
                        time = "08:30 PM",
                        foodsDescription = "High-protein low-carb dinner with 200g seasonal vegetables (cabbage, capsicum, cauliflower) cooked in 1 tsp oil",
                        approxCalories = 340,
                        proteinGrams = 22,
                        carbsGrams = 14,
                        fatsGrams = 18,
                        source = "Recommended Plan"
                    )
                )
            } else {
                listOf(
                    MealPlanEntity(date = date, slot = "Morning", name = "Breakfast", time = "08:00 AM", foodsDescription = "Nutritious breakfast with protein", approxCalories = 400, proteinGrams = 20, carbsGrams = 45, fatsGrams = 10, source = "Manual"),
                    MealPlanEntity(date = date, slot = "Lunch", name = "Lunch", time = "01:00 PM", foodsDescription = "Balanced meal with vegetables", approxCalories = 500, proteinGrams = 25, carbsGrams = 60, fatsGrams = 12, source = "Manual"),
                    MealPlanEntity(date = date, slot = "Dinner", name = "Dinner", time = "08:30 PM", foodsDescription = "Light dinner with protein source", approxCalories = 400, proteinGrams = 25, carbsGrams = 30, fatsGrams = 14, source = "Manual")
                )
            }
            nutritionDao.insertMeals(defaultMeals)
        }

        // Ensure environment & life maintenance tasks are initialized for the date
        ensureEnvironmentTasksForDate(date)
    }

    // Nutrition & Protein
    fun getMealsForDate(date: String): Flow<List<MealPlanEntity>> = nutritionDao.getMealsForDate(date)

    suspend fun toggleMealCompleted(meal: MealPlanEntity) {
        val newState = !meal.isCompleted
        nutritionDao.setMealCompleted(meal.id, newState)
        if (newState) {
            addXp(10)
        }
    }

    suspend fun addMeal(meal: MealPlanEntity) = nutritionDao.insertMeal(meal)
    suspend fun updateMeal(meal: MealPlanEntity) = nutritionDao.updateMeal(meal)
    suspend fun deleteMeal(meal: MealPlanEntity) = nutritionDao.deleteMeal(meal)

    fun getProteinLogsForDate(date: String): Flow<List<ProteinLogEntity>> =
        nutritionDao.getProteinLogsForDate(date)

    suspend fun logProteinFood(date: String, foodName: String, portionDesc: String, proteinGrams: Int) {
        nutritionDao.insertProteinLog(
            ProteinLogEntity(date = date, foodName = foodName, portionDesc = portionDesc, proteinGrams = proteinGrams)
        )
        addXp(10)
    }

    suspend fun deleteProteinLog(log: ProteinLogEntity) = nutritionDao.deleteProteinLog(log)

    // Water
    fun getWaterLogForDate(date: String): Flow<WaterLogEntity?> = waterDao.getWaterLogForDate(date)

    suspend fun addWater(date: String, amountDeltaMl: Int) {
        val current = waterDao.getWaterLogForDate(date).firstOrNull()?.amountMl ?: 0
        val newAmount = (current + amountDeltaMl).coerceAtLeast(0)
        waterDao.insertOrUpdateWaterLog(WaterLogEntity(date = date, amountMl = newAmount))
        if (newAmount >= 2500 && current < 2500) {
            addXp(15) // Water target bonus
        }
    }

    suspend fun setWater(date: String, amountMl: Int) {
        waterDao.insertOrUpdateWaterLog(WaterLogEntity(date = date, amountMl = amountMl.coerceAtLeast(0)))
    }

    // Sleep
    fun getSleepLogForDate(date: String): Flow<SleepLogEntity?> = sleepDao.getSleepLogForDate(date)
    fun getRecentSleepLogs(): Flow<List<SleepLogEntity>> = sleepDao.getRecentSleepLogs()

    suspend fun logSleep(date: String, bedtime: String, wakeTime: String, durationMinutes: Int, quality: Int) {
        sleepDao.insertOrUpdateSleepLog(
            SleepLogEntity(date = date, bedtime = bedtime, wakeTime = wakeTime, durationMinutes = durationMinutes, qualityRating = quality)
        )
        if (durationMinutes >= 420) { // 7h
            addXp(20)
        }
    }

    // Supplements
    fun getAllSupplements(): Flow<List<SupplementEntity>> = supplementDao.getAllSupplements()
    fun getSupplementLogsForDate(date: String): Flow<List<SupplementLogEntity>> = supplementDao.getSupplementLogsForDate(date)

    suspend fun toggleSupplementTaken(supplement: SupplementEntity, date: String, isTaken: Boolean) {
        if (isTaken) {
            supplementDao.deleteSupplementLog(supplement.id, date)
        } else {
            val now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            supplementDao.insertSupplementLog(
                SupplementLogEntity(supplementId = supplement.id, supplementName = supplement.name, date = date, takenTime = now)
            )
            addXp(5)
        }
    }

    suspend fun addSupplement(supplement: SupplementEntity) = supplementDao.insertSupplement(supplement)
    suspend fun updateSupplement(supplement: SupplementEntity) = supplementDao.updateSupplement(supplement)
    suspend fun deleteSupplement(supplement: SupplementEntity) = supplementDao.deleteSupplement(supplement)

    // Workout & Cardio
    fun getWorkoutSessionForDate(date: String): Flow<WorkoutSessionEntity?> = workoutDao.getWorkoutSessionForDate(date)
    fun getExercisesForSession(sessionId: Long): Flow<List<ExerciseLogEntity>> = workoutDao.getExercisesForSession(sessionId)

    suspend fun saveWorkoutSession(session: WorkoutSessionEntity): Long {
        val id = workoutDao.insertOrUpdateWorkoutSession(session)
        if (session.isCompleted) {
            addXp(30)
        }
        return id
    }

    suspend fun addExercise(exercise: ExerciseLogEntity) = workoutDao.insertExercise(exercise)
    suspend fun updateExercise(exercise: ExerciseLogEntity) = workoutDao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: ExerciseLogEntity) = workoutDao.deleteExercise(exercise)

    fun getCardioLogsForDate(date: String): Flow<List<CardioLogEntity>> = workoutDao.getCardioLogsForDate(date)
    fun getRecentCardioLogs(): Flow<List<CardioLogEntity>> = workoutDao.getRecentCardioLogs()

    suspend fun logCardio(cardio: CardioLogEntity) {
        workoutDao.insertCardioLog(cardio)
        addXp(10)
    }

    suspend fun deleteCardio(cardio: CardioLogEntity) = workoutDao.deleteCardioLog(cardio)

    // Body Measurements
    fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>> = bodyMeasurementDao.getAllMeasurements()
    fun getMeasurementForDate(date: String): Flow<BodyMeasurementEntity?> = bodyMeasurementDao.getMeasurementForDate(date)

    suspend fun saveMeasurement(measurement: BodyMeasurementEntity) {
        bodyMeasurementDao.insertMeasurement(measurement)
        // Update user profile current weight
        val currentProfile = userProfileDao.getUserProfileDirect()
        if (currentProfile != null) {
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

    suspend fun addMealPrepItem(item: MealPrepItemEntity) {
        mealPrepDao.insertMealPrepItem(item)
        addXp(15)
    }

    suspend fun consumeMealPrepPortion(item: MealPrepItemEntity) {
        if (item.portionsAvailable > 1) {
            mealPrepDao.updateMealPrepItem(item.copy(portionsAvailable = item.portionsAvailable - 1))
        } else {
            mealPrepDao.deleteMealPrepItem(item)
        }
        addXp(10)
    }

    suspend fun deleteMealPrepItem(item: MealPrepItemEntity) = mealPrepDao.deleteMealPrepItem(item)

    // Recovery & Falling Off
    fun getRecoveryForDate(date: String): Flow<RecoveryLogEntity?> = recoveryDao.getRecoveryForDate(date)

    suspend fun applyRecoveryPlan(date: String, obstacle: String, planText: String) {
        recoveryDao.insertOrUpdateRecovery(
            RecoveryLogEntity(date = date, obstacleReason = obstacle, planSelected = planText, isCompleted = true)
        )
        val profile = userProfileDao.getUserProfileDirect() ?: return
        userProfileDao.updateStreakStats(
            streak = profile.currentStreak,
            longest = profile.longestStreak,
            perfect = profile.perfectDays,
            recovery = profile.recoveryDays + 1,
            recoveryActive = true
        )
        addXp(25)
    }

    suspend fun restoreMomentum() {
        val profile = userProfileDao.getUserProfileDirect() ?: return
        userProfileDao.updateStreakStats(
            streak = (profile.currentStreak + 1).coerceAtLeast(1),
            longest = maxOf(profile.longestStreak, profile.currentStreak + 1),
            perfect = profile.perfectDays,
            recovery = profile.recoveryDays,
            recoveryActive = false
        )
        addXp(20)
    }

    // Activity Sessions (Gym, Walking, Running, Yoga, Dance, Sports, etc.)
    val allActivitySessions: Flow<List<ActivitySessionEntity>> = activitySessionDao.getAllActivitySessions()
    fun getActivitySessionsForDate(date: String): Flow<List<ActivitySessionEntity>> = activitySessionDao.getActivitySessionsForDate(date)

    suspend fun logActivitySession(session: ActivitySessionEntity): Long {
        val id = activitySessionDao.insertActivitySession(session)
        addXp(25)
        return id
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

    suspend fun toggleEnvironmentTask(item: EnvironmentTaskEntity) {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        val newState = !item.isCompleted
        environmentTaskDao.setTaskCompleted(item.id, newState, if (newState) now else "")
        if (newState) {
            addXp(10)
        }
    }

    suspend fun addEnvironmentTask(task: EnvironmentTaskEntity) {
        environmentTaskDao.insertTask(task)
    }

    suspend fun deleteEnvironmentTask(task: EnvironmentTaskEntity) {
        environmentTaskDao.deleteTask(task)
    }

    suspend fun ensureEnvironmentTasksForDate(date: String) {
        val existing = environmentTaskDao.getTasksForDateDirect(date)
        if (existing.isNotEmpty()) return

        val localDate = try { LocalDate.parse(date) } catch (e: Exception) { LocalDate.now() }
        val dayOfWeek = localDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() }

        val tasks = mutableListOf<EnvironmentTaskEntity>()
        var order = 0

        // Daily Routines
        tasks.add(EnvironmentTaskEntity(date = date, category = "MORNING", title = "Make bed & open windows for natural light", orderIndex = order++))
        tasks.add(EnvironmentTaskEntity(date = date, category = "MORNING", title = "Drink 500ml water before coffee/tea", orderIndex = order++))
        tasks.add(EnvironmentTaskEntity(date = date, category = "MORNING", title = "Write down today's top 3 non-negotiables", orderIndex = order++))

        tasks.add(EnvironmentTaskEntity(date = date, category = "LEAVING_HOME", title = "Kitchen sink clear of dishes", orderIndex = order++))
        tasks.add(EnvironmentTaskEntity(date = date, category = "LEAVING_HOME", title = "Essential check: keys, wallet, phone, water bottle", orderIndex = order++))

        tasks.add(EnvironmentTaskEntity(date = date, category = "AFTER_WORK", title = "10-minute rapid room & desk tidy", orderIndex = order++))
        tasks.add(EnvironmentTaskEntity(date = date, category = "AFTER_WORK", title = "Hang work clothes & change into movement attire", orderIndex = order++))

        tasks.add(EnvironmentTaskEntity(date = date, category = "BEFORE_SLEEP", title = "Kitchen counter wiped & trash checked", orderIndex = order++))
        tasks.add(EnvironmentTaskEntity(date = date, category = "BEFORE_SLEEP", title = "Set out tomorrow's workout/work clothes", orderIndex = order++))
        tasks.add(EnvironmentTaskEntity(date = date, category = "BEFORE_SLEEP", title = "Phone on charger away from bed", orderIndex = order++))

        // Weekly Focus Schedule based on current day
        when (dayOfWeek) {
            "Monday" -> tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Monday Focus: Weekly budget & financial check-in", dayOfWeek = dayOfWeek, orderIndex = order++))
            "Tuesday" -> tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Tuesday Focus: Desk declutter & filing documents", dayOfWeek = dayOfWeek, orderIndex = order++))
            "Wednesday" -> tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Wednesday Focus: Bathroom deep clean & mid-week grocery restock", dayOfWeek = dayOfWeek, orderIndex = order++))
            "Thursday" -> tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Thursday Focus: Dedicated learning / reading block", dayOfWeek = dayOfWeek, orderIndex = order++))
            "Friday" -> tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Friday Focus: Weekly review & weekend discipline plan", dayOfWeek = dayOfWeek, orderIndex = order++))
            "Saturday" -> {
                tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Saturday Focus: Room deep vacuum & laundry / fresh bedsheets", dayOfWeek = dayOfWeek, orderIndex = order++))
                tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Saturday Focus: Personal grooming & barber/nail care", dayOfWeek = dayOfWeek, orderIndex = order++))
            }
            "Sunday" -> {
                tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Sunday Focus: Meal prep foundation (boil eggs, soak pulses, prep sabzi)", dayOfWeek = dayOfWeek, orderIndex = order++))
                tasks.add(EnvironmentTaskEntity(date = date, category = "WEEKLY", title = "Sunday Focus: Digital declutter (delete photos, clear inbox, backup data)", dayOfWeek = dayOfWeek, orderIndex = order++))
            }
        }

        environmentTaskDao.insertAll(tasks)
    }

    // Daily Companion Check-ins (Morning, Midday, Evening)
    fun getDailyCheckin(date: String, timeOfDay: String): Flow<DailyCheckinEntity?> =
        dailyCheckinDao.getCheckin(date, timeOfDay)

    fun getAllCheckinsForDate(date: String): Flow<List<DailyCheckinEntity>> =
        dailyCheckinDao.getAllCheckinsForDate(date)

    suspend fun saveDailyCheckin(checkin: DailyCheckinEntity) {
        dailyCheckinDao.insertOrUpdateCheckin(checkin)
        addXp(20)
    }

    // Contextual Reflection Engine (80-120 Stoic Items with repetition prevention)
    suspend fun getReflectionForContext(timeOfDay: String, mood: String? = null): ReflectionItem {
        val recent = reflectionHistoryDao.getRecentReflectionIds(20)
        return ReflectionLibrary.getReflection(timeOfDay, mood, recent)
    }

    suspend fun getDailyReflection(timeOfDay: String, mood: String? = null): ReflectionItem =
        getReflectionForContext(timeOfDay, mood)

    suspend fun recordReflectionShown(reflectionId: String, date: String, timeOfDay: String) {
        reflectionHistoryDao.recordReflectionShown(
            ReflectionHistoryEntity(
                reflectionId = reflectionId,
                date = date,
                timeOfDay = timeOfDay
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
        dailyChecklistDao.clearAll()
        val emptyProfile = UserProfileEntity(
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
        userProfileDao.insertOrUpdateProfile(emptyProfile)
    }
}
