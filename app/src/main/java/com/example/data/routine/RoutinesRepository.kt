package com.example.data.routine

import android.content.Context
import androidx.room.withTransaction
import com.example.R
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.*
import com.example.data.repository.GamificationRepository
import com.example.data.repository.NewMeRepository
import com.example.util.DeviceClock
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate

enum class AppModule { ROUTINES, MEALS, GYM, ACTIVITY, WATER, SLEEP, CYCLE }

object RoutineSchedule {
    fun isDue(template: RoutineTemplateEntity, date: LocalDate): Boolean {
        if (!template.isActive || date.isBefore(LocalDate.parse(template.startDate))) return false
        return when (template.recurrence) {
            "DAILY" -> true
            "WEEKDAYS", "WEEKLY" -> template.daysOfWeekMask and (1 shl (date.dayOfWeek.value - 1)) != 0
            "MONTHLY" -> date.dayOfMonth == template.dayOfMonth.coerceAtMost(date.lengthOfMonth())
            else -> false
        }
    }

    fun next(template: RoutineTemplateEntity, from: LocalDate): LocalDate? =
        (0L..370L).asSequence().map { from.plusDays(it) }.firstOrNull { isDue(template, it) }

    fun validate(template: RoutineTemplateEntity) {
        require(template.title.isNotBlank() && template.title.length <= 80) { "Enter a routine title of 1 to 80 characters." }
        require(template.recurrence in setOf("DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY")) { "Choose a valid repeat pattern." }
        require(template.daysOfWeekMask in 1..127 && template.dayOfMonth in 1..31) { "Choose valid repeat days." }
        require(template.recurrence != "WEEKLY" || Integer.bitCount(template.daysOfWeekMask) == 1) { "Choose one weekly day." }
        LocalDate.parse(template.startDate)
    }
}

data class RoutineQuest(val template: RoutineTemplateEntity, val completed: Boolean)
data class StarterItem(val titleRes: Int, val icon: String, val category: String, val recurrence: String = "DAILY", val mask: Int = 127)
data class StarterPack(val id: String, val titleRes: Int, val items: List<StarterItem>)

object StarterPacks {
    val catalog = listOf(
        StarterPack("morning", R.string.pack_morning, listOf(
            StarterItem(R.string.routine_make_bed, "bed", "MORNING"), StarterItem(R.string.routine_curtains, "sun", "MORNING"),
            StarterItem(R.string.routine_drink_water, "water", "MORNING"), StarterItem(R.string.routine_stretch, "move", "MORNING"),
            StarterItem(R.string.routine_priorities, "focus", "MORNING"))),
        StarterPack("tidy", R.string.pack_tidy, listOf(
            StarterItem(R.string.routine_clear_desk, "tidy", "AFTER_WORK"), StarterItem(R.string.routine_dishes, "meal", "AFTER_WORK"),
            StarterItem(R.string.routine_laundry, "laundry", "WEEKLY", "WEEKLY", 32),
            StarterItem(R.string.routine_deep_clean, "tidy", "WEEKLY", "WEEKLY", 64),
            StarterItem(R.string.routine_inbox, "inbox", "MONTHLY", "MONTHLY"), StarterItem(R.string.routine_bills, "calendar", "MONTHLY", "MONTHLY"))),
        StarterPack("leaving", R.string.pack_leaving, listOf(
            StarterItem(R.string.routine_essentials, "key", "LEAVING_HOME"), StarterItem(R.string.routine_bottle, "water", "LEAVING_HOME"),
            StarterItem(R.string.routine_sink, "tidy", "LEAVING_HOME"))),
        StarterPack("wind_down", R.string.pack_wind_down, listOf(
            StarterItem(R.string.routine_charge, "phone", "BEFORE_SLEEP"), StarterItem(R.string.routine_clothes, "clothes", "BEFORE_SLEEP"),
            StarterItem(R.string.routine_counter, "tidy", "BEFORE_SLEEP"), StarterItem(R.string.routine_lights, "moon", "BEFORE_SLEEP"))),
        StarterPack("healthy", R.string.pack_healthy, listOf(
            StarterItem(R.string.routine_water_glass, "water", "MORNING"), StarterItem(R.string.routine_protein, "protein", "AFTER_WORK"),
            StarterItem(R.string.routine_movement, "move", "AFTER_WORK"), StarterItem(R.string.routine_sugary_drinks, "meal", "BEFORE_SLEEP")))
    )
}

class RoutinesRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    val templates = database.routineDao().observe()
    fun quests(date: String) = combine(templates, database.routineDao().observeCompletions(date)) { routines, completions ->
        routines.filter { RoutineSchedule.isDue(it, LocalDate.parse(date)) }.map { routine ->
            RoutineQuest(routine, completions.any { it.templateId == routine.id })
        }
    }

    suspend fun save(template: RoutineTemplateEntity): Long = database.withTransaction {
        RoutineSchedule.validate(template)
        val id = if (template.id == 0L) database.routineDao().insert(template.copy(createdAt = clock.millis()))
        else {
            val existing = checkNotNull(database.routineDao().get(template.id)) { "This routine no longer exists." }
            database.routineDao().update(template.copy(source = existing.source, createdAt = existing.createdAt))
            template.id
        }
        database.daySummaryDao().markDirty(SummaryDirtyEntity(LocalDate.now(clock).toString()))
        id
    }

    suspend fun complete(id: Long, date: String, completed: Boolean) = database.withTransaction {
        val day = LocalDate.parse(date)
        require(!day.isAfter(LocalDate.now(clock))) { "Future routines cannot be completed." }
        val template = checkNotNull(database.routineDao().get(id)) { "This routine no longer exists." }
        require(RoutineSchedule.isDue(template, day)) { "This routine is not due on this date." }
        NewMeRepository(database, clock).ensureDateInitialized(date)
        if (completed) {
            database.routineDao().complete(RoutineCompletionEntity(id, date, clock.millis()))
            GamificationRepository(database, clock).reward("routine:$id:$date", date, 10)
        } else database.routineDao().undo(id, date)
    }

    suspend fun delete(id: Long) = database.withTransaction {
        database.routineDao().delete(id)
        database.daySummaryDao().markDirty(SummaryDirtyEntity(LocalDate.now(clock).toString()))
    }

    suspend fun move(id: Long, direction: Int) = database.withTransaction {
        val ordered = database.routineDao().getAll().toMutableList()
        val index = ordered.indexOfFirst { it.id == id }
        val next = index + direction.coerceIn(-1, 1)
        if (index < 0 || next !in ordered.indices) return@withTransaction
        java.util.Collections.swap(ordered, index, next)
        ordered.forEachIndexed { order, routine -> database.routineDao().update(routine.copy(sortOrder = order)) }
    }

    suspend fun applyPacks(context: Context, selected: Set<String>) = database.withTransaction {
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        val applied = settings.starterPacks.split(',').filter { it.isNotBlank() }.toMutableSet()
        var order = database.routineDao().getAll().maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        var added = false
        for (pack in StarterPacks.catalog.filter { it.id in selected && it.id !in applied }) {
            for (item in pack.items) database.routineDao().insert(RoutineTemplateEntity(
                title = context.getString(item.titleRes), category = item.category, iconKey = item.icon,
                recurrence = item.recurrence, daysOfWeekMask = item.mask, startDate = LocalDate.now(clock).toString(),
                sortOrder = order++, source = "STARTER:${pack.id}", createdAt = clock.millis()))
            applied.add(pack.id)
            added = true
        }
        val modules = settings.enabledModules.split(',').filter { it.isNotBlank() }.toSet()
        database.settingsDao().save(settings.copy(starterPacks = applied.sorted().joinToString(","),
            enabledModules = if (added) (modules + AppModule.ROUTINES.name).sorted().joinToString(",") else settings.enabledModules))
        database.daySummaryDao().markDirty(SummaryDirtyEntity(LocalDate.now(clock).toString()))
    }
}