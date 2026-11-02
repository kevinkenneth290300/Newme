package com.example.data.reminder

import androidx.room.withTransaction
import com.example.data.cycle.CyclePrediction
import com.example.data.cycle.CycleRules
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyChecklistItemEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.RoutineTemplateEntity
import com.example.data.routine.RoutineSchedule
import com.example.util.DeviceClock
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class ReminderOccurrence(val key: String, val token: String, val epochMillis: Long, val timeSensitive: Boolean)

object ReminderRules {
    const val DAILY = "daily"
    const val CYCLE = "cycle"

    fun afterQuietHours(time: ZonedDateTime, start: Int, end: Int): ZonedDateTime {
        require(start in 0..1439 && end in 0..1439) { "Quiet hours must be valid local times." }
        if (start == end) return time
        val minute = time.hour * 60 + time.minute
        val quiet = if (start < end) minute in start until end else minute >= start || minute < end
        if (!quiet) return time
        val day = if (start > end && minute >= start) time.toLocalDate().plusDays(1) else time.toLocalDate()
        return day.atTime(LocalTime.ofSecondOfDay(end * 60L)).atZone(time.zone)
    }

    fun next(
        reminder: ReminderEntity,
        settings: AppSettingsEntity,
        now: ZonedDateTime,
        task: DailyChecklistItemEntity? = null,
        prediction: CyclePrediction = CyclePrediction.Insufficient,
        routine: RoutineTemplateEntity? = null,
        routineCompleted: Boolean = false
    ): ReminderOccurrence? {
        require(reminder.minuteOfDay in 0..1439) { "Reminder time is invalid." }
        if (!reminder.enabled) return null
        val dates = when (reminder.kind) {
            "DAILY" -> {
                if (!settings.taskRemindersEnabled || reminder.key != DAILY) return null
                (0L..2L).map { now.toLocalDate().plusDays(it) }
            }
            "TASK" -> {
                if (!settings.taskRemindersEnabled || task == null || task.isCompleted || task.id != reminder.taskId) return null
                listOf(LocalDate.parse(task.date))
            }
            "ROUTINE" -> {
                if (!settings.taskRemindersEnabled || "ROUTINES" !in settings.enabledModules.split(',') ||
                    routine == null || !routine.isActive || routine.id != reminder.routineId) return null
                val from = if (reminder.lastDeliveredToken.isNotBlank() && reminder.lastDeliveredToken >= now.toLocalDate().toString())
                    LocalDate.parse(reminder.lastDeliveredToken).plusDays(1) else now.toLocalDate()
                val next = RoutineSchedule.next(routine, if (routineCompleted) maxOf(from, now.toLocalDate().plusDays(1)) else from)
                listOf(next ?: return null)
            }
            "CYCLE" -> {
                if (!settings.cycleEnabled || !settings.cycleRemindersEnabled || reminder.key != CYCLE) return null
                listOf((prediction as? CyclePrediction.Estimate)?.date ?: return null)
            }
            else -> error("Unsupported reminder type.")
        }
        for (date in dates) {
            val token = date.toString()
            if (date.isBefore(now.toLocalDate()) || token <= reminder.lastDeliveredToken) continue
            val requested = date.atTime(LocalTime.ofSecondOfDay(reminder.minuteOfDay * 60L)).atZone(now.zone)
            val allowed = afterQuietHours(requested, settings.quietStartMinutes, settings.quietEndMinutes)
            if (reminder.kind == "TASK" && allowed.toLocalDate() != date) continue
            return ReminderOccurrence(reminder.key, token, allowed.toInstant().toEpochMilli(),
                reminder.timeSensitive && reminder.kind != "CYCLE")
        }
        return null
    }
}

class ReminderRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    val reminders = database.reminderDao().observe()

    suspend fun setTaskEnabled(enabled: Boolean) = database.withTransaction {
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(settings.copy(taskRemindersEnabled = enabled))
        if (enabled && database.reminderDao().get(ReminderRules.DAILY) == null) {
            database.reminderDao().save(ReminderEntity(ReminderRules.DAILY, "DAILY", enabled = true))
        }
    }

    suspend fun setCycleEnabled(enabled: Boolean) = database.withTransaction {
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        check(!enabled || settings.cycleEnabled) { "Enable cycle tracking before choosing its reminders." }
        database.settingsDao().save(settings.copy(cycleRemindersEnabled = enabled))
        val current = database.reminderDao().get(ReminderRules.CYCLE)
            ?: ReminderEntity(ReminderRules.CYCLE, "CYCLE")
        database.reminderDao().save(current.copy(enabled = enabled, timeSensitive = false))
    }

    suspend fun quietHours(start: Int, end: Int) = database.withTransaction {
        require(start in 0..1439 && end in 0..1439) { "Choose valid quiet hours." }
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(settings.copy(quietStartMinutes = start, quietEndMinutes = end))
    }

    suspend fun save(key: String, minute: Int, enabled: Boolean, timeSensitive: Boolean, taskId: Long? = null, routineId: Long? = null) = database.withTransaction {
        require(minute in 0..1439) { "Choose a valid reminder time." }
        val kind = when (key) {
            ReminderRules.DAILY -> "DAILY"
            ReminderRules.CYCLE -> "CYCLE"
            else -> {
                if (routineId != null) {
                    require(key == "routine:$routineId") { "Reminder must belong to its saved routine." }
                    check(database.routineDao().get(routineId) != null) { "This routine no longer exists." }
                    "ROUTINE"
                } else {
                    require(taskId != null && key == "task:$taskId") { "Reminder must belong to a saved task." }
                    check(database.dailyChecklistDao().getItem(taskId) != null) { "This task no longer exists." }
                    "TASK"
                }
            }
        }
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        check(kind != "CYCLE" || !enabled || settings.cycleEnabled) { "Cycle tracking is turned off." }
        val current = database.reminderDao().get(key)
        database.reminderDao().save(ReminderEntity(key, kind, taskId, minute, enabled,
            timeSensitive && kind != "CYCLE", current?.lastDeliveredToken.orEmpty(), routineId))
        if (enabled && kind != "CYCLE") database.settingsDao().save(settings.copy(taskRemindersEnabled = true))
        if (kind == "CYCLE") database.settingsDao().save(settings.copy(cycleRemindersEnabled = enabled))
    }

    suspend fun plan(): List<ReminderOccurrence> = database.withTransaction {
        val settings = database.settingsDao().get() ?: return@withTransaction emptyList()
        if (database.userProfileDao().getUserProfileDirect()?.isOnboarded != true) return@withTransaction emptyList()
        val now = ZonedDateTime.now(clock)
        val prediction = if (settings.cycleEnabled && settings.cycleRemindersEnabled)
            CycleRules.predict(database.cycleDao().getAll(), now.toLocalDate()) else CyclePrediction.Insufficient
        database.reminderDao().getAll().mapNotNull { reminder ->
            val task = reminder.taskId?.let { database.dailyChecklistDao().getItem(it) }
            val routine = reminder.routineId?.let { database.routineDao().get(it) }
            val completed = reminder.routineId?.let { database.routineDao().completion(it, now.toLocalDate().toString()) != null } == true
            ReminderRules.next(reminder, settings, now, task, prediction, routine, completed)
        }
    }

    suspend fun claim(key: String, token: String): Boolean {
        return database.withTransaction {
            val occurrence = plan().firstOrNull { it.key == key && it.token == token } ?: return@withTransaction false
            val now = ZonedDateTime.now(clock)
            val settings = database.settingsDao().get() ?: return@withTransaction false
            if (occurrence.epochMillis > clock.millis() ||
                ReminderRules.afterQuietHours(now, settings.quietStartMinutes, settings.quietEndMinutes).isAfter(now)) {
                return@withTransaction false
            }
            if (key == ReminderRules.DAILY && database.dailyChecklistDao()
                    .getChecklistForDateDirect(LocalDate.now(clock).toString()).none { !it.isCompleted }) {
                database.reminderDao().markDelivered(key, token)
                return@withTransaction false
            }
            database.reminderDao().markDelivered(key, token) == 1
        }
    }
}