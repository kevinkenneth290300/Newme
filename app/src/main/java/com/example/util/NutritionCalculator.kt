package com.example.util

import com.example.data.model.ActivityKind
import com.example.data.model.NutritionGoal
import com.example.data.model.ProfileSex

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
        goal: String,
        targetWeightKg: Float? = null
    ): NutritionTargets = calculateTargets(age, ProfileSex.fromStored(sex), heightCm, weightKg,
        ActivityKind.fromStored(activityType), NutritionGoal.fromStored(goal), targetWeightKg)

    fun calculateTargets(
        age: Int, sex: ProfileSex, heightCm: Float, weightKg: Float,
        activityType: ActivityKind, goal: NutritionGoal, targetWeightKg: Float? = null
    ): NutritionTargets {
        validateMetrics(age, heightCm, weightKg, targetWeightKg = targetWeightKg)
        // 1. Mifflin-St Jeor Formula for BMR
        val bmr = when (sex) {
            ProfileSex.FEMALE -> (10f * weightKg + 6.25f * heightCm - 5f * age - 161f).toInt()
            ProfileSex.MALE -> (10f * weightKg + 6.25f * heightCm - 5f * age + 5f).toInt()
            ProfileSex.OTHER -> (10f * weightKg + 6.25f * heightCm - 5f * age - 78f).toInt()
        }.coerceAtLeast(1000)

        // 2. Activity Multiplier based on selected activity
        val multiplier = activityType.multiplier

        val tdee = (bmr * multiplier).toInt()

        // 3. Goal Calorie Offset
        val effectiveGoal = if (goal == NutritionGoal.TARGET_WEIGHT) {
            require(targetWeightKg != null) { "Enter a valid target weight." }
            when {
                targetWeightKg < weightKg - 0.5f -> NutritionGoal.FAT_LOSS
                targetWeightKg > weightKg + 0.5f -> NutritionGoal.MUSCLE_GAIN
                else -> NutritionGoal.GENERAL_HEALTH
            }
        } else goal
        val calorieTarget = when (effectiveGoal) {
            NutritionGoal.FAT_LOSS -> (tdee - 400).coerceAtLeast(1300)
            NutritionGoal.MUSCLE_GAIN -> tdee + 300
            NutritionGoal.RECOMPOSITION -> (tdee - 200).coerceAtLeast(1400)
            else -> tdee
        }

        // 4. Macro Targets
        val proteinMultiplier = when (effectiveGoal) {
            NutritionGoal.MUSCLE_GAIN, NutritionGoal.RECOMPOSITION -> 1.8f
            NutritionGoal.FAT_LOSS -> 1.7f
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

        val explanation = "General estimates using Mifflin-St Jeor (BMR: $bmr kcal, TDEE: $tdee kcal), ${goal.label}, and ${activityType.label}. These are not measurements or medical advice; individual needs vary."

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

    fun validateMetrics(
        age: Int, heightCm: Float, weightKg: Float, targetWeightKg: Float? = null,
        bodyFatPercent: Float? = null, skeletalMuscleKg: Float? = null, waistCm: Float? = null,
        inBodyScore: Int? = null
    ) {
        require(age in 18..100) { "Enter an age from 18 to 100; nutrition estimates are intended for adults." }
        require(heightCm.isFinite() && heightCm in 100f..250f) { "Enter a height from 100 to 250 cm." }
        require(weightKg.isFinite() && weightKg in 30f..300f) { "Enter a weight from 30 to 300 kg." }
        require(targetWeightKg == null || (targetWeightKg.isFinite() && targetWeightKg in 30f..300f)) { "Enter a target weight from 30 to 300 kg." }
        require(bodyFatPercent == null || (bodyFatPercent.isFinite() && bodyFatPercent in 3f..70f)) { "Enter body fat from 3 to 70%, or leave it unknown." }
        require(skeletalMuscleKg == null || (skeletalMuscleKg.isFinite() && skeletalMuscleKg > 0f && skeletalMuscleKg <= weightKg)) { "Enter a positive muscle measurement no greater than body weight, or leave it unknown." }
        require(waistCm == null || (waistCm.isFinite() && waistCm in 40f..250f)) { "Enter a waist measurement from 40 to 250 cm, or leave it unknown." }
        require(inBodyScore == null || inBodyScore in 0..100) { "Enter a measurement score from 0 to 100, or leave it unknown." }
    }
}
