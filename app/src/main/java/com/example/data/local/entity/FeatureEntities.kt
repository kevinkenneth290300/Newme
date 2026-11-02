package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

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
    val legacyRewardCutoffDate: String = "",
    @ColumnInfo(defaultValue = "'ROUTINES,MEALS,GYM,ACTIVITY,WATER,SLEEP,CYCLE'") val enabledModules: String = "ROUTINES,MEALS,GYM,ACTIVITY,WATER,SLEEP,CYCLE",
    @ColumnInfo(defaultValue = "0") val dashboardSetupDone: Boolean = false,
    @ColumnInfo(defaultValue = "1") val momentsEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0") val hapticsEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "''") val pinnedBadges: String = "",
    @ColumnInfo(defaultValue = "''") val starterPacks: String = "",
    @ColumnInfo(defaultValue = "'sleep,mood,focus,energy,hydrate,win,profile,comeback'") val momentTypes: String = "sleep,mood,focus,energy,hydrate,win,profile,comeback",
    @ColumnInfo(defaultValue = "0") val cycleCheckinsEnabled: Boolean = false
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

@Entity(tableName = "reminders", foreignKeys = [ForeignKey(
    entity = DailyChecklistItemEntity::class, parentColumns = ["id"], childColumns = ["taskId"],
    onDelete = ForeignKey.CASCADE
), ForeignKey(entity = RoutineTemplateEntity::class, parentColumns = ["id"], childColumns = ["routineId"],
    onDelete = ForeignKey.CASCADE)], indices = [Index("taskId"), Index("routineId")])
data class ReminderEntity(
    @PrimaryKey val key: String,
    val kind: String,
    val taskId: Long? = null,
    val minuteOfDay: Int = 540,
    val enabled: Boolean = false,
    val timeSensitive: Boolean = false,
    val lastDeliveredToken: String = "",
    @ColumnInfo(defaultValue = "NULL") val routineId: Long? = null
)