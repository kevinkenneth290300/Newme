package com.example.data.moment

import androidx.room.withTransaction
import com.example.R
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.*
import com.example.data.model.DayPart
import com.example.data.repository.NewMeRepository
import com.example.util.DeviceClock
import org.json.JSONObject
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class PreparedMoment(val candidate: MomentCandidate, val cards: List<MomentCard>, val answers: Map<String, String>) {
    val card: MomentCard? get() = cards.firstOrNull { it.type !in answers }
}

class MomentsRepository(private val database: NewMeDatabase, private val clock: Clock = DeviceClock()) {
    suspend fun prepare(permissionOpen: Boolean = false): PreparedMoment? = database.withTransaction {
        val settings = database.settingsDao().get()
        val profile = database.userProfileDao().getUserProfileDirect()
        val date = MomentRules.date(clock).toString()
        val logs = database.momentDao().forDate(date)
        val checkins = listOf("MORNING", "MIDDAY", "EVENING").mapNotNull { database.dailyCheckinDao().getCheckinDirect(date, it) }
        var lastAction: String? = null
        database.openHelper.readableDatabase.query("SELECT MAX(date) FROM reward_ledger WHERE date < ?", arrayOf(date)).use { if (it.moveToFirst() && !it.isNull(0)) lastAction = it.getString(0) }
        val breakDays = lastAction?.let { ChronoUnit.DAYS.between(LocalDate.parse(it), LocalDate.parse(date)) - 1 } ?: 0
        val candidate = MomentRules.next(clock, DayPart.at(LocalTime.now(clock).hour), settings, checkins, logs,
            database.activityTrackingDao().getActive() != null, profile?.isOnboarded == true, permissionOpen, breakDays) ?: return@withTransaction null
        val previous = database.momentDao().get(candidate.key)
        if (previous?.status in setOf("ANSWERED", "SKIPPED")) return@withTransaction null
        val log = (previous ?: MomentLogEntity(candidate.key, date, candidate.part, candidate.type, updatedAt = clock.millis()))
            .copy(status = "SHOWING", updatedAt = clock.millis())
        database.momentDao().save(log)
        prepared(candidate.copy(type = log.type), log)
    }

    private suspend fun prepared(candidate: MomentCandidate, log: MomentLogEntity): PreparedMoment {
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        val profile = database.userProfileDao().getUserProfileDirect()
        val enabled = settings.momentTypes.split(',').toSet()
        val profileAlreadyAsked = database.momentDao().forDate(candidate.date).any { JSONObject(it.answers).has("profile") }
        val profileField = when {
            profile?.startingReason.isNullOrBlank() -> "reason"
            profile?.identityTraits.isNullOrBlank() -> "identity"
            profile?.workType.isNullOrBlank() -> "work"
            profile?.commonFoods.isNullOrBlank() -> "foods"
            else -> null
        }
        val cards = MomentCatalog.cards(candidate, enabled, database.sleepDao().getLog(candidate.date) == null,
            profileField != null && !profileAlreadyAsked).map { card ->
            if (card.type == "profile") {
                val choices = when (profileField) {
                    "identity" -> listOf("Consistent" to R.string.trait_consistent, "Curious" to R.string.trait_curious, "Grounded" to R.string.trait_grounded)
                    "work" -> listOf("Office" to R.string.work_office, "Physical" to R.string.work_physical, "Flexible" to R.string.work_flexible)
                    "foods" -> listOf("Plant-based" to R.string.foods_plant, "Mixed" to R.string.foods_mixed, "Vegetarian" to R.string.foods_vegetarian)
                    else -> listOf("Health" to R.string.reason_health, "Energy" to R.string.reason_energy, "Calm" to R.string.focus_calm, "Growth" to R.string.reason_growth)
                }
                card.copy(question = when (profileField) { "identity" -> R.string.profile_identity_question; "work" -> R.string.profile_work_question; "foods" -> R.string.profile_foods_question; else -> R.string.moment_reason_question },
                    options = choices.map { (value, label) -> MomentOption(JSONObject().put("field", profileField).put("value", value).toString(), label, "focus") })
            } else if (card.type != "win") card else {
                val completed = database.dailyChecklistDao().getChecklistForDateDirect(candidate.date).filter { it.isCompleted }.map {
                    MomentOption("task:${it.id}", R.string.win_something_else, "focus", it.title)
                } + database.routineDao().completions(candidate.date).mapNotNull { completion ->
                    database.routineDao().get(completion.templateId)?.let { MomentOption("routine:${it.id}", R.string.win_something_else, it.iconKey, it.title) }
                }
                card.copy(options = completed.take(3) + card.options)
            }
        }
        val json = JSONObject(log.answers)
        val answers = json.keys().asSequence().associateWith { json.getString(it) }
        return PreparedMoment(candidate, cards, answers)
    }

