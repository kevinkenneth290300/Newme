package com.example.data.companion

import com.example.data.local.entity.UserProfileEntity
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
            "CALM", "CALM COMPANION" -> CompanionTone.CALM
            "DIRECT", "DIRECT COACH" -> CompanionTone.DIRECT
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
        isRecoveryMode: Boolean = false
    ): CompanionMessage {
        val name = profile?.name?.trim()?.ifBlank { "there" } ?: "there"
        val timeOfDay = determineTimeOfDayContext(currentHour)
        val progressState = determineProgressState(progressPercent)
        val userTone = parseCompanionTone(profile?.companionTone)

        // 1. Contextual Greeting
        val greeting = when (timeOfDay) {
            TimeOfDayContext.MORNING -> "Good morning, $name."
            TimeOfDayContext.MIDDAY -> "Good afternoon, $name."
            TimeOfDayContext.EVENING -> if (currentHour >= 19) "You're home, $name." else "Good evening, $name."
            TimeOfDayContext.NIGHT -> "Good night, $name."
        }

        // 2. Identity echo check (if profile has traits e.g. "Consistent", "Disciplined", "Strong", etc.)
        val identityTraits = profile?.identityTraits?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val identityEcho = if (identityTraits.isNotEmpty()) {
            val trait = identityTraits.first()
            "Remember: you are becoming $trait."
        } else null

        // 3. Select message from content library with deterministic rotation based on hour & mood
        val allTimeMessages = CompanionContentRepository.getMessages(timeOfDay)

        // Filter priority: mood match & progress match
        val matchedMessages = allTimeMessages.filter { msg ->
            (msg.moods.contains(currentMood) || msg.moods.contains(MoodState.OKAY)) &&
            (msg.progressStates.contains(progressState) || msg.progressStates.contains(ProgressState.ON_TRACK))
        }

        val pool = if (matchedMessages.isNotEmpty()) matchedMessages else allTimeMessages
        val selectedContent = pool[(currentHour + currentMood.ordinal) % pool.size]

        // 4. Determine "Your Next Move" intelligently
        val targetProtein = profile?.proteinTargetGrams ?: 90
        val targetWater = ((profile?.waterTargetLiters ?: 2.5f) * 1000).toInt()

        val (nextActionTitle, actionType) = when (timeOfDay) {
            TimeOfDayContext.MORNING -> {
                when {
                    waterLoggedMl < 500 -> "Drink 500ml water to start your day" to ActionType.LOG_WATER
                    proteinLoggedGrams == 0 -> "Log high-protein morning meal" to ActionType.COMPLETE_MEAL
                    !hasCompletedWorkout && profile?.selectedActivities?.contains("Gym") == true -> "Start with 5-min warm-up" to ActionType.START_WORKOUT
                    else -> "Review morning commitments" to ActionType.VIEW_NEXT_STEPS
                }
            }
            TimeOfDayContext.MIDDAY -> {
                when {
                    waterLoggedMl < 1200 -> "Hydrate: Drink water before afternoon" to ActionType.LOG_WATER
                    proteinLoggedGrams < (targetProtein * 0.4f) -> "Log clean lunch with protein" to ActionType.COMPLETE_MEAL
                    !hasCompletedWorkout -> "Your evening workout is still ahead" to ActionType.VIEW_NEXT_STEPS
                    else -> "Check remaining afternoon actions" to ActionType.VIEW_NEXT_STEPS
                }
            }
            TimeOfDayContext.EVENING -> {
                when {
                    !hasCompletedWorkout && profile?.selectedActivities?.contains("Gym") == true -> "Start your planned workout session" to ActionType.START_WORKOUT
                    proteinLoggedGrams < (targetProtein * 0.8f) -> "Complete clean dinner" to ActionType.COMPLETE_MEAL
                    waterLoggedMl < targetWater -> "Reach your daily hydration target" to ActionType.LOG_WATER
                    else -> "Wind down living space" to ActionType.VIEW_NEXT_STEPS
                }
            }
            TimeOfDayContext.NIGHT -> {
                when {
                    !hasLoggedSleep -> "Log sleep schedule and close the day" to ActionType.SLEEP_PREP
                    else -> "Protect 7-8 hours of deep sleep" to ActionType.SLEEP_PREP
                }
            }
        }

        // 5. Build voice text
        val voiceText = "$greeting ${selectedContent.message}"

        return CompanionMessage(
            greeting = greeting,
            message = selectedContent.message,
            tone = userTone.displayName,
            suggestedAction = nextActionTitle,
            actionType = actionType,
            optionalVoiceText = voiceText,
            context = timeOfDay,
            priority = selectedContent.priority,
            identityEcho = identityEcho
        )
    }
}
