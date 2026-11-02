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
        DaySummaryRepository(database, clock).sync()
        val today = LocalDate.now(clock)
        val streak = database.daySummaryDao().streak(today.toString())
        val totals = database.daySummaryDao().totals(today.toString())
        if (database.settingsDao().get()?.goalChosen == true && !database.daySummaryDao().hasMissing()) {
            database.userProfileDao().updateStreakStats(streak.current, maxOf(profile.longestStreak, streak.longest),
                maxOf(profile.perfectDays, streak.successfulDays), profile.recoveryDays, profile.isRecoveryActive)
        }
        val values = badgeValues(today.toString())
        BadgeCatalog.all.filter { values.current(it) >= it.target }.forEach { database.achievementDao().unlockOnce(it.id, today.toString()) }
    }

    suspend fun achievementProgress(): List<AchievementProgress> = database.withTransaction {
        val today = LocalDate.now(clock).toString()
        val values = badgeValues(today)
        database.achievementDao().getAll().map { achievement ->
            val badge = BadgeCatalog.all.firstOrNull { it.id == achievement.id }
            val target = badge?.target ?: 1
            AchievementProgress(achievement, if (achievement.isUnlocked) target else badge?.let(values::current) ?: 0, target)
        }
    }

    private suspend fun badgeValues(today: String): BadgeValues {
        val summaries = database.daySummaryDao()
        val totals = summaries.totals(today)
        val streak = summaries.streak(today)
        val profile = database.userProfileDao().getUserProfileDirect()
        val sql = database.openHelper.readableDatabase
        var routes = 0
        var activityTypes = 0
        sql.query("SELECT COUNT(*) FROM (SELECT activity_tracks.sessionKey FROM activity_tracks JOIN route_points ON activity_tracks.sessionKey = route_points.sessionKey WHERE activity_tracks.status = 'FINISHED' GROUP BY activity_tracks.sessionKey HAVING COUNT(route_points.id) >= 2)").use { if (it.moveToFirst()) routes = it.getInt(0) }
        sql.query("SELECT COUNT(*) FROM (SELECT activityType FROM activity_sessions WHERE isCompleted = 1 UNION SELECT activityType FROM activity_tracks WHERE status = 'FINISHED')").use { if (it.moveToFirst()) activityTypes = it.getInt(0) }
        return BadgeValues(mapOf(BadgeMetric.ACTIONS to database.rewardDao().count(), BadgeMetric.GOALS to totals.goalDays,
            BadgeMetric.STREAK to maxOf(streak.longest, profile?.longestStreak ?: 0), BadgeMetric.COMEBACKS to summaries.comebacks(today, "-1 day"),
            BadgeMetric.PROTEIN to totals.proteinDays, BadgeMetric.WATER to totals.waterDays, BadgeMetric.PREP to database.rewardDao().countMatching("mealprep:%"),
            BadgeMetric.SLEEP to totals.sleepDays, BadgeMetric.ACTIVITIES to (database.activitySessionDao().getTotalCompletedActivities().firstValue() + database.workoutDao().getTotalCompletedWorkouts().firstValue()),
            BadgeMetric.ROUTES to routes, BadgeMetric.ACTIVITY_TYPES to activityTypes, BadgeMetric.STEPS to totals.maxSteps, BadgeMetric.DISTANCE to totals.meters.coerceAtMost(Int.MAX_VALUE.toDouble()).toInt(),
            BadgeMetric.GYM_WEEKS to summaries.gymWeeks(today), BadgeMetric.ROUTINE_DAYS to totals.routineDays, BadgeMetric.CHECKINS to totals.checkins,
            BadgeMetric.PERFECT_QUESTS to totals.perfectQuests, BadgeMetric.EARLY_DAYS to totals.earlyDays, BadgeMetric.LEVEL to (profile?.currentLevel ?: 1), BadgeMetric.FRESH_STARTS to summaries.comebacks(today, "-3 day")))
    }

    private suspend fun kotlinx.coroutines.flow.Flow<Int>.firstValue(): Int = first()
}