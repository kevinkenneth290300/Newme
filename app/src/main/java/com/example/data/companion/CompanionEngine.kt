package com.example.data.companion

import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.ActivityKind
import com.example.data.repository.DailyGoalProgress
import java.time.LocalTime

object CompanionEngine {

    fun determineTimeOfDayContext(hour: Int = LocalTime.now().hour): TimeOfDayContext {
        return when (hour) {
            in 4..11 -> TimeOfDayContext.MORNING
            in 12..16 -> TimeOfDayContext.MIDDAY
            in 17..21 -> TimeOfDayContext.EVENING
            else -> TimeOfDayContext.NIGHT
        }
    }

    fun parseMoodState(moodString: String?): MoodState {
        return when (moodString?.lowercase()?.trim()) {
            "tired", "😴 tired", "😴" -> MoodState.TIRED
            "ready", "💪 ready", "💪", "energized" -> MoodState.READY
            "unmotivated", "😐 unmotivated", "😐" -> MoodState.UNMOTIVATED
            "motivated", "🔥 highly motivated", "🔥 motivated", "🔥", "great" -> MoodState.MOTIVATED
            "stressed", "😟 stressed", "😟" -> MoodState.STRESSED
            "overwhelmed", "😵 overwhelmed", "😵" -> MoodState.OVERWHELMED
            else -> MoodState.OKAY
        }
    }

    fun parseCompanionTone(toneString: String?): CompanionTone {
        return when (toneString?.uppercase()?.trim()) {
            "CALM", "CALM COMPANION", "CALM & GROUNDED", "PHILOSOPHICAL & REFLECTIVE" -> CompanionTone.CALM
            "DIRECT", "DIRECT COACH", "DIRECT & CHALLENGING" -> CompanionTone.DIRECT
            "STRICT", "STRICT ACCOUNTABILITY" -> CompanionTone.STRICT
            else -> CompanionTone.BALANCED
        }
    }

    fun determineProgressState(percent: Int): ProgressState {
        return when {
            percent >= 100 -> ProgressState.COMPLETED
            percent >= 75 -> ProgressState.NEARLY_COMPLETE
            percent >= 50 -> ProgressState.ON_TRACK
            percent >= 25 -> ProgressState.ON_TRACK
            else -> ProgressState.BEHIND
        }
    }

