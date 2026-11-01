package com.example.data.repository

import com.example.data.local.entity.MealPlanEntity
import com.example.data.local.entity.UserProfileEntity

object MealSuggestions {
    fun forDate(profile: UserProfileEntity, date: String): List<MealPlanEntity> {
        if (!profile.usesRecommendedPlan) return emptyList()
        val vegan = profile.dietaryPreference == "Vegan"
        val eggs = profile.dietaryPreference in setOf("Eggetarian", "Non-vegetarian")
        val meat = profile.dietaryPreference == "Non-vegetarian"
        val breakfast = if (eggs) "Eggs, oats & fruit" else "Moong chilla & fruit"
        val breakfastFoods = if (eggs) "2 eggs, 40g oats and 1 piece of fruit" else "2 moong chillas with vegetables and 1 piece of fruit"
        val lunchProtein = if (meat) "Chicken" else if (vegan) "Tofu & dal" else "Dal & curd"
        val dinnerProtein = if (meat) "Fish" else if (vegan) "Tofu" else "Paneer"
        return listOf(
            MealPlanEntity(date = date, slot = "Morning", name = breakfast, time = "08:00 AM", foodsDescription = breakfastFoods,
                approxCalories = 380, proteinGrams = if (eggs) 22 else 18, carbsGrams = 48, fatsGrams = 12, source = "Suggested Plan"),
            MealPlanEntity(date = date, slot = "Lunch", name = "$lunchProtein, rice & vegetables", time = "01:00 PM",
                foodsDescription = "1 protein portion, 100g cooked rice and 200g seasonal vegetables; nutrition is approximate",
                approxCalories = 450, proteinGrams = if (meat) 35 else 24, carbsGrams = 55, fatsGrams = 14, source = "Suggested Plan"),
            MealPlanEntity(date = date, slot = "Dinner", name = "$dinnerProtein, roti & vegetables", time = "08:00 PM",
                foodsDescription = "150g $dinnerProtein, 1 roti and 200g vegetables; adjust portions to your own needs",
                approxCalories = 440, proteinGrams = if (meat) 32 else 24, carbsGrams = 40, fatsGrams = 18, source = "Suggested Plan")
        )
    }
}