package com.example.data.moment

import com.example.R
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyCheckinEntity
import com.example.data.local.entity.MomentLogEntity
import com.example.data.model.DayPart
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class MomentCandidate(val key: String, val date: String, val part: String, val type: String)
data class MomentOption(val value: String, val label: Int, val icon: String, val text: String? = null)
data class MomentCard(val type: String, val question: Int, val options: List<MomentOption>)

object MomentRules {
    fun isNewOpen(now: Long, backgroundAt: Long?, coldStart: Boolean): Boolean = coldStart ||
        (backgroundAt != null && now - backgroundAt >= 10 * 60_000L)

    fun date(clock: Clock): LocalDate = LocalDate.now(clock).let {
        if (LocalTime.now(clock).hour < 4) it.minusDays(1) else it
    }

    fun next(clock: Clock, dayPart: DayPart, settings: AppSettingsEntity?, checkins: List<DailyCheckinEntity>,
        logs: List<MomentLogEntity>, trackingActive: Boolean, onboarded: Boolean, permissionOpen: Boolean = false, breakDays: Long = 0): MomentCandidate? {
        if (!onboarded || settings?.momentsEnabled != true || !settings.dashboardSetupDone || trackingActive || permissionOpen) return null
        val date = date(clock).toString()
        val part = dayPart.checkinPeriod
        val enabled = settings.momentTypes.split(',').toSet()
        if (breakDays >= 2 && "comeback" in enabled && logs.none { it.type == "COMEBACK" && it.status in setOf("ANSWERED", "SKIPPED") }) {
            return MomentCandidate("$date:$part", date, part, "COMEBACK")
        }
        if (checkins.any { it.date == date && it.timeOfDay == part && it.isCompleted }) return null
        val key = "$date:$part"
        val previous = logs.firstOrNull { it.momentKey == key }
        if (previous?.status in setOf("ANSWERED", "SKIPPED") || (previous?.snoozeCount ?: 0) >= 3) return null
        val available = when (part) {
            "MORNING" -> setOf("sleep", "mood", "focus")
            "MIDDAY" -> setOf("energy", "hydrate")
            else -> setOf("mood", "win")
        }
        if (available.none { it in enabled }) return null
        return MomentCandidate(key, date, part, part)
    }

    fun snoozed(current: MomentLogEntity, now: Long): MomentLogEntity {
        val count = current.snoozeCount + 1
        return current.copy(status = if (count >= 3) "SKIPPED" else "SNOOZED", snoozeCount = count, updatedAt = now)
    }
}

object MomentCatalog {
    private val mood = listOf(MomentOption("Tired", R.string.mood_tired, "moon"), MomentOption("Stressed", R.string.mood_stressed, "focus"),
        MomentOption("Okay", R.string.mood_okay, "sun"), MomentOption("Good", R.string.mood_good, "move"), MomentOption("Great", R.string.mood_great, "sun"))
    fun cards(candidate: MomentCandidate, enabled: Set<String>, includeSleep: Boolean, includeProfile: Boolean): List<MomentCard> {
        if (candidate.type == "COMEBACK") return listOf(MomentCard("comeback", R.string.moment_comeback_question, listOf(
            MomentOption("tiny", R.string.moment_tiny_win, "sun"), MomentOption("plan", R.string.moment_see_plan, "calendar"), MomentOption("later", R.string.moment_not_now, "moon"))))
        val cards: List<MomentCard> = when (candidate.part) {
            "MORNING" -> listOfNotNull(
                if (includeSleep) MomentCard("sleep", R.string.moment_sleep_question, listOf(
                    MomentOption("270:1", R.string.sleep_under_five, "moon"), MomentOption("330:2", R.string.sleep_five_six, "moon"),
                    MomentOption("390:3", R.string.sleep_six_seven, "bed"), MomentOption("450:4", R.string.sleep_seven_eight, "sun"),
                    MomentOption("510:5", R.string.sleep_eight_plus, "sun"))) else null,
                MomentCard("mood", R.string.mood_question, mood),
                MomentCard("focus", R.string.moment_focus_question, listOf(MomentOption("Move", R.string.log_move, "move"), MomentOption("Eat well", R.string.focus_eat, "meal"),
                    MomentOption("Calm", R.string.focus_calm, "moon"), MomentOption("Learn", R.string.focus_learn, "focus"), MomentOption("Tidy", R.string.focus_tidy, "tidy"))))
            "MIDDAY" -> listOf(MomentCard("energy", R.string.moment_energy_question, listOf(MomentOption("Low", R.string.energy_low, "moon"),
                MomentOption("Okay", R.string.mood_okay, "sun"), MomentOption("High", R.string.energy_high, "move"))),
                MomentCard("hydrate", R.string.moment_hydrate_question, listOf(MomentOption("250", R.string.add_glass, "water"), MomentOption("later", R.string.later, "moon"))))
            else -> listOf(MomentCard("mood", R.string.moment_evening_question, mood), MomentCard("win", R.string.moment_win_question,
                listOf(MomentOption("other", R.string.win_something_else, "sun"))))
        }.filter { it.type in enabled }
        val profileCards: List<MomentCard> = if (includeProfile && "profile" in enabled) listOf(MomentCard("profile", R.string.moment_reason_question, listOf(
            MomentOption("Health", R.string.reason_health, "sun"), MomentOption("Energy", R.string.reason_energy, "move"),
            MomentOption("Calm", R.string.focus_calm, "moon"), MomentOption("Growth", R.string.reason_growth, "focus")))) else emptyList()
        return cards + profileCards
    }
}