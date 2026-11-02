package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.SummaryTracking
import com.example.data.local.entity.DaySummaryEntity
import com.example.util.DeviceClock
import java.time.Clock
import java.time.LocalDate

class DaySummaryRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    suspend fun sync(backfillLimit: Int = 32) = database.withTransaction {
        val sql = database.openHelper.writableDatabase
        SummaryTracking.install(sql)
        val today = LocalDate.now(clock).toString()
        val dates = (database.daySummaryDao().dirty(64) + database.daySummaryDao().missing(backfillLimit) + today).distinct()
        for (date in dates) {
            val records = DailyProgressRepository(database).records(date)
            if (records.targets == null) { database.daySummaryDao().clearDirty(date); continue }
            val targets = records.targets
            val grams = records.nutrition.protein.sumOf { it.proteinGrams.toLong() } + records.nutrition.meals.filter { it.isCompleted }.sumOf { it.proteinGrams.toLong() }
            var earned = 0
            var early = false
            var checkins = 0
            var steps = 0
            var meters = 0.0
            var mood = ""
            sql.query("SELECT COALESCE(SUM(xp), 0) FROM reward_ledger WHERE date = ?", arrayOf(date)).use { if (it.moveToFirst()) earned = it.getInt(0) }
            sql.query("SELECT mood FROM daily_checkins WHERE date = ? AND isCompleted = 1 ORDER BY id DESC", arrayOf(date)).use { while (it.moveToNext()) { checkins++; if (mood.isBlank()) mood = it.getString(0) } }
            sql.query("SELECT EXISTS(SELECT 1 FROM reward_ledger WHERE eventKey = ? AND CAST(STRFTIME('%H', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) < 8)", arrayOf("checkin:$date:MORNING")).use { if (it.moveToFirst()) early = it.getInt(0) == 1 }
            sql.query("SELECT COALESCE(SUM(steps), 0), COALESCE(SUM(distanceMeters), 0) FROM tracking_day_totals WHERE date = ?", arrayOf(date)).use { if (it.moveToFirst()) { steps = it.getInt(0); meters = it.getDouble(1) } }
            database.daySummaryDao().save(DaySummaryEntity(date, DailyProgressRules.goal(records).met,
                records.tasks.count { it.isCompleted } + records.routineCompletions, records.tasks.size + records.routinesDue,
                DailyProgressRules.activityMinutes(records.movement), earned, clock.millis(),
                targets.targetsKnown && targets.proteinTargetGrams > 0 && grams >= targets.proteinTargetGrams,
                targets.targetsKnown && targets.waterTargetMl > 0 && (records.rest.water?.amountMl ?: 0) >= targets.waterTargetMl,
                targets.targetsKnown && targets.sleepTargetMinutes > 0 && (records.rest.sleep?.durationMinutes ?: 0) >= targets.sleepTargetMinutes,
                records.movement.workout?.isCompleted == true, records.routineCompletions, records.routinesDue,
                checkins, early, steps, meters, records.rest.sleep?.durationMinutes ?: 0, mood))
            database.daySummaryDao().clearDirty(date)
        }
    }
}