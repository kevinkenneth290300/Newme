package com.example.data.model

enum class NutritionGoal(val label: String) {
    FAT_LOSS("Fat loss"), MUSCLE_GAIN("Muscle gain"), RECOMPOSITION("Recomposition"),
    TARGET_WEIGHT("Target weight"), GENERAL_HEALTH("General health goal");

    companion object {
        fun fromStored(value: String): NutritionGoal = entries.firstOrNull { it.name.equals(value, true) }
            ?: when (value.trim().lowercase()) {
                "fat loss", "fat-loss goal" -> FAT_LOSS
                "muscle gain", "muscle goal" -> MUSCLE_GAIN
                "recomposition" -> RECOMPOSITION
                "target weight" -> TARGET_WEIGHT
                "general health goal", "general health", "discipline/self-growth",
                "i don't know yet - help me decide", "i don't know yet \u2014 help me decide" -> GENERAL_HEALTH
                else -> throw IllegalArgumentException("Choose a supported nutrition goal.")
            }
    }
}

enum class ProfileSex(val label: String) {
    MALE("Male"), FEMALE("Female"), OTHER("Other");
    companion object {
        fun fromStored(value: String): ProfileSex = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: throw IllegalArgumentException("Choose Male, Female, or Other.")
    }
}

enum class ActivityKind(val label: String, val multiplier: Float, val outdoor: Boolean = false, val countsSteps: Boolean = false) {
    WALKING("Walking", 1.35f, true, true), RUNNING("Running / Jogging", 1.55f, true, true),
    CYCLING("Cycling", 1.50f, true), DANCING("Dancing", 1.50f), SWIMMING("Swimming", 1.65f),
    GYM("Gym / Strength Training", 1.55f), YOGA("Yoga", 1.35f), SPORTS("Sports", 1.65f),
    OTHER("Other activity", 1.35f), NONE("No planned activity", 1.20f);

    companion object {
        fun fromStored(value: String): ActivityKind = entries.firstOrNull {
            it.name.equals(value, true) || it.label.equals(value, true)
        } ?: when (value.trim().lowercase()) {
            "brisk walking", "walk" -> WALKING
            "running", "jogging", "jogging/running" -> RUNNING
            "dance" -> DANCING
            "gym", "strength training" -> GYM
            "inactive", "zero", "none" -> NONE
            else -> OTHER
        }
    }
}

enum class DailyGoalKind(val label: String, val unit: String) {
    TASKS("Completed tasks", "tasks"), ACTIVITY_MINUTES("Activity minutes", "minutes");
    fun validate(target: Int) {
        require(target in (if (this == TASKS) 1..50 else 1..1440)) {
            if (this == TASKS) "Choose 1 to 50 tasks." else "Choose 1 to 1440 activity minutes."
        }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class OnboardingPreferences(
    val dailyGoalKind: DailyGoalKind,
    val dailyGoalTarget: Int,
    val cycleEnabled: Boolean = false
)