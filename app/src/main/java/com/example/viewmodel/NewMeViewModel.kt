package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NewMeDatabase
import com.example.data.local.content.ReflectionItem
import com.example.data.local.entity.*
import com.example.data.companion.*
import com.example.data.repository.DisciplineScoreBreakdown
import com.example.data.repository.LevelInfo
import com.example.data.repository.NewMeRepository
import com.example.util.VoiceCompanion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class NewMeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NewMeDatabase.getInstance(application)
    val repository = NewMeRepository(database)
    val voiceCompanion = VoiceCompanion(application)

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val levelInfo: StateFlow<LevelInfo> = userProfile.map { profile ->
        repository.calculateLevelInfo(profile?.currentXp ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LevelInfo(1, 0, 0, 100, 0f))

    private val _dailyReflection = MutableStateFlow<ReflectionItem?>(null)
    val dailyReflection: StateFlow<ReflectionItem?> = _dailyReflection.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val environmentTasks: StateFlow<List<EnvironmentTaskEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getEnvironmentTasksForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailyCheckins: StateFlow<List<DailyCheckinEntity>> = _selectedDate.flatMapLatest { date: String ->
        repository.getAllCheckinsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendConnections: StateFlow<List<FriendConnectionEntity>> = repository.getAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val checklist: StateFlow<List<DailyChecklistItemEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getChecklistForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val meals: StateFlow<List<MealPlanEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getMealsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val proteinLogs: StateFlow<List<ProteinLogEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getProteinLogsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalProteinLogged: StateFlow<Int> = proteinLogs.map { logs ->
        logs.sumOf { it.proteinGrams }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val waterLog: StateFlow<WaterLogEntity?> = _selectedDate.flatMapLatest { date ->
        repository.getWaterLogForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sleepLog: StateFlow<SleepLogEntity?> = _selectedDate.flatMapLatest { date ->
        repository.getSleepLogForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentSleepLogs: StateFlow<List<SleepLogEntity>> = repository.getRecentSleepLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupplements: StateFlow<List<SupplementEntity>> = repository.getAllSupplements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val supplementLogs: StateFlow<List<SupplementLogEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getSupplementLogsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val workoutSession: StateFlow<WorkoutSessionEntity?> = _selectedDate.flatMapLatest { date ->
        repository.getWorkoutSessionForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val workoutExercises: StateFlow<List<ExerciseLogEntity>> = workoutSession.flatMapLatest { session ->
        if (session != null) {
            repository.getExercisesForSession(session.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val cardioLogs: StateFlow<List<CardioLogEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getCardioLogsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activitySessions: StateFlow<List<ActivitySessionEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getActivitySessionsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivitySessions: StateFlow<List<ActivitySessionEntity>> = repository.allActivitySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthConnectImports: StateFlow<List<HealthConnectImportEntity>> = repository.getRecentHealthConnectImports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyMeasurements: StateFlow<List<BodyMeasurementEntity>> = repository.getAllMeasurements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealPrepItems: StateFlow<List<MealPrepItemEntity>> = repository.getAllMealPrepItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val recoveryLog: StateFlow<RecoveryLogEntity?> = _selectedDate.flatMapLatest { date ->
        repository.getRecoveryForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dynamic Discipline Score Breakdown (Strict 0-baseline, no fake starter points)
    val disciplineScore: StateFlow<DisciplineScoreBreakdown> = combine(
        meals,
        totalProteinLogged,
        workoutSession,
        cardioLogs,
        activitySessions,
        waterLog,
        sleepLog,
        allSupplements,
        supplementLogs,
        mealPrepItems,
        checklist,
        userProfile
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val mealList = args[0] as List<MealPlanEntity>
        val proteinGrams = args[1] as Int
        val workout = args[2] as WorkoutSessionEntity?
        @Suppress("UNCHECKED_CAST")
        val cardioList = args[3] as List<CardioLogEntity>
        @Suppress("UNCHECKED_CAST")
        val activities = args[4] as List<ActivitySessionEntity>
        val water = args[5] as WaterLogEntity?
        val sleep = args[6] as SleepLogEntity?
        @Suppress("UNCHECKED_CAST")
        val suppList = args[7] as List<SupplementEntity>
        @Suppress("UNCHECKED_CAST")
        val suppLogs = args[8] as List<SupplementLogEntity>
        @Suppress("UNCHECKED_CAST")
        val prepList = args[9] as List<MealPrepItemEntity>
        @Suppress("UNCHECKED_CAST")
        val checkList = args[10] as List<DailyChecklistItemEntity>
        val profile = args[11] as UserProfileEntity?

        val targetProtein = profile?.proteinTargetGrams ?: 90
        val targetWaterMl = ((profile?.waterTargetLiters ?: 2.5f) * 1000).toInt()
        val targetSleepMinutes = ((profile?.sleepTargetHours ?: 7.5f) * 60).toInt()

        // 1. Nutrition: 25 pts
        val completedMeals = mealList.count { it.isCompleted }
        val nutritionScore = if (mealList.isNotEmpty()) {
            ((completedMeals.toFloat() / mealList.size) * 25).toInt()
        } else 0

        // 2. Protein: 15 pts
        val proteinRatio = (proteinGrams.toFloat() / targetProtein).coerceIn(0f, 1f)
        val proteinScore = (proteinRatio * 15).toInt()

        // 3. Movement / Activity / Workout: 20 pts
        // Counts structured gym workouts, cardio sessions, or dynamic activity sessions (walking, running, yoga, dance, sports)
        val hasCompletedGym = workout?.isCompleted == true
        val hasCompletedActivity = activities.any { it.isCompleted }
        val hasCardio = cardioList.isNotEmpty() || workout?.cardioDone == true

        val movementBase = when {
            hasCompletedGym || hasCompletedActivity -> 15
            workout != null -> 8
            activities.isNotEmpty() -> 8
            else -> 0
        }
        val movementBonus = if (hasCardio || activities.size > 1) 5 else 0
        val workoutScore = (movementBase + movementBonus).coerceAtMost(20)

        // 4. Water: 10 pts
        val currentWater = water?.amountMl ?: 0
        val waterScore = if (targetWaterMl > 0) {
            ((currentWater.toFloat() / targetWaterMl).coerceIn(0f, 1f) * 10).toInt()
        } else 0

        // 5. Sleep: 15 pts
        val currentSleepMin = sleep?.durationMinutes ?: 0
        val sleepScore = if (targetSleepMinutes > 0 && sleep != null) {
            ((currentSleepMin.toFloat() / targetSleepMinutes).coerceIn(0f, 1f) * 15).toInt()
        } else 0

        // 6. Supplements: 5 pts (if user doesn't take supplements, they are not penalized)
        val supplementScore = if (profile?.usesSupplements == false) {
            5
        } else {
            val activeSupps = suppList.count { it.isActive }
            if (activeSupps > 0) {
                ((suppLogs.size.toFloat() / activeSupps).coerceIn(0f, 1f) * 5).toInt()
            } else 0
        }

        // 7. Meal Prep: 5 pts
        val mealPrepScore = if (prepList.isNotEmpty()) 5 else 0

        // 8. Habits / checklist: 5 pts
        val checklistDone = checkList.count { it.isCompleted }
        val habitScore = if (checkList.isNotEmpty()) {
            ((checklistDone.toFloat() / checkList.size).coerceIn(0f, 1f) * 5).toInt()
        } else 0

        val total = (nutritionScore + proteinScore + workoutScore + waterScore + sleepScore + supplementScore + mealPrepScore + habitScore).coerceIn(0, 100)

        DisciplineScoreBreakdown(
            totalScore = total,
            nutritionScore = nutritionScore,
            nutritionMax = 25,
            proteinScore = proteinScore,
            proteinMax = 15,
            workoutScore = workoutScore,
            workoutMax = 20,
            waterScore = waterScore,
            waterMax = 10,
            sleepScore = sleepScore,
            sleepMax = 15,
            supplementScore = supplementScore,
            supplementMax = 5,
            mealPrepScore = mealPrepScore,
            mealPrepMax = 5,
            habitScore = habitScore,
            habitMax = 5
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DisciplineScoreBreakdown()
    )

    private val _currentMood = MutableStateFlow<String>("Okay")
    val currentMood: StateFlow<String> = _currentMood.asStateFlow()

    val companionMessage: StateFlow<CompanionMessage> = combine(
        userProfile,
        disciplineScore,
        totalProteinLogged,
        waterLog,
        workoutSession,
        sleepLog,
        _currentMood
    ) { args: Array<Any?> ->
        val profile = args[0] as UserProfileEntity?
        val score = args[1] as DisciplineScoreBreakdown
        val protein = args[2] as Int
        val water = args[3] as WaterLogEntity?
        val workout = args[4] as WorkoutSessionEntity?
        val sleep = args[5] as SleepLogEntity?
        val moodStr = args[6] as String

        val mood = CompanionEngine.parseMoodState(moodStr)
        val waterMl = water?.amountMl ?: 0
        val isWorkoutDone = workout?.isCompleted == true
        val hasSleep = sleep != null
        CompanionEngine.generateCompanionMessage(
            profile = profile,
            currentHour = java.time.LocalTime.now().hour,
            currentMood = mood,
            progressPercent = score.totalScore,
            proteinLoggedGrams = protein,
            waterLoggedMl = waterMl,
            hasCompletedWorkout = isWorkoutDone,
            hasLoggedSleep = hasSleep,
            isRecoveryMode = profile?.isRecoveryActive == true
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CompanionMessage(
            greeting = "Good day",
            message = "You don't control everything that happens today. You do control what you do next.",
            tone = "Balanced"
        )
    )

    val isVoiceSpeaking: StateFlow<Boolean> = voiceCompanion.isSpeaking

    fun setMood(mood: String) {
        _currentMood.value = mood
        val timeContext = CompanionEngine.determineTimeOfDayContext().name
        viewModelScope.launch {
            repository.saveDailyCheckin(
                DailyCheckinEntity(
                    date = _selectedDate.value,
                    timeOfDay = timeContext,
                    mood = mood,
                    practiceVirtue = "Mood set to $mood",
                    isCompleted = true
                )
            )
        }
    }

    fun markFirstMorningWelcomeSeen() {
        viewModelScope.launch {
            repository.setFirstMorningWelcomeSeen(true)
        }
    }

    fun speakCompanionMessage(text: String) {
        voiceCompanion.speak(text)
    }

    fun stopVoice() {
        voiceCompanion.stop()
    }

    fun applyMinimumViableDay(obstacle: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val plan = "1. Hydrate with 500ml water\n2. Hit protein target\n3. 15 min light movement\n4. One clean meal\n5. Protect sleep"
            repository.applyRecoveryPlan(date, obstacle, plan)
            repository.addWater(date, 500)
            repository.addXp(30)
        }
    }

    val dayDisplayString: StateFlow<String> = combine(_selectedDate, userProfile) { date, profile ->
        try {
            val start = LocalDate.parse(profile?.startDate ?: "2026-09-01")
            val current = LocalDate.parse(date)
            val diff = ChronoUnit.DAYS.between(start, current) + 1
            val dayNum = diff.coerceAtLeast(1)
            "DAY %03d".format(dayNum)
        } catch (e: Exception) {
            "DAY 001"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DAY 001")

    init {
        viewModelScope.launch {
            repository.ensureDateInitialized(_selectedDate.value)
            loadTodayReflection("MORNING", "Good")
        }
    }

    // Actions
    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            repository.ensureDateInitialized(date)
        }
    }

    fun toggleChecklistItem(item: DailyChecklistItemEntity) {
        viewModelScope.launch {
            repository.toggleChecklistItem(item)
        }
    }

    fun addChecklistItem(section: String, title: String, xp: Int = 15) {
        viewModelScope.launch {
            repository.addChecklistItem(
                DailyChecklistItemEntity(
                    date = _selectedDate.value,
                    section = section,
                    title = title,
                    isCompleted = false,
                    xpReward = xp
                )
            )
        }
    }

    fun deleteChecklistItem(item: DailyChecklistItemEntity) {
        viewModelScope.launch {
            repository.deleteChecklistItem(item)
        }
    }

    // Nutrition & Protein
    fun toggleMealCompleted(meal: MealPlanEntity) {
        viewModelScope.launch {
            repository.toggleMealCompleted(meal)
        }
    }

    fun addMeal(meal: MealPlanEntity) {
        viewModelScope.launch {
            repository.addMeal(meal.copy(date = _selectedDate.value))
        }
    }

    fun updateMeal(meal: MealPlanEntity) {
        viewModelScope.launch {
            repository.updateMeal(meal)
        }
    }

    fun deleteMeal(meal: MealPlanEntity) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }

    fun duplicateMeal(meal: MealPlanEntity) {
        viewModelScope.launch {
            repository.addMeal(meal.copy(id = 0, name = "${meal.name} (Copy)"))
        }
    }

    fun applyIndianFoundationMeals() {
        val isVeg = userProfile.value?.dietaryPreference?.contains("Non", ignoreCase = true) != true
        val date = _selectedDate.value
        viewModelScope.launch {
            repository.addMeal(
                MealPlanEntity(
                    date = date,
                    slot = "Morning",
                    name = if (isVeg) "Besan Paneer Chilla" else "Egg Bhurji & Multigrain Roti",
                    time = "08:30 AM",
                    foodsDescription = if (isVeg) "2 Besan chillas with 60g paneer filling + mint coriander chutney" else "3 whole eggs bhurji with onions and tomatoes + 1 multigrain roti",
                    approxCalories = 360,
                    proteinGrams = 28,
                    carbsGrams = 32,
                    fatsGrams = 12,
                    isCompleted = false,
                    source = "Recommended Plan"
                )
            )
            repository.addMeal(
                MealPlanEntity(
                    date = date,
                    slot = "Lunch",
                    name = "Foundation Dal Bowl & Sabzi",
                    time = "01:30 PM",
                    foodsDescription = "1 generous bowl yellow dal / rajma + 200g seasonal vegetable sabzi + 100g fresh curd",
                    approxCalories = 450,
                    proteinGrams = 26,
                    carbsGrams = 55,
                    fatsGrams = 14,
                    isCompleted = false,
                    source = "Recommended Plan"
                )
            )
            repository.addMeal(
                MealPlanEntity(
                    date = date,
                    slot = "Post-workout",
                    name = "Protein Booster Anchor",
                    time = "05:30 PM",
                    foodsDescription = "1 scoop Whey Protein in water or roasted chana / sattu drink",
                    approxCalories = 180,
                    proteinGrams = 24,
                    carbsGrams = 10,
                    fatsGrams = 3,
                    isCompleted = false,
                    source = "Recommended Plan"
                )
            )
            repository.addMeal(
                MealPlanEntity(
                    date = date,
                    slot = "Dinner",
                    name = if (isVeg) "Paneer / Soya Tikka & Green Salad" else "Grilled Chicken Breast & Steamed Veg",
                    time = "08:30 PM",
                    foodsDescription = if (isVeg) "150g grilled low-fat paneer / spiced soya chunks + cucumber tomato salad" else "160g tandoori / pan-seared chicken breast + tossed green salad",
                    approxCalories = 390,
                    proteinGrams = 32,
                    carbsGrams = 18,
                    fatsGrams = 14,
                    isCompleted = false,
                    source = "Recommended Plan"
                )
            )
        }
    }

    fun quickAddProtein(foodName: String, portion: String, grams: Int) {
        viewModelScope.launch {
            repository.logProteinFood(_selectedDate.value, foodName, portion, grams)
        }
    }

    fun deleteProteinLog(log: ProteinLogEntity) {
        viewModelScope.launch {
            repository.deleteProteinLog(log)
        }
    }

    // Water
    fun addWater(amountDeltaMl: Int) {
        viewModelScope.launch {
            repository.addWater(_selectedDate.value, amountDeltaMl)
        }
    }

    fun setWater(amountMl: Int) {
        viewModelScope.launch {
            repository.setWater(_selectedDate.value, amountMl)
        }
    }

    // Sleep
    fun logSleep(bedtime: String, wakeTime: String, durationMinutes: Int, quality: Int) {
        viewModelScope.launch {
            repository.logSleep(_selectedDate.value, bedtime, wakeTime, durationMinutes, quality)
        }
    }

    // Supplements
    fun toggleSupplementTaken(supplement: SupplementEntity, isTaken: Boolean) {
        viewModelScope.launch {
            repository.toggleSupplementTaken(supplement, _selectedDate.value, isTaken)
        }
    }

    fun addSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            repository.addSupplement(supplement)
        }
    }

    fun updateSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            repository.updateSupplement(supplement)
        }
    }

    fun deleteSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            repository.deleteSupplement(supplement)
        }
    }

    // Workout & Cardio
    fun saveWorkoutSession(title: String, durationMinutes: Int, warmUp: Boolean, strength: Boolean, cardio: Boolean, coolDown: Boolean, isCompleted: Boolean, notes: String) {
        viewModelScope.launch {
            val existing = workoutSession.value
            val session = WorkoutSessionEntity(
                id = existing?.id ?: 0,
                date = _selectedDate.value,
                title = title,
                durationMinutes = durationMinutes,
                warmUpDone = warmUp,
                strengthDone = strength,
                cardioDone = cardio,
                coolDownDone = coolDown,
                isCompleted = isCompleted,
                notes = notes
            )
            repository.saveWorkoutSession(session)
        }
    }

    fun addExercise(sessionId: Long, name: String, category: String, setsSummary: String, prevPerformance: String) {
        viewModelScope.launch {
            repository.addExercise(
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseName = name,
                    category = category,
                    setsSummary = setsSummary,
                    previousPerformance = prevPerformance
                )
            )
        }
    }

    fun deleteExercise(exercise: ExerciseLogEntity) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun logCardio(type: String, durationMinutes: Int, distanceKm: Float, intensity: String, calories: Int) {
        viewModelScope.launch {
            repository.logCardio(
                CardioLogEntity(
                    date = _selectedDate.value,
                    cardioType = type,
                    durationMinutes = durationMinutes,
                    distanceKm = distanceKm,
                    intensity = intensity,
                    calories = calories
                )
            )
        }
    }

    fun deleteCardio(cardio: CardioLogEntity) {
        viewModelScope.launch {
            repository.deleteCardio(cardio)
        }
    }

    // Body Measurements
    fun saveMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch {
            repository.saveMeasurement(measurement)
        }
    }

    fun deleteMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurement)
        }
    }

    // Meal Prep
    fun addMealPrepItem(name: String, portions: Int, expiry: String, notes: String) {
        viewModelScope.launch {
            repository.addMealPrepItem(
                MealPrepItemEntity(
                    foodName = name,
                    portionsAvailable = portions,
                    prepDate = _selectedDate.value,
                    expiryDate = expiry,
                    notes = notes
                )
            )
        }
    }

    fun consumeMealPrepPortion(item: MealPrepItemEntity) {
        viewModelScope.launch {
            repository.consumeMealPrepPortion(item)
        }
    }

    fun deleteMealPrepItem(item: MealPrepItemEntity) {
        viewModelScope.launch {
            repository.deleteMealPrepItem(item)
        }
    }

    // Recovery & Falling Off
    fun applyFallingOffPlan(obstacle: String, planText: String) {
        viewModelScope.launch {
            repository.applyRecoveryPlan(_selectedDate.value, obstacle, planText)
        }
    }

    fun restoreMomentum() {
        viewModelScope.launch {
            repository.restoreMomentum()
        }
    }

    // Profile & Settings
    fun updateProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun completeOnboarding(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.completeOnboarding(profile)
        }
    }

    fun restartOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding()
        }
    }

    // Dynamic Activity Logging
    fun logActivitySession(
        activityType: String,
        title: String,
        durationMinutes: Int,
        intensity: String,
        distanceKm: Float? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val session = ActivitySessionEntity(
                date = _selectedDate.value,
                activityType = activityType,
                title = title,
                durationMinutes = durationMinutes,
                intensity = intensity,
                distanceKm = distanceKm ?: 0f,
                notes = notes,
                isCompleted = true,
                source = "Manual"
            )
            repository.logActivitySession(session)
        }
    }

    fun deleteActivitySession(session: ActivitySessionEntity) {
        viewModelScope.launch {
            repository.deleteActivitySession(session)
        }
    }

    fun importHealthConnectData(recordId: String, dataType: String, value: String, date: String) {
        viewModelScope.launch {
            repository.importHealthConnectRecord(recordId, dataType, value, date)
        }
    }

    // Daily Reflections & Voice Companion
    fun loadTodayReflection(timeOfDay: String = "MORNING", mood: String = "Good") {
        viewModelScope.launch {
            val reflection = repository.getDailyReflection(timeOfDay, mood)
            _dailyReflection.value = reflection
            val profile = userProfile.value
            if (profile?.voiceEnabled == true && reflection != null) {
                val shouldSpeak = when (timeOfDay.uppercase()) {
                    "MORNING" -> profile.speakMorning
                    "MIDDAY" -> profile.speakMidday
                    "EVENING" -> profile.speakEvening
                    else -> false
                }
                if (shouldSpeak) {
                    voiceCompanion.speak(reflection.text)
                }
            }
        }
    }

    fun speakText(text: String) {
        voiceCompanion.speak(text)
    }

    fun stopSpeaking() {
        voiceCompanion.stop()
    }

    // Environment & Living Space
    fun toggleEnvironmentTask(task: EnvironmentTaskEntity) {
        viewModelScope.launch {
            repository.toggleEnvironmentTask(task)
        }
    }

    fun addEnvironmentTask(title: String, category: String = "MORNING") {
        viewModelScope.launch {
            repository.addEnvironmentTask(
                EnvironmentTaskEntity(
                    date = _selectedDate.value,
                    title = title,
                    category = category,
                    isCompleted = false
                )
            )
        }
    }

    fun deleteEnvironmentTask(task: EnvironmentTaskEntity) {
        viewModelScope.launch {
            repository.deleteEnvironmentTask(task)
        }
    }

    // Daily Check-ins (Morning Anchor, Midday Reset, Evening Reflection)
    fun recordDailyCheckin(
        timeOfDay: String,
        mood: String = "Focused",
        practiceVirtue: String = "",
        mattersMost: String = "",
        refuseToLeaveUnfinished: String = "",
        middayStatus: String = "",
        completedPercent: Int = 0,
        reflectionId: String = "",
        reflectionText: String = ""
    ) {
        viewModelScope.launch {
            val checkin = DailyCheckinEntity(
                date = _selectedDate.value,
                timeOfDay = timeOfDay,
                mood = mood,
                practiceVirtue = practiceVirtue,
                mattersMost = mattersMost,
                refuseToLeaveUnfinished = refuseToLeaveUnfinished,
                middayStatus = middayStatus,
                completedPercent = completedPercent,
                reflectionId = reflectionId,
                reflectionText = reflectionText,
                isCompleted = true
            )
            repository.saveDailyCheckin(checkin)
        }
    }

    // Friend Connections (Local Accountability)
    fun addFriend(code: String, name: String) {
        viewModelScope.launch {
            repository.addFriend(code, name)
        }
    }

    fun updateFriendStatus(id: Long, momentum: Int, lastActive: String, status: String) {
        viewModelScope.launch {
            repository.updateFriendStatus(id, momentum, lastActive, status)
        }
    }

    fun removeFriend(id: Long) {
        viewModelScope.launch {
            repository.deleteFriend(id)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceCompanion.shutdown()
    }
}
