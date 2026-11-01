package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "SYSTEM",
    val reducedMotion: Boolean = false,
    val dailyGoalType: String = "TASKS",
    val dailyGoalTarget: Int = 3,
    val goalChosen: Boolean = false,
    val cycleEnabled: Boolean = false,
    val cycleRemindersEnabled: Boolean = false,
    val taskRemindersEnabled: Boolean = false,
    val quietStartMinutes: Int = 1320,
    val quietEndMinutes: Int = 420,
    val legacyRewardCutoffDate: String = ""
)

@Entity(tableName = "daily_state")
data class DailyStateEntity(
    @PrimaryKey val date: String,
    val goalType: String,
    val goalTarget: Int,
    val goalEnabled: Boolean,
    val calorieTarget: Int,
    val proteinTargetGrams: Int,
    val waterTargetMl: Int,
    val sleepTargetMinutes: Int,
    val targetsKnown: Boolean = true,
    val activityEnabled: Boolean = false
)

@Entity(tableName = "reward_ledger")
data class RewardEventEntity(
    @PrimaryKey val eventKey: String,
    val date: String,
    val xp: Int,
    val createdAt: Long
)

@Entity(tableName = "legacy_reward_exclusions")
data class LegacyRewardExclusionEntity(@PrimaryKey val eventKey: String)

@Entity(tableName = "cycle_entries", indices = [Index(value = ["startDate"], unique = true)])
data class CycleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,
    val endDate: String? = null,
    val symptoms: String = ""
)