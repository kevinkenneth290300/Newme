package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "activity_tracks")
data class ActivityTrackEntity(
    @PrimaryKey val sessionKey: String,
    val activityType: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long? = null,
    val status: String = "RUNNING",
    val elapsedMillis: Long = 0,
    val lastElapsedRealtime: Long,
    val lastWallEpochMillis: Long,
    val lastStepCounter: Long = -1,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val gpsEnabled: Boolean = false,
    val routeSegment: Int = 0,
    val warning: String = ""
)

@Entity(tableName = "active_tracking", foreignKeys = [ForeignKey(entity = ActivityTrackEntity::class,
    parentColumns = ["sessionKey"], childColumns = ["sessionKey"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionKey")])
data class ActiveTrackingEntity(@PrimaryKey val id: Int = 1, val sessionKey: String)

@Entity(tableName = "tracking_day_totals", primaryKeys = ["sessionKey", "date"],
    foreignKeys = [ForeignKey(entity = ActivityTrackEntity::class, parentColumns = ["sessionKey"],
        childColumns = ["sessionKey"], onDelete = ForeignKey.CASCADE)], indices = [Index("date")])
data class TrackingDayTotalEntity(val sessionKey: String, val date: String, val elapsedMillis: Long = 0,
    val steps: Int = 0, val distanceMeters: Double = 0.0)

@Entity(tableName = "route_points", foreignKeys = [ForeignKey(entity = ActivityTrackEntity::class,
    parentColumns = ["sessionKey"], childColumns = ["sessionKey"], onDelete = ForeignKey.CASCADE)], indices = [Index("sessionKey")])
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionKey: String,
    val epochMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val segment: Int
)