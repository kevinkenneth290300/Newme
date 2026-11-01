package com.example.data.activity

import android.location.Location
import android.os.SystemClock
import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.*
import com.example.data.model.ActivityKind
import com.example.data.repository.GamificationRepository
import com.example.data.repository.NewMeRepository
import com.example.util.DeviceClock
import java.time.Clock
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

class ActivityTrackingRepository(
    private val database: NewMeDatabase,
    private val clock: Clock = DeviceClock(),
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val dao = database.activityTrackingDao()
    val active = dao.observeActive()
    val history = dao.observeHistory()

    suspend fun start(kind: ActivityKind, gps: Boolean): ActivityTrackEntity = database.withTransaction {
        require(kind != ActivityKind.NONE) { "Choose an activity." }
        require(!gps || kind.outdoor) { "GPS is only available for outdoor walking, running, or cycling." }
        check(dao.getActive() == null) { "Finish or discard the active session before starting another." }
        check(database.userProfileDao().getUserProfileDirect()?.isOnboarded == true) { "Complete setup first." }
        val now = clock.millis()
        val track = ActivityTrackEntity(UUID.randomUUID().toString(), kind.name, now,
            lastElapsedRealtime = elapsedRealtime(), lastWallEpochMillis = now, gpsEnabled = gps)
        dao.insertTrack(track)
        dao.insertActive(ActiveTrackingEntity(sessionKey = track.sessionKey))
        NewMeRepository(database, clock).ensureDateInitialized(LocalDate.now(clock).toString())
        track
    }

    suspend fun tick() = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction
        if (track.status != TrackingPhase.RUNNING.name) return@withTransaction
        val elapsed = elapsedRealtime()
        val wall = clock.millis()
        val delta = TrackingRules.credibleElapsed(track.lastElapsedRealtime, elapsed)
        if (delta == null) {
            dao.updateTrack(track.copy(status = TrackingPhase.PAUSED.name, lastElapsedRealtime = elapsed,
                lastWallEpochMillis = wall, lastStepCounter = -1, routeSegment = track.routeSegment + 1,
                warning = "Tracking was interrupted. Saved time is kept; review and resume or log missing time manually."))
            return@withTransaction
        }
        val repository = NewMeRepository(database, clock)
        TrackingRules.splitDuration(wall, delta, clock.zone).forEach { (date, millis) ->
            repository.ensureDateInitialized(date)
            val day = dao.getDay(track.sessionKey, date) ?: TrackingDayTotalEntity(track.sessionKey, date)
            dao.saveDay(day.copy(elapsedMillis = day.elapsedMillis + millis))
        }
        dao.updateTrack(track.copy(elapsedMillis = track.elapsedMillis + delta, lastElapsedRealtime = elapsed, lastWallEpochMillis = wall))
    }

    suspend fun pause(reason: String = "") = database.withTransaction {
        tick()
        val track = dao.getActive() ?: return@withTransaction
        dao.updateTrack(track.copy(status = TrackingPhase.PAUSED.name, lastStepCounter = -1,
            routeSegment = track.routeSegment + 1, warning = reason.ifBlank { track.warning }))
    }

    suspend fun resume() = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction
        require(track.status == TrackingPhase.PAUSED.name) { "The session is already running." }
        dao.updateTrack(track.copy(status = TrackingPhase.RUNNING.name, lastElapsedRealtime = elapsedRealtime(),
            lastWallEpochMillis = clock.millis(), lastStepCounter = -1, routeSegment = track.routeSegment + 1, warning = ""))
    }

    suspend fun recoverInterrupted() = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction
        if (track.status == TrackingPhase.RUNNING.name) dao.updateTrack(track.copy(status = TrackingPhase.PAUSED.name,
            lastElapsedRealtime = elapsedRealtime(), lastWallEpochMillis = clock.millis(), lastStepCounter = -1,
            routeSegment = track.routeSegment + 1, warning = "Session interrupted. Saved progress is kept; resume explicitly or finish."))
    }

    suspend fun warning(message: String) = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction
        if (track.warning != message) dao.updateTrack(track.copy(warning = message))
    }

    suspend fun steps(reading: Double) = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction
        if (track.status != TrackingPhase.RUNNING.name || !ActivityKind.fromStored(track.activityType).countsSteps) return@withTransaction
        val change = TrackingRules.steps(track.lastStepCounter, reading) ?: return@withTransaction
        val total = (track.steps.toLong() + change.added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        dao.updateTrack(track.copy(lastStepCounter = change.baseline, steps = total))
        if (change.added > 0) {
            val date = LocalDate.now(clock).toString()
            val day = dao.getDay(track.sessionKey, date) ?: TrackingDayTotalEntity(track.sessionKey, date)
            dao.saveDay(day.copy(steps = (day.steps.toLong() + change.added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()))
        }
    }

    suspend fun point(location: Location): Boolean = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction false
        if (!track.gpsEnabled || track.status != TrackingPhase.RUNNING.name) return@withTransaction false
        if (!location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy !in 0f..50f ||
            !location.latitude.isFinite() || !location.longitude.isFinite() || location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) return@withTransaction false
        val now = clock.millis()
        if (location.time > now + 5000 || now - location.time > 30_000) return@withTransaction false
        val previous = dao.lastPoint(track.sessionKey)
        if (previous != null && location.time <= previous.epochMillis) return@withTransaction false
        val duration = previous?.let { location.time - it.epochMillis } ?: 0
        val connected = previous != null && previous.segment == track.routeSegment && duration in 1..30_000
        val distance = if (connected) {
            val result = FloatArray(1)
            Location.distanceBetween(previous!!.latitude, previous.longitude, location.latitude, location.longitude, result)
            result[0].toDouble()
        } else 0.0
        if (connected && distance / (duration / 1000.0) > 35.0) return@withTransaction false
        if (connected && distance < 3.0) return@withTransaction false
        val segment = if (previous != null && !connected && previous.segment == track.routeSegment) track.routeSegment + 1 else track.routeSegment
        dao.insertPoint(RoutePointEntity(sessionKey = track.sessionKey, epochMillis = location.time,
            latitude = location.latitude, longitude = location.longitude, accuracyMeters = location.accuracy, segment = segment))
        dao.updateTrack(track.copy(distanceMeters = track.distanceMeters + distance, routeSegment = segment))
        if (distance > 0) {
            val date = Instant.ofEpochMilli(location.time).atZone(clock.zone).toLocalDate().toString()
            val day = dao.getDay(track.sessionKey, date) ?: TrackingDayTotalEntity(track.sessionKey, date)
            dao.saveDay(day.copy(distanceMeters = day.distanceMeters + distance))
        }
        true
    }

    suspend fun finish(): ActivityTrackEntity? = database.withTransaction {
        tick()
        val track = dao.getActive() ?: return@withTransaction null
        val finished = track.copy(status = TrackingPhase.FINISHED.name, endEpochMillis = clock.millis(), lastStepCounter = -1)
        dao.updateTrack(finished)
        for (day in dao.getDays(track.sessionKey)) {
            val minutes = (day.elapsedMillis / 60_000).coerceAtMost(1440).toInt()
            if (minutes > 0) {
                val key = "${track.sessionKey}:${day.date}"
                val existing = database.activitySessionDao().getSessionByKey(key)
                database.activitySessionDao().insertActivitySession(ActivitySessionEntity(id = existing?.id ?: 0,
                    date = day.date, activityType = track.activityType, title = ActivityKind.fromStored(track.activityType).label,
                    durationMinutes = minutes, distanceKm = (day.distanceMeters / 1000.0).toFloat(), steps = day.steps,
                    source = "Phone timer", externalRecordId = key, isCompleted = true))
            }
        }
        dao.clearActive()
        val game = GamificationRepository(database, clock)
        if (track.elapsedMillis >= 60_000) game.reward("tracked-session:${track.sessionKey}", LocalDate.now(clock).toString(), 25)
        game.refresh()
        finished
    }

    suspend fun discard() = database.withTransaction {
        val track = dao.getActive() ?: return@withTransaction
        dao.clearActive()
        dao.deleteTrack(track.sessionKey)
    }

    suspend fun correctDistance(key: String, kilometers: Double) = database.withTransaction {
        require(kilometers.isFinite() && kilometers in 0.0..1000.0) { "Enter a distance between 0 and 1000 km." }
        val track = checkNotNull(dao.getTrack(key)) { "This tracked session no longer exists." }
        check(track.status == TrackingPhase.FINISHED.name && dao.getActive()?.sessionKey != key) { "Stop and save the session before correcting distance." }
        val meters = kilometers * 1000.0
        val days = dao.getDays(key)
        check(days.isNotEmpty() || meters == 0.0) { "This session has no saved time to associate with a distance." }
        val measured = days.sumOf { it.distanceMeters }
        val elapsed = days.sumOf { it.elapsedMillis }.toDouble()
        var remaining = meters
        days.forEachIndexed { index, day ->
            val weight = when {
                measured > 0 -> day.distanceMeters / measured
                elapsed > 0 -> day.elapsedMillis / elapsed
                else -> 1.0 / days.size
            }
            val distance = if (index == days.lastIndex) remaining else (meters * weight).coerceIn(0.0, remaining)
            remaining = (remaining - distance).coerceAtLeast(0.0)
            dao.saveDay(day.copy(distanceMeters = distance))
            database.activitySessionDao().getSessionByKey("$key:${day.date}")?.let { summary ->
                database.activitySessionDao().updateActivitySession(summary.copy(distanceKm = (distance / 1000.0).toFloat()))
            }
        }
        dao.updateTrack(track.copy(distanceMeters = meters))
    }

    suspend fun deleteRoute(key: String) = database.withTransaction {
        require(dao.getTrack(key)?.status != TrackingPhase.RUNNING.name) { "Pause or finish the session before deleting route points." }
        dao.deleteRoute(key)
        dao.getTrack(key)?.let { track -> dao.updateTrack(track.copy(routeSegment = track.routeSegment + 1)) }
    }

    suspend fun deleteFinished(key: String) = database.withTransaction {
        require(dao.getActive()?.sessionKey != key) { "Stop the active session before deleting it." }
        database.activitySessionDao().deleteTrackedSummaries("$key:%")
        dao.deleteTrack(key)
        GamificationRepository(database, clock).refresh()
    }
}