package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.*
import com.example.data.model.ActivityKind
import com.example.data.model.DailyGoalKind
import com.example.data.routine.RoutineSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class DayNutrition(val meals: List<MealPlanEntity>, val protein: List<ProteinLogEntity>)
data class DayMovement(val workout: WorkoutSessionEntity?, val activities: List<ActivitySessionEntity>, val cardio: List<CardioLogEntity>, val trackedMillis: Long = 0)
data class DayRest(val water: WaterLogEntity?, val sleep: SleepLogEntity?)
data class DayRecords(val targets: DailyStateEntity?, val nutrition: DayNutrition, val movement: DayMovement,
    val rest: DayRest, val tasks: List<DailyChecklistItemEntity>, val routineCompletions: Int = 0, val routinesDue: Int = 0)
data class DailyGoalProgress(val kind: DailyGoalKind = DailyGoalKind.TASKS, val current: Int = 0, val target: Int = 0, val available: Boolean = false, val questsAvailable: Int? = null) {
    val met: Boolean get() = available && target > 0 && current >= target
    val exceedsQuests: Boolean get() = available && kind == DailyGoalKind.TASKS && questsAvailable != null && target > questsAvailable
}
data class StreakSummary(val current: Int, val longest: Int, val successfulDays: Int)
data class AchievementProgress(val achievement: AchievementEntity, val current: Int, val target: Int)

object DailyProgressRules {
    fun activityMinutes(movement: DayMovement): Int {
        val gym = movement.workout?.takeIf { it.isCompleted }?.durationMinutes ?: 0
        val activities = movement.activities.filter { it.isCompleted && it.source != "Phone timer" &&
            !(gym > 0 && ActivityKind.fromStored(it.activityType) == ActivityKind.GYM) }
            .sumOf { it.durationMinutes.coerceAtLeast(0).toLong() }
        val cardio = if (movement.workout?.isCompleted == true && movement.workout.cardioDone) 0L
            else movement.cardio.sumOf { it.durationMinutes.coerceAtLeast(0).toLong() }
        return (gym + activities + cardio + movement.trackedMillis / 60_000).coerceAtMost(1440L).toInt()
    }

    fun goal(records: DayRecords): DailyGoalProgress {
        val day = records.targets ?: return DailyGoalProgress()
        val kind = DailyGoalKind.entries.firstOrNull { it.name == day.goalType } ?: return DailyGoalProgress()
        val current = when (kind) {
            DailyGoalKind.TASKS -> records.tasks.count { it.isCompleted } + records.routineCompletions
            DailyGoalKind.ACTIVITY_MINUTES -> activityMinutes(records.movement)
        }
        return DailyGoalProgress(kind, current, day.goalTarget, day.goalEnabled, records.tasks.size + records.routinesDue)
    }