    suspend fun answer(candidate: MomentCandidate, type: String, value: String): PreparedMoment? = database.withTransaction {
        val log = database.momentDao().get(candidate.key) ?: return@withTransaction null
        if (log.status in setOf("ANSWERED", "SKIPPED")) return@withTransaction null
        check(database.activityTrackingDao().getActive() == null) { "Finish the activity before answering a Moment." }
        val current = prepared(candidate, log)
        val card = current.card ?: return@withTransaction null
        require(card.type == type && card.options.any { it.value == value }) { "Choose a valid answer." }
        val json = JSONObject(log.answers).put(type, value)
        val updated = log.copy(answers = json.toString(), updatedAt = clock.millis())
        val next = prepared(candidate, updated)
        if (next.card != null) {
            database.momentDao().save(updated)
            return@withTransaction next
        }
        val repository = NewMeRepository(database, clock)
        if (candidate.type != "COMEBACK") {
            val period = candidate.part
            val existing = database.dailyCheckinDao().getCheckinDirect(candidate.date, period)
                ?: DailyCheckinEntity(date = candidate.date, timeOfDay = period)
            val win = json.optString("win")
            val winText = current.cards.firstOrNull { it.type == "win" }?.options?.firstOrNull { it.value == win }?.text
            repository.saveDailyCheckin(existing.copy(mood = json.optString("mood", existing.mood), mattersMost = json.optString("focus", winText ?: existing.mattersMost),
                middayStatus = json.optString("energy", existing.middayStatus), refuseToLeaveUnfinished = if (win == "other") "A personal win" else winText ?: existing.refuseToLeaveUnfinished,
                isCompleted = true, timestamp = clock.millis()))
            if (json.has("sleep") && database.sleepDao().getLog(candidate.date) == null) {
                val sleep = json.getString("sleep").split(':')
                database.sleepDao().insertOrUpdateSleepLog(SleepLogEntity(candidate.date, bedtime = "", wakeTime = "", durationMinutes = sleep[0].toInt(), qualityRating = sleep[1].toInt(), source = "Moment"))
            }
            if (json.optString("hydrate") == "250") repository.addWater(candidate.date, 250)
            if (json.has("profile")) database.userProfileDao().getUserProfileDirect()?.let { currentProfile ->
                val answer = JSONObject(json.getString("profile"))
                val value = answer.getString("value")
                repository.saveProfile(when (answer.getString("field")) {
                    "identity" -> currentProfile.copy(identityTraits = value)
                    "work" -> currentProfile.copy(workType = value)
                    "foods" -> currentProfile.copy(commonFoods = value)
                    else -> currentProfile.copy(startingReason = value)
                })
            }
        }
        database.momentDao().save(updated.copy(status = "ANSWERED"))
        null
    }

    suspend fun later(candidate: MomentCandidate) = database.withTransaction {
        database.momentDao().get(candidate.key)?.let { current ->
            if (current.status !in setOf("ANSWERED", "SKIPPED")) database.momentDao().save(MomentRules.snoozed(current, clock.millis()))
        }
    }
    suspend fun skip(candidate: MomentCandidate) = database.withTransaction {
        database.momentDao().get(candidate.key)?.let { database.momentDao().save(it.copy(status = "SKIPPED", updatedAt = clock.millis())) }
    }
}