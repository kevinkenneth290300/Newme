package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTrackingDao {
    @Query("SELECT activity_tracks.* FROM activity_tracks INNER JOIN active_tracking ON activity_tracks.sessionKey = active_tracking.sessionKey WHERE active_tracking.id = 1")
    fun observeActive(): Flow<ActivityTrackEntity?>

    @Query("SELECT activity_tracks.* FROM activity_tracks INNER JOIN active_tracking ON activity_tracks.sessionKey = active_tracking.sessionKey WHERE active_tracking.id = 1")
    suspend fun getActive(): ActivityTrackEntity?

    @Query("SELECT * FROM activity_tracks WHERE sessionKey = :key")
    suspend fun getTrack(key: String): ActivityTrackEntity?

    @Query("SELECT * FROM activity_tracks WHERE status = 'FINISHED' ORDER BY startEpochMillis DESC")
    fun observeHistory(): Flow<List<ActivityTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrack(track: ActivityTrackEntity)

    @Update
    suspend fun updateTrack(track: ActivityTrackEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActive(active: ActiveTrackingEntity)

    @Query("DELETE FROM active_tracking WHERE id = 1")
    suspend fun clearActive()

    @Query("SELECT * FROM tracking_day_totals WHERE sessionKey = :key AND date = :date")
    suspend fun getDay(key: String, date: String): TrackingDayTotalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDay(total: TrackingDayTotalEntity)

    @Query("SELECT * FROM tracking_day_totals WHERE sessionKey = :key ORDER BY date")
    suspend fun getDays(key: String): List<TrackingDayTotalEntity>

    @Query("SELECT COALESCE(SUM(elapsedMillis), 0) FROM tracking_day_totals WHERE date = :date")
    fun observeMillis(date: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(elapsedMillis), 0) FROM tracking_day_totals WHERE date = :date")
    suspend fun getMillis(date: String): Long

    @Insert
    suspend fun insertPoint(point: RoutePointEntity)

    @Query("SELECT * FROM route_points WHERE sessionKey = :key ORDER BY epochMillis DESC, id DESC LIMIT 1")
    suspend fun lastPoint(key: String): RoutePointEntity?

    @Query("SELECT * FROM route_points WHERE sessionKey = :key ORDER BY epochMillis, id")
    fun observeRoute(key: String): Flow<List<RoutePointEntity>>

    @Query("DELETE FROM route_points WHERE sessionKey = :key")
    suspend fun deleteRoute(key: String)

    @Query("DELETE FROM activity_tracks WHERE sessionKey = :key")
    suspend fun deleteTrack(key: String)
}