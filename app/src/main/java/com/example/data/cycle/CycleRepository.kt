package com.example.data.cycle

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CycleEntryEntity
import com.example.util.DeviceClock
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface CyclePrediction {
    data class Estimate(val date: LocalDate, val medianDays: Int, val intervalCount: Int) : CyclePrediction
    data object Insufficient : CyclePrediction
    data object Uncertain : CyclePrediction
}

object CycleRules {
    fun predict(entries: List<CycleEntryEntity>, today: LocalDate): CyclePrediction {
        val starts = try { entries.map { LocalDate.parse(it.startDate) }.distinct().sorted().takeLast(7) }
            catch (error: java.time.format.DateTimeParseException) { return CyclePrediction.Uncertain }
        if (starts.size < 3) return CyclePrediction.Insufficient
        if (starts.any { it.isAfter(today) }) return CyclePrediction.Uncertain
        val intervals = starts.zipWithNext { previous, next -> ChronoUnit.DAYS.between(previous, next).toInt() }
        if (intervals.any { it !in 15..60 } || intervals.max() - intervals.min() > 9) return CyclePrediction.Uncertain
        val sorted = intervals.sorted()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        val next = starts.last().plusDays(median.toLong())
        if (next.isBefore(today)) return CyclePrediction.Uncertain
        return CyclePrediction.Estimate(next, median, intervals.size)
    }
}

class CycleRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    val entries = database.cycleDao().observe()

    suspend fun enabled(enabled: Boolean) = database.withTransaction {
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(settings.copy(cycleEnabled = enabled,
            cycleRemindersEnabled = if (enabled) settings.cycleRemindersEnabled else false))
    }

    suspend fun save(entry: CycleEntryEntity) = database.withTransaction {
        val today = LocalDate.now(clock)
        val start = LocalDate.parse(entry.startDate)
        require(start.year >= 1900 && !start.isAfter(today)) { "Enter a start date no later than today." }
        entry.endDate?.let {
            val end = LocalDate.parse(it)
            require(!end.isBefore(start) && !end.isAfter(today)) { "End date must be between the start and today." }
        }
        require(entry.symptoms.length <= 1000) { "Keep optional symptoms within 1000 characters." }
        val existing = database.cycleDao().getByStart(entry.startDate)
        if (entry.id > 0) database.cycleDao().update(entry)
        else if (existing != null) database.cycleDao().update(entry.copy(id = existing.id))
        else database.cycleDao().insert(entry)
    }

    suspend fun delete(entry: CycleEntryEntity) = database.withTransaction { database.cycleDao().delete(entry) }
}