package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_templates ORDER BY sortOrder, id") fun observe(): Flow<List<RoutineTemplateEntity>>
    @Query("SELECT * FROM routine_templates ORDER BY sortOrder, id") suspend fun getAll(): List<RoutineTemplateEntity>
    @Query("SELECT * FROM routine_templates WHERE id = :id") suspend fun get(id: Long): RoutineTemplateEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(template: RoutineTemplateEntity): Long
    @Update suspend fun update(template: RoutineTemplateEntity)
    @Query("DELETE FROM routine_templates WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM routine_completions WHERE date = :date") fun observeCompletions(date: String): Flow<List<RoutineCompletionEntity>>
    @Query("SELECT * FROM routine_completions WHERE date = :date") suspend fun completions(date: String): List<RoutineCompletionEntity>
    @Query("SELECT * FROM routine_completions WHERE templateId = :id AND date = :date") suspend fun completion(id: Long, date: String): RoutineCompletionEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun complete(completion: RoutineCompletionEntity): Long
    @Query("DELETE FROM routine_completions WHERE templateId = :id AND date = :date") suspend fun undo(id: Long, date: String)
}

@Dao
interface DaySummaryDao {
    @Query("SELECT * FROM day_summary ORDER BY date DESC") fun observe(): Flow<List<DaySummaryEntity>>
    @Query("SELECT * FROM day_summary WHERE date = :date") suspend fun get(date: String): DaySummaryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(summary: DaySummaryEntity)
    @Query("SELECT date FROM summary_dirty ORDER BY date DESC LIMIT :limit") suspend fun dirty(limit: Int): List<String>
    @Query("SELECT daily_state.date FROM daily_state LEFT JOIN day_summary ON daily_state.date = day_summary.date WHERE day_summary.date IS NULL ORDER BY daily_state.date DESC LIMIT :limit") suspend fun missing(limit: Int): List<String>
    @Query("DELETE FROM summary_dirty WHERE date = :date") suspend fun clearDirty(date: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun markDirty(dirty: SummaryDirtyEntity)
    @Query("SELECT * FROM day_summary WHERE date <= :today ORDER BY date") suspend fun getAll(today: String): List<DaySummaryEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM daily_state LEFT JOIN day_summary ON daily_state.date = day_summary.date WHERE day_summary.date IS NULL)") suspend fun hasMissing(): Boolean
    @Query("WITH RECURSIVE current_run(date, run) AS (SELECT CASE WHEN EXISTS(SELECT 1 FROM day_summary WHERE date = :today AND goalMet = 1) THEN :today ELSE DATE(:today, '-1 day') END, 0 UNION ALL SELECT DATE(date, '-1 day'), run + 1 FROM current_run WHERE EXISTS(SELECT 1 FROM day_summary WHERE day_summary.date = current_run.date AND goalMet = 1)), runs(date, run) AS (SELECT date, 1 FROM day_summary AS initial_day WHERE goalMet = 1 AND date <= :today AND NOT EXISTS(SELECT 1 FROM day_summary AS previous_day WHERE previous_day.date = DATE(initial_day.date, '-1 day') AND previous_day.goalMet = 1) UNION ALL SELECT next_day.date, runs.run + 1 FROM runs JOIN day_summary AS next_day ON next_day.date = DATE(runs.date, '+1 day') WHERE next_day.goalMet = 1 AND next_day.date <= :today) SELECT (SELECT MAX(run) FROM current_run) AS `current`, COALESCE((SELECT MAX(run) FROM runs), 0) AS longest, (SELECT COUNT(*) FROM day_summary WHERE goalMet = 1 AND date <= :today) AS successfulDays")
    suspend fun streak(today: String): com.example.data.repository.StreakSummary
    @Query("SELECT COALESCE(SUM(goalMet), 0) AS goalDays, COALESCE(SUM(proteinMet), 0) AS proteinDays, COALESCE(SUM(waterMet), 0) AS waterDays, COALESCE(SUM(sleepMet), 0) AS sleepDays, COALESCE(SUM(CASE WHEN routineDone > 0 THEN 1 ELSE 0 END), 0) AS routineDays, COALESCE(SUM(checkins), 0) AS checkins, COALESCE(SUM(earlyCheckin), 0) AS earlyDays, COALESCE(SUM(CASE WHEN tasksTotal >= 3 AND tasksDone >= tasksTotal THEN 1 ELSE 0 END), 0) AS perfectQuests, COALESCE(MAX(trackedSteps), 0) AS maxSteps, COALESCE(SUM(trackedMeters), 0.0) AS meters FROM day_summary WHERE date <= :today")
    suspend fun totals(today: String): SummaryTotals
    @Query("SELECT COUNT(*) FROM day_summary AS returned WHERE goalMet = 1 AND date <= :today AND NOT EXISTS(SELECT 1 FROM day_summary AS prior WHERE prior.date = DATE(returned.date, '-1 day') AND prior.goalMet = 1) AND EXISTS(SELECT 1 FROM day_summary AS older WHERE older.goalMet = 1 AND older.date < DATE(returned.date, :gap))")
    suspend fun comebacks(today: String, gap: String): Int
    @Query("SELECT COUNT(*) FROM (SELECT STRFTIME('%Y-%W', date) AS week FROM day_summary WHERE workoutDone = 1 AND date <= :today GROUP BY week HAVING COUNT(*) >= 3)") suspend fun gymWeeks(today: String): Int
}

data class SummaryTotals(val goalDays: Int, val proteinDays: Int, val waterDays: Int, val sleepDays: Int,
    val routineDays: Int, val checkins: Int, val earlyDays: Int, val perfectQuests: Int, val maxSteps: Int, val meters: Double)

@Dao
interface MomentDao {
    @Query("SELECT * FROM moment_log WHERE date = :date ORDER BY updatedAt") suspend fun forDate(date: String): List<MomentLogEntity>
    @Query("SELECT * FROM moment_log WHERE momentKey = :key") suspend fun get(key: String): MomentLogEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(moment: MomentLogEntity)
}