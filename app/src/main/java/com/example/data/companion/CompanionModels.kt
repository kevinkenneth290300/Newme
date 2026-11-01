package com.example.data.companion

import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.DisciplineScoreBreakdown
import com.example.data.repository.DailyGoalProgress

data class CompanionContext(val profile: UserProfileEntity?, val score: DisciplineScoreBreakdown,
    val protein: Int, val water: Int, val workoutDone: Boolean, val sleepLogged: Boolean, val goal: DailyGoalProgress)

enum class CompanionTone(val displayName: String, val philosophy: String) {
    CALM("Calm Companion", "Thoughtful, gentle and reassuring."),
    DIRECT("Direct Coach", "Honest, practical and willing to challenge you."),
    STRICT("Strict Accountability", "Push me when I'm avoiding what I know I should do."),
    BALANCED("Balanced", "Support me when I'm struggling. Challenge me when I'm making excuses.")
}

enum class TimeOfDayContext {
    MORNING,
    MIDDAY,
    EVENING,
    NIGHT
}

enum class MoodState(val displayName: String, val emoji: String) {
    TIRED("Tired", "😴"),
    OKAY("Okay", "🙂"),
    READY("Ready", "💪"),
    UNMOTIVATED("Unmotivated", "😐"),
    MOTIVATED("Motivated", "🔥"),
    STRESSED("Stressed", "😟"),
    OVERWHELMED("Overwhelmed", "😵")
}

enum class ProgressState {
    BEHIND,
    ON_TRACK,
    AHEAD,
    NEARLY_COMPLETE,
    COMPLETED
}

enum class CompanionTheme {
    DISCIPLINE,
    SELF_RESPECT,
    CONTROL,
    CONSISTENCY,
    RESILIENCE,
    PATIENCE,
    FOCUS,
    RECOVERY,
    FAILURE,
    STARTING_AGAIN,
    SELF_REFLECTION,
    MENTAL_CALM
}

enum class ActionType {
    START_WORKOUT,
    LOG_PROTEIN,
    LOG_WATER,
    CHECK_IN,
    COMPLETE_MEAL,
    SLEEP_TARGET,
    SLEEP_PREP,
    RECOVERY_DAY,
    VIEW_NEXT_STEPS
}

data class CompanionContent(
    val id: String,
    val timeOfDay: TimeOfDayContext,
    val theme: CompanionTheme,
    val moods: List<MoodState>,
    val progressStates: List<ProgressState>,
    val tone: CompanionTone,
    val message: String,
    val optionalAction: String? = null,
    val actionType: ActionType? = null,
    val priority: Int = 1
)

data class CompanionMessage(
    val greeting: String,
    val message: String,
    val tone: String,
    val suggestedAction: String? = null,
    val actionType: ActionType? = null,
    val optionalVoiceText: String? = null,
    val context: TimeOfDayContext = TimeOfDayContext.MORNING,
    val priority: Int = 1,
    val identityEcho: String? = null
)
