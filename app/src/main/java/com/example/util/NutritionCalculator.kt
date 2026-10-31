package com.example.util

data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val calorieTarget: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val waterLiters: Float,
    val explanation: String
)

object NutritionCalculator {

    fun calculateTargets(
        age: Int,
        sex: String,
        heightCm: Float,
        weightKg: Float,
        activityType: String,
        goal: String
    ): NutritionTargets {
        // 1. Mifflin-St Jeor Formula for BMR
        val bmr = when (sex.lowercase()) {
            "female" -> (10f * weightKg + 6.25f * heightCm - 5f * age - 161f).toInt()
            "male" -> (10f * weightKg + 6.25f * heightCm - 5f * age + 5f).toInt()
            else -> (10f * weightKg + 6.25f * heightCm - 5f * age - 78f).toInt()
        }.coerceAtLeast(1000)

        // 2. Activity Multiplier based on selected activity
        val multiplier = when {
            activityType.contains("inactive", ignoreCase = true) || activityType.contains("zero", ignoreCase = true) -> 1.20f
            activityType.contains("Yoga", ignoreCase = true) || activityType.contains("Walking", ignoreCase = true) -> 1.35f
            activityType.contains("Dance", ignoreCase = true) || activityType.contains("Cycling", ignoreCase = true) -> 1.50f
            activityType.contains("Gym", ignoreCase = true) || activityType.contains("Running", ignoreCase = true) -> 1.55f
            activityType.contains("Sports", ignoreCase = true) || activityType.contains("Swimming", ignoreCase = true) -> 1.65f
            else -> 1.45f
        }

        val tdee = (bmr * multiplier).toInt()

        // 3. Goal Calorie Offset
        val calorieTarget = when {
            goal.contains("Fat loss", ignoreCase = true) -> (tdee - 400).coerceAtLeast(1300)
            goal.contains("Muscle gain", ignoreCase = true) -> (tdee + 300)
            goal.contains("Recomposition", ignoreCase = true) -> (tdee - 200).coerceAtLeast(1400)
            else -> tdee
        }

        // 4. Macro Targets
        val proteinMultiplier = when {
            goal.contains("Muscle gain", ignoreCase = true) || goal.contains("Recomposition", ignoreCase = true) -> 1.8f
            goal.contains("Fat loss", ignoreCase = true) -> 1.7f
            else -> 1.4f
        }
        val proteinGrams = (weightKg * proteinMultiplier).toInt().coerceIn(65, 200)

        val fatCalories = (calorieTarget * 0.25f).toInt()
        val fatGrams = (fatCalories / 9).coerceIn(40, 100)

        val remainingCaloriesForCarbs = (calorieTarget - (proteinGrams * 4) - (fatGrams * 9)).coerceAtLeast(400)
        val carbsGrams = (remainingCaloriesForCarbs / 4).coerceIn(60, 450)

        // 5. Water Target
        val waterLiters = when {
            weightKg > 85f -> 3.5f
            weightKg > 70f -> 3.0f
            weightKg > 55f -> 2.5f
            else -> 2.2f
        }

        val explanation = "Calculated using Mifflin-St Jeor formula (BMR: $bmr kcal, TDEE: $tdee kcal) tailored to your $goal focus and $activityType routine. Adjust anytime in Profile."

        return NutritionTargets(
            bmr = bmr,
            tdee = tdee,
            calorieTarget = calorieTarget,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            waterLiters = waterLiters,
            explanation = explanation
        )
    }
}