    fun generateCompanionMessage(
        profile: UserProfileEntity?,
        currentHour: Int = LocalTime.now().hour,
        currentMood: MoodState = MoodState.OKAY,
        progressPercent: Int = 0,
        proteinLoggedGrams: Int = 0,
        waterLoggedMl: Int = 0,
        hasCompletedWorkout: Boolean = false,
        hasLoggedSleep: Boolean = false,
        isRecoveryMode: Boolean = false,
        rotationSeed: Long = 0,
        dailyGoal: DailyGoalProgress? = null,
        targetProteinGrams: Int? = null,
        targetWaterMl: Int? = null,
        isHistorical: Boolean = false
    ): CompanionMessage {
        val name = profile?.name?.trim()?.ifBlank { "there" } ?: "there"
        val timeOfDay = determineTimeOfDayContext(currentHour)
        val progressState = determineProgressState(progressPercent)
        val userTone = parseCompanionTone(profile?.companionTone)

        // 1. Contextual Greeting
        val greeting = when (timeOfDay) {
            TimeOfDayContext.MORNING -> "Good morning, $name."
            TimeOfDayContext.MIDDAY -> "Good afternoon, $name."
            TimeOfDayContext.EVENING -> "Good evening, $name."
            TimeOfDayContext.NIGHT -> "Good night, $name."
        }

        // 2. Identity echo check (if profile has traits e.g. "Consistent", "Disciplined", "Strong", etc.)
        val identityTraits = profile?.identityTraits?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val identityEcho = if (identityTraits.isNotEmpty()) {
            val trait = identityTraits.first()
            "Remember: you are becoming $trait."
        } else null

        // 3. Select message from content library with deterministic rotation based on hour & mood
        val allTimeMessages = CompanionContentRepository.getMessages(timeOfDay).filter { content ->
            content.tone == userTone && currentMood in content.moods && progressState in content.progressStates
        }

        // Filter priority: mood match & progress match
        val pool = allTimeMessages.filter { !it.message.contains("you're home", true) &&
            !it.message.contains("you've completed", true) && !it.message.contains("you have completed", true) }
        val selectedContent = pool.getOrNull(if (pool.isEmpty()) 0 else Math.floorMod(rotationSeed + currentMood.ordinal + userTone.ordinal, pool.size.toLong()).toInt())
        val fallback = "Choose one manageable action. Your next decision can be a fresh start."

        // 4. Determine "Your Next Move" intelligently
        val targetProtein = targetProteinGrams ?: profile?.proteinTargetGrams ?: 0
        val targetWater = targetWaterMl ?: ((profile?.waterTargetLiters ?: 0f) * 1000).toInt()
        val gymSelected = profile?.selectedActivities?.split(",")?.any { ActivityKind.fromStored(it) == ActivityKind.GYM } == true

        val (nextActionTitle, actionType) = when (timeOfDay) {
            TimeOfDayContext.MORNING -> {
                when {
                    targetWater > 0 && waterLoggedMl < minOf(500, targetWater) -> "Record water you drink" to ActionType.LOG_WATER
                    profile?.usesRecommendedPlan == true && proteinLoggedGrams == 0 -> "Review your meal suggestions" to ActionType.COMPLETE_MEAL
                    !hasCompletedWorkout && gymSelected -> "Open your gym plan" to ActionType.START_WORKOUT
                    else -> "Review morning commitments" to ActionType.VIEW_NEXT_STEPS
                }
            }
            TimeOfDayContext.MIDDAY -> {
                when {
                    targetWater > 0 && waterLoggedMl < minOf(1200, targetWater) -> "Record water you drink" to ActionType.LOG_WATER
                    targetProtein > 0 && proteinLoggedGrams < (targetProtein * 0.4f) -> "Record your meal or protein" to ActionType.COMPLETE_MEAL
                    !hasCompletedWorkout && gymSelected -> "Review your gym plan" to ActionType.START_WORKOUT
                    else -> "Check remaining afternoon actions" to ActionType.VIEW_NEXT_STEPS
                }
            }
            TimeOfDayContext.EVENING -> {
                when {
                    !hasCompletedWorkout && gymSelected -> "Open your gym plan" to ActionType.START_WORKOUT
                    targetProtein > 0 && proteinLoggedGrams < (targetProtein * 0.8f) -> "Record your dinner or protein" to ActionType.COMPLETE_MEAL
                    targetWater > 0 && waterLoggedMl < targetWater -> "Record your water intake" to ActionType.LOG_WATER
                    else -> "Wind down living space" to ActionType.VIEW_NEXT_STEPS
                }
            }
            TimeOfDayContext.NIGHT -> {
                when {
                    !hasLoggedSleep -> "Record sleep when you wake" to ActionType.SLEEP_PREP
                    else -> "Review your evening check-in" to ActionType.CHECK_IN
                }
            }
        }

        // 5. Build voice text
        val message = when {
            isHistorical -> "These are your saved records for the selected day. Today's settings do not rewrite that history."
            dailyGoal?.met == true -> "Your recorded daily goal is met. Keep the next step manageable."
            isRecoveryMode -> "Restart with one manageable action. Previously earned XP is yours to keep."
            else -> selectedContent?.message ?: fallback
        }
        val action = if (isHistorical) "Review this day's records" else nextActionTitle
        val voiceText = "$greeting $message"

        return CompanionMessage(
            greeting = greeting,
            message = message,
            tone = userTone.displayName,
            suggestedAction = action,
            actionType = if (isHistorical) ActionType.VIEW_NEXT_STEPS else actionType,
            optionalVoiceText = voiceText,
            context = timeOfDay,
            priority = selectedContent?.priority ?: 1,
            identityEcho = identityEcho
        )
    }
}
