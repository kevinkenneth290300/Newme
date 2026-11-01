package com.example.data.activity

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NewMeDatabase
import com.example.data.model.ActivityKind
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ActivityTrackingService : Service(), SensorEventListener, LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, _ ->
        _error.value = "Tracking was interrupted. Saved progress is kept; review and resume explicitly."
        _commandPending.value = false
        shuttingDown = true
        if (::sensors.isInitialized && ::locations.isInitialized) stopInputs()
        stopSelf()
    })
    private val processing = Mutex()
    private lateinit var repository: ActivityTrackingRepository
    private lateinit var sensors: SensorManager
    private lateinit var locations: LocationManager
    private var ticker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var shuttingDown = false

    override fun onCreate() {
        super.onCreate()
        repository = ActivityTrackingRepository(NewMeDatabase.getInstance(this))
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        locations = getSystemService(LOCATION_SERVICE) as LocationManager
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: STOP
        val kind = ActivityKind.entries.firstOrNull { it.name == intent?.getStringExtra(KIND) } ?: ActivityKind.OTHER
        val gpsRequested = intent?.getBooleanExtra(GPS, false) == true
        val gpsAllowed = gpsRequested && canUseGps(this)
        try {
            val type = foregroundTypes(kind, gpsAllowed)
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        } catch (failure: Exception) {
            _error.value = "Android could not start tracking. Use a manual log or retry while NewMe is open."
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch {
            processing.withLock {
                try {
                    when (action) {
                        START -> {
                            val existing = NewMeDatabase.getInstance(this@ActivityTrackingService).activityTrackingDao().getActive()
                            if (existing != null) { _commandPending.value = false; return@withLock }
                            repository.start(kind, gpsAllowed)
                            if (gpsRequested && !gpsAllowed) repository.warning("Precise GPS or location services unavailable. Timer/manual distance remains available.")
                            beginInputs(kind, gpsAllowed)
                        }
                        RESUME -> {
                            repository.resume()
                            val track = NewMeDatabase.getInstance(this@ActivityTrackingService).activityTrackingDao().getActive()
                            if (track != null) beginInputs(ActivityKind.fromStored(track.activityType), track.gpsEnabled && canUseGps(this@ActivityTrackingService))
                        }
                        PAUSE -> { stopInputs(); repository.pause(); stopSelf() }
                        STOP -> { stopInputs(); repository.finish(); stopSelf() }
                        DISCARD -> { stopInputs(); repository.discard(); stopSelf() }
                    }
                    _commandPending.value = false
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    _error.value = "Tracking could not continue. Saved progress is kept; review the session or use a manual log."
                    _commandPending.value = false
                    stopInputs()
                    repository.recoverInterrupted()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun foregroundTypes(kind: ActivityKind, gps: Boolean): Int {
        var type = 0
        if (Build.VERSION.SDK_INT >= 34) {
            type = if (kind.countsSteps && hasStepAccess(this)) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                else ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        if (Build.VERSION.SDK_INT >= 29 && gps) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        return type
    }

    @android.annotation.SuppressLint("MissingPermission", "WakelockTimeout")
    private suspend fun beginInputs(kind: ActivityKind, gps: Boolean) {
        stopInputs()
        shuttingDown = false
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (kind.countsSteps) {
            if (sensor != null && hasStepAccess(this)) {
                if (!sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)) repository.warning("Step sensor unavailable. Use the timer or enter steps manually.")
            } else repository.warning("Step sensor missing or permission denied. Timer/manual logging remains available.")
        }
        if (gps) {
            try {
                locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 3f, this, Looper.getMainLooper())
            } catch (failure: SecurityException) {
                repository.warning("GPS permission unavailable. Timer/manual distance remains available.")
            } catch (failure: IllegalArgumentException) {
                repository.warning("GPS provider unavailable. Timer/manual distance remains available.")
            }
        }
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NewMe:ActiveTracking").apply {
            setReferenceCounted(false)
            acquire(15 * 60 * 1000L)
        }
        ticker = scope.launch {
            var renewal = 0
            while (isActive && !shuttingDown) {
                delay(5000)
                processing.withLock {
                    repository.tick()
                    val active = NewMeDatabase.getInstance(this@ActivityTrackingService).activityTrackingDao().getActive()
                    if (active?.status != TrackingPhase.RUNNING.name) { stopInputs(); stopSelf(); return@launch }
                    if (++renewal >= 12) { wakeLock?.acquire(15 * 60 * 1000L); renewal = 0 }
                }
            }
        }
    }

    private fun stopInputs() {
        sensors.unregisterListener(this)
        try { locations.removeUpdates(this) } catch (failure: SecurityException) { Unit }
        ticker?.cancel()
        ticker = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (shuttingDown || event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val reading = event.values.firstOrNull()?.toDouble() ?: return
        scope.launch { processing.withLock { if (!shuttingDown) repository.steps(reading) } }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onLocationChanged(location: Location) {
        if (shuttingDown) return
        val copy = Location(location)
        scope.launch { processing.withLock { if (!shuttingDown) repository.point(copy) } }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER && !shuttingDown) scope.launch { repository.warning("GPS signal unavailable. Timer continues; manual distance can be logged.") }
    }

    override fun onProviderEnabled(provider: String) = Unit

    @Deprecated("Legacy Android callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

    private fun notification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Active Activity", NotificationManager.IMPORTANCE_LOW)
        )
        val content = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pause = PendingIntent.getService(this, 1, Intent(this, ActivityTrackingService::class.java).setAction(PAUSE), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 2, Intent(this, ActivityTrackingService::class.java).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_tracking)
            .setContentTitle("NewMe activity in progress").setContentText("User-started tracking is active")
            .setContentIntent(content).setOngoing(true).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Pause", pause).addAction(0, "Stop", stop).build()
    }

    private suspend fun quiesce() {
        shuttingDown = true
        stopInputs()
        processing.withLock { Unit }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        shuttingDown = true
        stopInputs()
        scope.cancel()
        _commandPending.value = false
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "newme_active_tracking"
        private const val NOTIFICATION_ID = 4001
        const val START = "com.example.tracking.START"
        const val RESUME = "com.example.tracking.RESUME"
        const val PAUSE = "com.example.tracking.PAUSE"
        const val STOP = "com.example.tracking.STOP"
        const val DISCARD = "com.example.tracking.DISCARD"
        private const val KIND = "activity_kind"
        private const val GPS = "gps_enabled"
        @Volatile private var instance: ActivityTrackingService? = null
        val isRunning: Boolean get() = instance != null
        private val _error = MutableStateFlow<String?>(null)
        val errors = _error.asStateFlow()
        private val _commandPending = MutableStateFlow(false)
        val commandPending = _commandPending.asStateFlow()
        fun clearError() { _error.value = null }

        fun hasStepAccess(context: Context): Boolean = Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

        fun canUseGps(context: Context): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)

        fun command(context: Context, action: String, kind: ActivityKind = ActivityKind.OTHER, gps: Boolean = false) {
            if (_commandPending.value) return
            _commandPending.value = true
            val intent = Intent(context, ActivityTrackingService::class.java).setAction(action).putExtra(KIND, kind.name).putExtra(GPS, gps)
            try { ContextCompat.startForegroundService(context, intent) }
            catch (failure: Exception) { _commandPending.value = false; throw failure }
        }

        suspend fun flushPending() { instance?.let { service -> service.processing.withLock { service.repository.tick() } } }
        suspend fun stopForMaintenance() { instance?.let { service ->
            service.quiesce()
            service.repository.recoverInterrupted()
        } }
    }
}