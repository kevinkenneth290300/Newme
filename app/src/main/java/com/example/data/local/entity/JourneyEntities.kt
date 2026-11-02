package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routine_templates")
data class RoutineTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "MORNING",
    val iconKey: String = "sun",
    val recurrence: String = "DAILY",
    val daysOfWeekMask: Int = 127,
    val dayOfMonth: Int = 1,
    val startDate: String,
    val sortOrder: Int = 0,
    val source: String = "USER",
    val isActive: Boolean = true,
    val createdAt: Long = 0
)

@Entity(tableName = "routine_completions", primaryKeys = ["templateId", "date"], foreignKeys = [ForeignKey(
    entity = RoutineTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("date")])
data class RoutineCompletionEntity(val templateId: Long, val date: String, val completedAt: Long)

@Entity(tableName = "day_summary")
data class DaySummaryEntity(
    @PrimaryKey val date: String,
    val goalMet: Boolean,
    val tasksDone: Int,
    val tasksTotal: Int,
    val activityMinutes: Int,
    val xpEarned: Int,
    val updatedAt: Long,
    val proteinMet: Boolean = false,
    val waterMet: Boolean = false,
    val sleepMet: Boolean = false,
    val workoutDone: Boolean = false,
    val routineDone: Int = 0,
    val routineTotal: Int = 0,
    val checkins: Int = 0,
    val earlyCheckin: Boolean = false,
    val trackedSteps: Int = 0,
    val trackedMeters: Double = 0.0,
    val sleepMinutes: Int = 0,
    val mood: String = ""
)

@Entity(tableName = "summary_dirty")
data class SummaryDirtyEntity(@PrimaryKey val date: String)

@Entity(tableName = "moment_log")
data class MomentLogEntity(
    @PrimaryKey val momentKey: String,
    val date: String,
    val part: String,
    val type: String,
    val status: String = "SHOWING",
    val snoozeCount: Int = 0,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "'{}'") val answers: String = "{}"
)