    fun score(records: DayRecords): DisciplineScoreBreakdown {
        val day = records.targets
        val known = day?.targetsKnown == true
        val nutritionMax = if (records.nutrition.meals.isNotEmpty()) 25 else 0
        val proteinMax = if (known && day!!.proteinTargetGrams > 0) 15 else 0
        val movementMax = if (day?.activityEnabled == true || activityMinutes(records.movement) > 0) 20 else 0
        val waterMax = if (known && day!!.waterTargetMl > 0) 10 else 0
        val sleepMax = if (known && day!!.sleepTargetMinutes > 0) 15 else 0
        val habitMax = if (records.tasks.isNotEmpty() || records.routinesDue > 0) 5 else 0
        val meals = records.nutrition.meals
        val nutrition = fraction(meals.count { it.isCompleted }, meals.size, nutritionMax)
        val protein = records.nutrition.protein.sumOf { it.proteinGrams.coerceAtLeast(0).toLong() } +
            meals.filter { it.isCompleted }.sumOf { it.proteinGrams.coerceAtLeast(0).toLong() }
        val proteinScore = fraction(protein.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), day?.proteinTargetGrams ?: 0, proteinMax)
        val movement = fraction(activityMinutes(records.movement),
            if (day?.goalType == DailyGoalKind.ACTIVITY_MINUTES.name && day.goalEnabled) day.goalTarget else 20, movementMax)
        val water = fraction(records.rest.water?.amountMl ?: 0, day?.waterTargetMl ?: 0, waterMax)
        val sleep = fraction(records.rest.sleep?.durationMinutes ?: 0, day?.sleepTargetMinutes ?: 0, sleepMax)
        val habit = fraction(records.tasks.count { it.isCompleted } + records.routineCompletions, records.tasks.size + records.routinesDue, habitMax)
        val max = nutritionMax + proteinMax + movementMax + waterMax + sleepMax + habitMax
        val total = if (max == 0) 0 else ((nutrition + proteinScore + movement + water + sleep + habit) * 100f / max).toInt().coerceIn(0, 100)
        return DisciplineScoreBreakdown(total, nutrition, nutritionMax, proteinScore, proteinMax, movement, movementMax,
            water, waterMax, sleep, sleepMax, 0, 0, 0, 0, habit, habitMax, known)
    }

    fun streak(today: LocalDate, qualifyingDates: Set<LocalDate>): StreakSummary {
        var cursor = if (today in qualifyingDates) today else today.minusDays(1)
        var current = 0
        while (cursor in qualifyingDates) { current++; cursor = cursor.minusDays(1) }
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        val dates = qualifyingDates.filter { !it.isAfter(today) }.sorted()
        for (date in dates) {
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
        }
        return StreakSummary(current, longest, dates.size)
    }

    private fun fraction(value: Int, target: Int, max: Int): Int =
        if (max <= 0 || target <= 0) 0 else ((value.toFloat() / target).coerceIn(0f, 1f) * max).toInt()
}

class DailyProgressRepository(private val database: NewMeDatabase) {
    fun observeDay(date: String): Flow<DayRecords> {
        val food = combine(database.nutritionDao().getMealsForDate(date), database.nutritionDao().getProteinLogsForDate(date), ::DayNutrition)
        val movement = combine(database.workoutDao().getWorkoutSessionForDate(date), database.activitySessionDao().getActivitySessionsForDate(date),
            database.workoutDao().getCardioLogsForDate(date), database.activityTrackingDao().observeMillis(date), ::DayMovement)
        val rest = combine(database.waterDao().getWaterLogForDate(date), database.sleepDao().getSleepLogForDate(date), ::DayRest)
        val quests = combine(database.dailyChecklistDao().getChecklistForDate(date), database.routineDao().observe(),
            database.routineDao().observeCompletions(date)) { tasks, templates, completions ->
            Triple(tasks, completions.size, templates.count { RoutineSchedule.isDue(it, LocalDate.parse(date)) })
        }
        return combine(database.dailyStateDao().observe(date), food, movement, rest, quests) { targets, nutrition, activity, recovery, items ->
            DayRecords(targets, nutrition, activity, recovery, items.first, items.second, items.third)
        }
    }

    suspend fun records(date: String): DayRecords = database.withTransaction {
        DayRecords(database.dailyStateDao().get(date), DayNutrition(database.nutritionDao().getMealsForDateDirect(date), database.nutritionDao().getProteinLogs(date)),
            DayMovement(database.workoutDao().getSession(date), database.activitySessionDao().getActivitySessionsForDateDirect(date), database.workoutDao().getCardioLogs(date), database.activityTrackingDao().getMillis(date)),
            DayRest(database.waterDao().getLog(date), database.sleepDao().getLog(date)), database.dailyChecklistDao().getChecklistForDateDirect(date),
            database.routineDao().completions(date).size, database.routineDao().getAll().count { RoutineSchedule.isDue(it, LocalDate.parse(date)) })
    }
}