package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NewMeDatabase
import com.example.data.backup.BackupStore
import com.example.data.local.content.ReflectionItem
import com.example.data.local.entity.*
import com.example.data.model.OnboardingPreferences
import com.example.data.model.DailyGoalKind
import com.example.data.model.ThemeMode
import com.example.data.companion.*
import com.example.data.cycle.CycleRepository
import com.example.data.cycle.CycleRules
import com.example.data.cycle.CyclePrediction
import com.example.data.activity.ActivityTrackingRepository
import com.example.data.activity.ActivityTrackingService
import com.example.data.model.ActivityKind
import com.example.data.repository.DisciplineScoreBreakdown
import com.example.data.repository.LevelInfo
import com.example.data.repository.NewMeRepository
import com.example.data.repository.DailyProgressRepository
import com.example.data.repository.DailyProgressRules
import com.example.data.repository.DailyGoalProgress
import com.example.data.repository.GamificationRepository
import com.example.data.repository.AchievementProgress
import com.example.data.repository.SettingsRepository
import com.example.util.VoiceCompanion
import com.example.util.UiWriteQueue
import com.example.util.DeviceClock
import com.example.util.DateSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class NewMeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NewMeDatabase.getInstance(application)
    val repository = NewMeRepository(database)
    val voiceCompanion = VoiceCompanion(application)
    private val clock = DeviceClock()
    private val now = MutableStateFlow(clock.instant())
    private val dateSelection = DateSelection(LocalDate.now(clock))
    private val progressRepository = DailyProgressRepository(database)
    private val gamification = GamificationRepository(database, clock)
    private val settingsRepository = SettingsRepository(database)
    private val cycleRepository = CycleRepository(database, clock)
    private val trackingRepository = ActivityTrackingRepository(database, clock)
    val activeTracking = trackingRepository.active.catch { error ->
        if (error is CancellationException) throw error
        _operationError.value = "Activity state could not be opened. Saved data was kept."
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val trackedHistory = trackingRepository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trackingBusy = ActivityTrackingService.commandPending
    private val _savingActivityLog = MutableStateFlow(false)
    val savingActivityLog = _savingActivityLog.asStateFlow()
    val cycleEntries = cycleRepository.entries.catch { error ->
        if (error is CancellationException) throw error
        _operationError.value = "Cycle history could not be opened. Existing data was kept."
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cyclePrediction = combine(cycleEntries, now) { entries, instant ->
        CycleRules.predict(entries, LocalDateTime.ofInstant(instant, clock.zone).toLocalDate())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CyclePrediction.Insufficient)
    private val _savingCycle = MutableStateFlow(false)
    val savingCycle = _savingCycle.asStateFlow()
    private val _achievementProgress = MutableStateFlow<List<AchievementProgress>>(emptyList())
    val achievementProgress = _achievementProgress.asStateFlow()
    private val backupStore = BackupStore(database)
    private val _operationError = MutableStateFlow<String?>(null)
    val operationError = _operationError.asStateFlow()
    private val writes = UiWriteQueue(viewModelScope, { _operationError.value = it }, {
        gamification.refresh()
        _achievementProgress.value = gamification.achievementProgress()
    })
    private val _loadState = MutableStateFlow<AppLoadState>(AppLoadState.Loading)
    val loadState = _loadState.asStateFlow()
    private val reload = MutableStateFlow(0)
    private val _savingOnboarding = MutableStateFlow(false)
    val savingOnboarding = _savingOnboarding.asStateFlow()
    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState = _backupState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userProfile: StateFlow<UserProfileEntity?> = reload.flatMapLatest {
        repository.userProfile.onEach { _loadState.value = AppLoadState.Ready }.catch { error ->
            if (error is CancellationException) throw error
            _loadState.value = AppLoadState.Error("Local storage could not be opened. Nothing was deleted. Keep the app installed; a compatible update may be needed. Do not clear app data.")
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val appSettings: StateFlow<AppSettingsEntity?> = database.settingsDao().observe().catch { error ->
        if (error is CancellationException) throw error
        _loadState.value = AppLoadState.Error("Local settings could not be opened. Nothing was deleted; keep the app installed.")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val levelInfo: StateFlow<LevelInfo> = userProfile.map { profile ->
        repository.calculateLevelInfo(profile?.currentXp ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LevelInfo(1, 0, 0, 100, 0f))

    private val _dailyReflection = MutableStateFlow<ReflectionItem?>(null)
    val dailyReflection: StateFlow<ReflectionItem?> = _dailyReflection.asStateFlow()
    private var reflectionContext: String? = null
    private val _savingCheckin = MutableStateFlow(false)
    val savingCheckin = _savingCheckin.asStateFlow()
    val offlineVoiceAvailable = voiceCompanion.offlineAvailable

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val environmentTasks: StateFlow<List<EnvironmentTaskEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getEnvironmentTasksForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailyCheckins: StateFlow<List<DailyCheckinEntity>> = _selectedDate.flatMapLatest { date: String ->
        repository.getAllCheckinsForDate(date).map { checkins -> checkins.distinctBy { it.timeOfDay } }
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

    val totalProteinLogged: StateFlow<Int> = combine(proteinLogs, meals) { logs, plannedMeals ->
        logs.sumOf { it.proteinGrams } + plannedMeals.filter { it.isCompleted }.sumOf { it.proteinGrams }
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val dayRecords = _selectedDate.flatMapLatest(progressRepository::observeDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val disciplineScore: StateFlow<DisciplineScoreBreakdown> = dayRecords.map { records ->
        records?.let(DailyProgressRules::score) ?: DisciplineScoreBreakdown(hasTargetHistory = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisciplineScoreBreakdown(hasTargetHistory = false))

    val dailyGoal: StateFlow<DailyGoalProgress> = dayRecords.map { records ->
        records?.let(DailyProgressRules::goal) ?: DailyGoalProgress()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyGoalProgress())
    val dailyTargets = dayRecords.map { it?.targets }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentMood = MutableStateFlow<String>("Okay")
    val currentMood: StateFlow<String> = _currentMood.asStateFlow()

    private val companionContext = combine(userProfile, dayRecords, disciplineScore, dailyGoal) { profile, records, score, goal ->
        CompanionContext(profile, score, records?.nutrition?.protein?.sumOf { it.proteinGrams } ?: 0,
            records?.rest?.water?.amountMl ?: 0, records?.movement?.workout?.isCompleted == true, records?.rest?.sleep != null, goal)
    }
    val companionMessage: StateFlow<CompanionMessage> = combine(companionContext, _currentMood, now, _selectedDate, dailyTargets) { context, moodStr, instant, date, targets ->
        val profile = context.profile
        CompanionEngine.generateCompanionMessage(
            profile = profile,
            currentHour = LocalDateTime.ofInstant(instant, clock.zone).hour,
            currentMood = CompanionEngine.parseMoodState(moodStr), progressPercent = context.score.totalScore,
            proteinLoggedGrams = context.protein, waterLoggedMl = context.water,
            hasCompletedWorkout = context.workoutDone, hasLoggedSleep = context.sleepLogged,
            isRecoveryMode = profile?.isRecoveryActive == true,
            rotationSeed = LocalDate.parse(date).toEpochDay(), dailyGoal = context.goal,
            targetProteinGrams = targets?.takeIf { it.targetsKnown }?.proteinTargetGrams ?: 0,
            targetWaterMl = targets?.takeIf { it.targetsKnown }?.waterTargetMl ?: 0,
            isHistorical = date != LocalDate.now(clock).toString()
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
    }

    fun markFirstMorningWelcomeSeen() {
        writes.launch {
            repository.setFirstMorningWelcomeSeen(true)
        }
    }

    fun speakCompanionMessage(text: String) {
        speakText(text)
    }

    fun stopVoice() {
        voiceCompanion.stop()
    }

    fun applyMinimumViableDay(obstacle: String) {
        writes.launch {
            val date = _selectedDate.value
            val plan = "1. Hydrate with 500ml water\n2. Hit protein target\n3. 15 min light movement\n4. One clean meal\n5. Protect sleep"
            repository.applyRecoveryPlan(date, obstacle, plan)
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
            ActivityTrackingService.errors.collect { error ->
                if (error != null) { _operationError.value = error; ActivityTrackingService.clearError() }
            }
        }
        writes.launch {
            if (!ActivityTrackingService.isRunning) trackingRepository.recoverInterrupted()
            repository.ensureDateInitialized(_selectedDate.value)
            loadTodayReflection("MORNING", "Good")
        }
        viewModelScope.launch {
            userProfile.filterNotNull().distinctUntilChangedBy { it.isOnboarded }.collect { profile ->
                if (profile.isOnboarded) writes.launch { repository.ensureDateInitialized(_selectedDate.value) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                onAppResumed()
                delay(30_000)
            }
        }
    }

    // Actions
    fun selectDate(date: String) {
        dateSelection.select(LocalDate.parse(date))
        _selectedDate.value = dateSelection.selected.toString()
        writes.launch {
            repository.ensureDateInitialized(date)
        }
    }

    fun onAppResumed() {
        now.value = clock.instant()
        val previousToday = dateSelection.today
        val newDate = dateSelection.refresh(LocalDate.now(clock)).toString()
        if (newDate != _selectedDate.value) {
            _selectedDate.value = newDate
            writes.launch { repository.ensureDateInitialized(newDate) }
        }
        if (previousToday != dateSelection.today && newDate == _selectedDate.value) writes.launch { gamification.refresh() }
        val key = "${_selectedDate.value}:$reflectionPeriod"
        if (userProfile.value?.isOnboarded == true && reflectionContext != key) loadTodayReflection(reflectionPeriod, _currentMood.value)
    }

    val reflectionPeriod: String get() = when (LocalDateTime.ofInstant(clock.instant(), clock.zone).hour) {
        in 5..11 -> "MORNING"
        in 12..16 -> "MIDDAY"
        else -> "EVENING"
    }

    fun toggleChecklistItem(item: DailyChecklistItemEntity) {
        writes.launch {
            repository.toggleChecklistItem(item)
        }
    }

    fun addChecklistItem(section: String, title: String, xp: Int = 15) {
        writes.launch {
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
        writes.launch {
            repository.deleteChecklistItem(item)
        }
    }

    // Nutrition & Protein
    fun toggleMealCompleted(meal: MealPlanEntity) {
        writes.launch {
            repository.toggleMealCompleted(meal)
        }
    }

    fun addMeal(meal: MealPlanEntity) {
        writes.launch {
            repository.addMeal(meal.copy(date = _selectedDate.value))
        }
    }

    fun updateMeal(meal: MealPlanEntity) {
        writes.launch {
            repository.updateMeal(meal)
        }
    }

    fun deleteMeal(meal: MealPlanEntity) {
        writes.launch {
            repository.deleteMeal(meal)
        }
    }

    fun duplicateMeal(meal: MealPlanEntity) {
        writes.launch {
            repository.addMeal(meal.copy(id = 0, name = "${meal.name} (Copy)"))
        }
    }

    fun applyIndianFoundationMeals() {
        val isVeg = userProfile.value?.dietaryPreference?.contains("Non", ignoreCase = true) != true
        val date = _selectedDate.value
        writes.launch {
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
        writes.launch {
            repository.logProteinFood(_selectedDate.value, foodName, portion, grams)
        }
    }

    fun deleteProteinLog(log: ProteinLogEntity) {
        writes.launch {
            repository.deleteProteinLog(log)
        }
    }

    // Water
    fun addWater(amountDeltaMl: Int) {
        writes.launch {
            repository.addWater(_selectedDate.value, amountDeltaMl)
        }
    }

    fun setWater(amountMl: Int) {
        writes.launch {
            repository.setWater(_selectedDate.value, amountMl)
        }
    }

    // Sleep
    fun logSleep(bedtime: String, wakeTime: String, durationMinutes: Int, quality: Int) {
        writes.launch {
            repository.logSleep(_selectedDate.value, bedtime, wakeTime, durationMinutes, quality)
        }
    }

    // Supplements
    fun toggleSupplementTaken(supplement: SupplementEntity, isTaken: Boolean) {
        writes.launch {
            repository.toggleSupplementTaken(supplement, _selectedDate.value, isTaken)
        }
    }

    fun addSupplement(supplement: SupplementEntity) {
        writes.launch {
            repository.addSupplement(supplement)
        }
    }

    fun updateSupplement(supplement: SupplementEntity) {
        writes.launch {
            repository.updateSupplement(supplement)
        }
    }

    fun deleteSupplement(supplement: SupplementEntity) {
        writes.launch {
            repository.deleteSupplement(supplement)
        }
    }

    // Workout & Cardio
    fun saveWorkoutSession(title: String, durationMinutes: Int, warmUp: Boolean, strength: Boolean, cardio: Boolean, coolDown: Boolean, isCompleted: Boolean, notes: String) {
        writes.launch {
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
        writes.launch {
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
        writes.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun logCardio(type: String, durationMinutes: Int, distanceKm: Float, intensity: String, calories: Int) {
        writes.launch {
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
        writes.launch {
            repository.deleteCardio(cardio)
        }
    }

    // Body Measurements
    fun saveMeasurement(measurement: BodyMeasurementEntity, onSaved: () -> Unit = {}) {
        writes.launch {
            repository.saveMeasurement(measurement)
            onSaved()
        }
    }

    fun deleteMeasurement(measurement: BodyMeasurementEntity) {
        writes.launch {
            repository.deleteMeasurement(measurement)
        }
    }

    // Meal Prep
    fun addMealPrepItem(name: String, portions: Int, expiry: String, notes: String) {
        writes.launch {
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
        writes.launch {
            repository.consumeMealPrepPortion(item)
        }
    }

    fun deleteMealPrepItem(item: MealPrepItemEntity) {
        writes.launch {
            repository.deleteMealPrepItem(item)
        }
    }

    // Recovery & Falling Off
    fun applyFallingOffPlan(obstacle: String, planText: String) {
        writes.launch {
            repository.applyRecoveryPlan(_selectedDate.value, obstacle, planText)
        }
    }

    fun restoreMomentum() {
        writes.launch {
            repository.restoreMomentum()
        }
    }

    // Profile & Settings
    fun updateProfile(profile: UserProfileEntity) {
        writes.launch {
            repository.saveProfile(profile)
        }
    }

    fun setDailyGoal(kind: DailyGoalKind, target: Int) {
        writes.launch { repository.setDailyGoal(kind, target) }
    }

    fun setAppearance(theme: ThemeMode, reducedMotion: Boolean) {
        writes.launch { settingsRepository.appearance(theme, reducedMotion) }
    }

    fun setCycleEnabled(enabled: Boolean) { writes.launch { cycleRepository.enabled(enabled) } }

    fun saveCycle(entry: CycleEntryEntity, onSaved: () -> Unit) {
        if (_savingCycle.value || writes.isMaintaining) return
        _savingCycle.value = true
        writes.launch {
            try { cycleRepository.save(entry); onSaved() }
            finally { _savingCycle.value = false }
        }
    }

    fun deleteCycle(entry: CycleEntryEntity) { writes.launch { cycleRepository.delete(entry) } }

    fun startActivity(kind: ActivityKind, gps: Boolean) = trackingCommand(ActivityTrackingService.START, kind, gps)
    fun resumeActivity() {
        val track = activeTracking.value ?: return
        trackingCommand(ActivityTrackingService.RESUME, ActivityKind.fromStored(track.activityType), track.gpsEnabled)
    }
    fun pauseActivity() = trackingCommand(ActivityTrackingService.PAUSE)
    fun stopActivity() = trackingCommand(ActivityTrackingService.STOP)
    fun discardActivity() = trackingCommand(ActivityTrackingService.DISCARD)

    private fun trackingCommand(action: String, kind: ActivityKind = ActivityKind.OTHER, gps: Boolean = false) {
        if (writes.isMaintaining) { _operationError.value = "Wait for the local data operation to finish."; return }
        try { ActivityTrackingService.command(getApplication(), action, kind, gps) }
        catch (failure: Exception) { _operationError.value = "Android could not start tracking. Open NewMe and retry, or use a manual activity log." }
    }

    fun route(key: String) = database.activityTrackingDao().observeRoute(key)
    fun deleteRoute(key: String) { writes.launch { trackingRepository.deleteRoute(key) } }
    fun deleteTrackedActivity(key: String) { writes.launch { trackingRepository.deleteFinished(key) } }

    fun completeOnboarding(profile: UserProfileEntity, preferences: OnboardingPreferences? = null, onSaved: () -> Unit = {}) {
        if (_savingOnboarding.value || writes.isMaintaining) return
        _savingOnboarding.value = true
        writes.launch {
            try {
                repository.completeOnboarding(profile, preferences)
                onSaved()
            } finally {
                _savingOnboarding.value = false
            }
        }
    }

    fun restartOnboarding() {
        writes.launch {
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
        notes: String = "",
        steps: Int = 0,
        onSaved: () -> Unit = {}
    ) {
        if (_savingActivityLog.value || writes.isMaintaining) return
        _savingActivityLog.value = true
        val date = _selectedDate.value
        writes.launch {
            try {
            val session = ActivitySessionEntity(
                date = date,
                activityType = activityType,
                title = title,
                durationMinutes = durationMinutes,
                intensity = intensity,
                distanceKm = distanceKm ?: 0f,
                steps = steps,
                notes = notes,
                isCompleted = true,
                source = "Manual"
            )
            repository.logActivitySession(session)
            onSaved()
            } finally { _savingActivityLog.value = false }
        }
    }

    fun deleteActivitySession(session: ActivitySessionEntity) {
        writes.launch {
            repository.deleteActivitySession(session)
        }
    }

    fun importHealthConnectData(recordId: String, dataType: String, value: String, date: String) {
        writes.launch {
            repository.importHealthConnectRecord(recordId, dataType, value, date)
        }
    }

    // Daily Reflections & Voice Companion
    fun loadTodayReflection(timeOfDay: String = "MORNING", mood: String = "Good") {
        val date = _selectedDate.value
        writes.launch {
            if (userProfile.value?.isOnboarded != true) return@launch
            val reflection = repository.getDailyReflection(timeOfDay, mood)
            _dailyReflection.value = reflection
            reflectionContext = "$date:$timeOfDay"
        }
    }

    fun reflectionWasShown(reflectionId: String) {
        val date = _selectedDate.value
        val period = reflectionPeriod
        writes.launch { repository.recordReflectionShown(reflectionId, date, period) }
    }

    fun speakText(text: String) {
        if (!voiceCompanion.speak(text)) _operationError.value = "Offline voice is unavailable. Install an offline English voice in Android settings; NewMe will not use a network voice."
    }

    fun stopSpeaking() {
        voiceCompanion.stop()
    }

    // Environment & Living Space
    fun toggleEnvironmentTask(task: EnvironmentTaskEntity) {
        writes.launch {
            repository.toggleEnvironmentTask(task)
        }
    }

    fun addEnvironmentTask(title: String, category: String = "MORNING") {
        writes.launch {
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
        writes.launch {
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
        reflectionText: String = "",
        onSaved: () -> Unit = {}
    ) {
        if (_savingCheckin.value || writes.isMaintaining) return
        _savingCheckin.value = true
        val date = _selectedDate.value
        writes.launch {
            try {
            val checkin = DailyCheckinEntity(
                date = date,
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
            onSaved()
            } finally { _savingCheckin.value = false }
        }
    }

    fun saveWelcome(mood: String, intention: String, onSaved: () -> Unit) {
        if (_savingCheckin.value || writes.isMaintaining) return
        _savingCheckin.value = true
        writes.launch {
            try {
                repository.saveWelcomeIntention(mood, intention)
                _currentMood.value = mood
                onSaved()
            } finally { _savingCheckin.value = false }
        }
    }

    // Friend Connections (Local Accountability)
    fun addFriend(code: String, name: String) {
        writes.launch {
            repository.addFriend(code, name)
        }
    }

    fun updateFriendStatus(id: Long, momentum: Int, lastActive: String, status: String) {
        writes.launch {
            repository.updateFriendStatus(id, momentum, lastActive, status)
        }
    }

    fun removeFriend(id: Long) {
        writes.launch {
            repository.deleteFriend(id)
        }
    }

    fun resetAllData(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                writes.withMaintenance {
                    ActivityTrackingService.stopForMaintenance()
                    repository.resetAllData()
                    voiceCompanion.stop()
                    _dailyReflection.value = null
                    _currentMood.value = "Okay"
                    _selectedDate.value = LocalDate.now().toString()
                    userProfile.first { it == null }
                    onSaved()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _operationError.value = "Reset failed. Please retry; do not clear app data or uninstall."
            }
        }
    }

    fun clearOperationError() { _operationError.value = null }

    fun retryLoading() {
        _loadState.value = AppLoadState.Loading
        reload.value += 1
    }

    fun dismissBackup() {
        if (_backupState.value != BackupUiState.Working) _backupState.value = BackupUiState.Idle
    }

    fun exportBackup(uri: Uri, password: CharArray) {
        runBackup(password) {
            ActivityTrackingService.flushPending()
            backupStore.exportTo(getApplication<Application>().contentResolver, uri, password)
            _backupState.value = BackupUiState.Success("Encrypted backup saved to local storage.")
        }
    }

    fun prepareRestore(uri: Uri, password: CharArray) {
        runBackup(password) {
            val prepared = backupStore.prepareFrom(getApplication<Application>().contentResolver, uri, password)
            _backupState.value = BackupUiState.ReadyToRestore(prepared)
        }
    }

    fun confirmRestore() {
        val prepared = (_backupState.value as? BackupUiState.ReadyToRestore)?.backup ?: return
        runBackup(null) {
            ActivityTrackingService.stopForMaintenance()
            trackingRepository.recoverInterrupted()
            backupStore.restore(prepared)
            voiceCompanion.stop()
            _selectedDate.value = LocalDate.now().toString()
            _backupState.value = BackupUiState.Success("Backup restored. Private reminders remain off until enabled again.")
        }
    }

    private fun runBackup(password: CharArray?, block: suspend () -> Unit) {
        if (_backupState.value == BackupUiState.Working) {
            password?.fill('\u0000')
            return
        }
        _backupState.value = BackupUiState.Working
        viewModelScope.launch {
            try {
                writes.withMaintenance { block() }
            } catch (error: CancellationException) {
                _backupState.value = BackupUiState.Idle
                throw error
            } catch (error: Exception) {
                val message = if (error is IllegalArgumentException || error is java.io.IOException) {
                    error.message ?: "Backup failed. Existing data has not changed."
                } else "Backup failed. Existing data has not changed."
                _backupState.value = BackupUiState.Error(message)
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceCompanion.shutdown()
    }
}
