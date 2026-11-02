package com.example.data.repository

import com.example.R

enum class BadgeMetric { ACTIONS, GOALS, STREAK, COMEBACKS, PROTEIN, WATER, PREP, SLEEP, ACTIVITIES, ROUTES, ACTIVITY_TYPES, STEPS, DISTANCE, GYM_WEEKS, ROUTINE_DAYS, CHECKINS, PERFECT_QUESTS, EARLY_DAYS, LEVEL, FRESH_STARTS }
data class BadgeDefinition(val id: String, val title: Int, val hint: Int, val category: Int, val tier: Int, val metric: BadgeMetric, val target: Int)
data class BadgeValues(val values: Map<BadgeMetric, Int>) { fun current(badge: BadgeDefinition) = (values[badge.metric] ?: 0).coerceIn(0, badge.target) }

object BadgeCatalog {
    val all = listOf(
        BadgeDefinition("first_step", R.string.badge_first_step, R.string.hint_first_step, 0, 0, BadgeMetric.ACTIONS, 1),
        BadgeDefinition("first_day", R.string.badge_first_day, R.string.hint_first_day, 0, 0, BadgeMetric.GOALS, 1),
        BadgeDefinition("three_day_spark", R.string.badge_three_day, R.string.hint_three_day, 0, 0, BadgeMetric.STREAK, 3),
        BadgeDefinition("seven_day_momentum", R.string.badge_seven_day, R.string.hint_seven_day, 0, 1, BadgeMetric.STREAK, 7),
        BadgeDefinition("fourteen_day_blaze", R.string.badge_fourteen_day, R.string.hint_fourteen_day, 0, 2, BadgeMetric.STREAK, 14),
        BadgeDefinition("thirty_day_momentum", R.string.badge_thirty_day, R.string.hint_thirty_day, 0, 3, BadgeMetric.STREAK, 30),
        BadgeDefinition("centurion", R.string.badge_centurion, R.string.hint_centurion, 0, 3, BadgeMetric.STREAK, 100),
        BadgeDefinition("recovery_builder", R.string.badge_comeback, R.string.hint_comeback, 0, 0, BadgeMetric.COMEBACKS, 1),
        BadgeDefinition("protein_consistency", R.string.badge_protein, R.string.hint_protein, 1, 1, BadgeMetric.PROTEIN, 7),
        BadgeDefinition("hydration_hero", R.string.badge_hydration, R.string.hint_hydration, 1, 0, BadgeMetric.WATER, 7),
        BadgeDefinition("meal_prep_builder", R.string.badge_meal_prep, R.string.hint_meal_prep, 1, 1, BadgeMetric.PREP, 3),
        BadgeDefinition("sleep_champion", R.string.badge_sleep, R.string.hint_sleep, 1, 1, BadgeMetric.SLEEP, 7),
        BadgeDefinition("activity_builder", R.string.badge_activity, R.string.hint_activity, 2, 1, BadgeMetric.ACTIVITIES, 10),
        BadgeDefinition("trailblazer", R.string.badge_trailblazer, R.string.hint_trailblazer, 2, 0, BadgeMetric.ROUTES, 1),
        BadgeDefinition("all_rounder", R.string.badge_all_rounder, R.string.hint_all_rounder, 2, 2, BadgeMetric.ACTIVITY_TYPES, 3),
        BadgeDefinition("ten_k_steps", R.string.badge_steps, R.string.hint_steps, 2, 2, BadgeMetric.STEPS, 10000),
        BadgeDefinition("long_haul", R.string.badge_long_haul, R.string.hint_long_haul, 2, 2, BadgeMetric.DISTANCE, 50000),
        BadgeDefinition("gym_regular", R.string.badge_gym, R.string.hint_gym, 2, 2, BadgeMetric.GYM_WEEKS, 4),
        BadgeDefinition("tidy_streak", R.string.badge_tidy, R.string.hint_tidy, 3, 1, BadgeMetric.ROUTINE_DAYS, 7),
        BadgeDefinition("reflective", R.string.badge_reflective, R.string.hint_reflective, 3, 1, BadgeMetric.CHECKINS, 7),
        BadgeDefinition("perfect_day", R.string.badge_perfect, R.string.hint_perfect, 3, 2, BadgeMetric.PERFECT_QUESTS, 1),
        BadgeDefinition("early_bird", R.string.badge_early, R.string.hint_early, 3, 1, BadgeMetric.EARLY_DAYS, 5),
        BadgeDefinition("rank_elite", R.string.badge_elite, R.string.hint_elite, 4, 2, BadgeMetric.LEVEL, 5),
        BadgeDefinition("fresh_start", R.string.badge_fresh, R.string.hint_fresh, 4, 0, BadgeMetric.FRESH_STARTS, 1)
    )
    fun rank(level: Int): Int = listOf(R.string.rank_rookie, R.string.rank_trainee, R.string.rank_challenger, R.string.rank_contender,
        R.string.rank_elite, R.string.rank_veteran, R.string.rank_master, R.string.rank_champion, R.string.rank_legend, R.string.rank_mythic)[(level - 1).coerceIn(0, 9)]
}