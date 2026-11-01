package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.RewardEventEntity
import com.example.util.DeviceClock
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

object Levels {
    private val thresholds = listOf(0, 500, 1200, 2000, 3000, 4500, 6500, 9000, 12000, 16000)
    fun info(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)
        val extraLevels = if (xp >= thresholds.last()) (xp - thresholds.last()) / 4000 else 0
        val level = if (xp >= thresholds.last()) thresholds.size + extraLevels else thresholds.count { xp >= it }
        val base = if (level >= thresholds.size) thresholds.last() + extraLevels * 4000 else thresholds[level - 1]
        val next = if (level < thresholds.size) thresholds[level] else (base.toLong() + 4000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val progress = ((xp - base).toFloat() / (next - base).coerceAtLeast(1)).coerceIn(0f, 1f)
        return LevelInfo(level, xp, base, next, progress)
    }
}

class GamificationRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    suspend fun reward(key: String, date: String, xp: Int, legacyKey: String? = null): Boolean = database.withTransaction {
        require(xp > 0 && key.isNotBlank()) { "A reward must identify a recorded action." }
        require(!LocalDate.parse(date).isAfter(LocalDate.now(clock))) { "Future actions cannot earn XP." }
        val profile = database.userProfileDao().getUserProfileDirect()?.takeIf { it.isOnboarded } ?: return@withTransaction false
        val rewards = database.rewardDao()
        if (rewards.isExcluded(key)) return@withTransaction false
        val cutoff = database.settingsDao().get()?.legacyRewardCutoffDate.orEmpty()
        if (legacyKey != null && cutoff.isNotBlank() && date <= cutoff && rewards.isExcluded(legacyKey)) return@withTransaction false
        if (rewards.insert(RewardEventEntity(key, date, xp, clock.millis())) == -1L) return@withTransaction false
        val total = (profile.currentXp.toLong() + xp).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        database.userProfileDao().updateXpAndLevel(total, Levels.info(total).level)
        true
    }

    suspend fun refresh() = database.withTransaction {
        val profile = database.userProfileDao().getUserProfileDirect()?.takeIf { it.isOnboarded } ?: return@withTransaction
        NewMeDatabase.seedInitialData(database)
        val progress = DailyProgressRepository(database)
        val days = database.dailyStateDao().getAll()
        val qualifying = mutableSetOf<LocalDate>()
        var proteinDays = 0
        for (day in days) {
            val records = progress.records(day.date)
            if (DailyProgressRules.goal(records).met) qualifying.add(LocalDate.parse(day.date))
            val grams = records.nutrition.protein.sumOf { it.proteinGrams } + records.nutrition.meals.filter { it.isCompleted }.sumOf { it.proteinGrams }
            if (day.targetsKnown && day.proteinTargetGrams > 0 && grams >= day.proteinTargetGrams) proteinDays++
        }
        val today = LocalDate.now(clock)
        val streak = DailyProgressRules.streak(today, qualifying)
        if (database.settingsDao().get()?.goalChosen == true) {
            database.userProfileDao().updateStreakStats(streak.current, maxOf(profile.longestStreak, streak.longest),
                maxOf(profile.perfectDays, streak.successfulDays), profile.recoveryDays, profile.isRecoveryActive)
        }
        val count = database.rewardDao().count()
        val activities = database.activitySessionDao().getTotalCompletedActivities().firstValue() +
            database.workoutDao().getTotalCompletedWorkouts().firstValue()
        val prep = database.rewardDao().countMatching("mealprep:%")
        val recovered = qualifying.any { date -> date.isBefore(today.plusDays(1)) &&
            date.minusDays(1) !in qualifying && qualifying.any { it.isBefore(date.minusDays(1)) } }
        val rules = mapOf("first_step" to (count >= 1), "first_day" to (streak.successfulDays >= 1),
            "seven_day_momentum" to (streak.longest >= 7), "thirty_day_momentum" to (streak.longest >= 30),
            "protein_consistency" to (proteinDays >= 7), "activity_builder" to (activities >= 10),
            "meal_prep_builder" to (prep >= 3), "recovery_builder" to recovered)
        rules.filterValues { it }.keys.forEach { database.achievementDao().unlockOnce(it, today.toString()) }
    }

    suspend fun achievementProgress(): List<AchievementProgress> = database.withTransaction {
        val progress = DailyProgressRepository(database)
        val days = database.dailyStateDao().getAll()
        val qualifying = mutableSetOf<LocalDate>()
        var proteinDays = 0
        for (day in days) {
            val records = progress.records(day.date)
            if (DailyProgressRules.goal(records).met) qualifying.add(LocalDate.parse(day.date))
            val grams = records.nutrition.protein.sumOf { it.proteinGrams } + records.nutrition.meals.filter { it.isCompleted }.sumOf { it.proteinGrams }
            if (day.targetsKnown && day.proteinTargetGrams > 0 && grams >= day.proteinTargetGrams) proteinDays++
        }
        val streak = DailyProgressRules.streak(LocalDate.now(clock), qualifying)
        val values = mapOf("first_step" to (database.rewardDao().count() to 1), "first_day" to (streak.successfulDays to 1),
            "seven_day_momentum" to (streak.longest to 7), "thirty_day_momentum" to (streak.longest to 30),
            "protein_consistency" to (proteinDays to 7), "activity_builder" to ((database.activitySessionDao().getTotalCompletedActivities().firstValue() +
                database.workoutDao().getTotalCompletedWorkouts().firstValue()) to 10),
            "meal_prep_builder" to (database.rewardDao().countMatching("mealprep:%") to 3), "recovery_builder" to (0 to 1))
        database.achievementDao().getAll().map { achievement ->
            val (current, target) = values[achievement.id] ?: (0 to 1)
            AchievementProgress(achievement, if (achievement.isUnlocked) target else current.coerceAtMost(target), target)
        }
    }

    private suspend fun kotlinx.coroutines.flow.Flow<Int>.firstValue(): Int = first()
}