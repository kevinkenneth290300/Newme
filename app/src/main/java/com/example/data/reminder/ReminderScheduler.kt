package com.example.data.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.NewMeApplication
import com.example.R
import com.example.data.local.NewMeDatabase
import com.example.util.DeviceClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.time.Clock
import java.time.ZonedDateTime

enum class ReminderTiming { BLOCKED, APPROXIMATE, EXACT }

class ReminderScheduler(
    private val context: Context,
    private val database: NewMeDatabase,
    private val clock: Clock = DeviceClock()
) {
    private val repository = ReminderRepository(database, clock)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val preferences = context.getSharedPreferences("reminder_scheduler", Context.MODE_PRIVATE)
    val failure = MutableStateFlow(preferences.getString("failure", null))

    @OptIn(FlowPreview::class)
    fun observe(scope: CoroutineScope) = scope.launch {
        database.invalidationTracker.createFlow("reminders", "app_settings", "cycle_entries", "daily_checklist", "user_profile")
            .debounce(250).collect {
                try { reconcile() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { reportFailure() }
            }
    }

    suspend fun reconcile() = mutex.withLock { reconcileLocked() }

    private suspend fun reconcileLocked() {
        channels()
        val records = database.reminderDao().getAll()
        val keys = records.map { it.key }.toSet()
        val previous = preferences.getStringSet("keys", emptySet()).orEmpty().toSet()
        val settings = database.settingsDao().get()
        val onboarded = database.userProfileDao().getUserProfileDirect()?.isOnboarded == true
        val plan = if (onboarded) repository.plan() else emptyList()
        val desired = plan.filter { timing(context, it.timeSensitive, it.key) != ReminderTiming.BLOCKED }.associateBy { it.key }
        for (key in previous + keys) {
            alarms.cancel(pending(key))
            val record = records.firstOrNull { it.key == key }
            val consent = onboarded && record?.enabled == true && when (record.kind) {
                "CYCLE" -> settings?.cycleEnabled == true && settings.cycleRemindersEnabled
                "DAILY" -> settings?.taskRemindersEnabled == true
                "TASK" -> settings?.taskRemindersEnabled == true && record.taskId?.let {
                    database.dailyChecklistDao().getItem(it)?.isCompleted == false
                } == true
                else -> false
            }
            if (!consent || timing(context, false, key) == ReminderTiming.BLOCKED) notifications.cancel(notificationTag(key), NOTIFICATION_ID)
        }
        for (occurrence in desired.values) {
            val allowedNow = settings?.let {
                ReminderRules.afterQuietHours(ZonedDateTime.now(clock), it.quietStartMinutes, it.quietEndMinutes)
                    .toInstant().toEpochMilli()
            } ?: clock.millis()
            val time = maxOf(occurrence.epochMillis, allowedNow, clock.millis() + 1000)
            val operation = pending(occurrence.key, occurrence.token)
            if (timing(context, occurrence.timeSensitive, occurrence.key) == ReminderTiming.EXACT) {
                try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation) }
                catch (denied: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation) }
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, operation)
            }
        }
        check(preferences.edit().putStringSet("keys", keys).remove("failure").commit()) { "Reminder scheduling could not be saved." }
        failure.value = null
    }

    suspend fun deliver(key: String, token: String) = mutex.withLock {
        channels()
        if (timing(context, false, key) != ReminderTiming.BLOCKED && repository.claim(key, token)) {
            notifications.notify(notificationTag(key), NOTIFICATION_ID, notification(key))
        }
        reconcileLocked()
    }

    fun notification(key: String): Notification {
        val discreet = key == ReminderRules.CYCLE
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = NotificationCompat.Builder(context, channel(key)).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NewMe").setContentText("A private reminder is ready.").build()
        return NotificationCompat.Builder(context, channel(key)).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NewMe")
            .setContentText(if (discreet) "A private check-in is ready when you are." else "Your planned tasks are ready when you are.")
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public).build()
    }

    fun reportFailure() {
        val message = "Reminders could not be updated. Open reminder settings and retry."
        preferences.edit().putString("failure", message).apply()
        failure.value = message
    }

    private fun channels() {
        if (Build.VERSION.SDK_INT >= 26) {
            notifications.createNotificationChannel(NotificationChannel(TASK_CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT))
            notifications.createNotificationChannel(NotificationChannel(PRIVATE_CHANNEL, "Private reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            })
        }
    }

    private fun pending(key: String, token: String = ""): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(FIRE)
            .setData(Uri.Builder().scheme("newme-reminder").authority("local").appendPath(key).build())
            .putExtra("key", key).putExtra("token", token)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val FIRE = "com.aistudio.newme.trkfit.REMINDER"
        const val TASK_CHANNEL = "newme_tasks"
        const val PRIVATE_CHANNEL = "newme_private"
        private const val NOTIFICATION_ID = 2002
        private val mutex = Mutex()

        fun from(context: Context): ReminderScheduler = (context.applicationContext as NewMeApplication).reminders
        private fun channel(key: String) = if (key == ReminderRules.CYCLE) PRIVATE_CHANNEL else TASK_CHANNEL
        private fun notificationTag(key: String) = "newme-reminder-${key.hashCode()}"

        fun timing(context: Context, timeSensitive: Boolean, key: String = ReminderRules.DAILY): ReminderTiming {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return ReminderTiming.BLOCKED
            val manager = context.getSystemService(NotificationManager::class.java)
            if (!manager.areNotificationsEnabled()) return ReminderTiming.BLOCKED
            if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(channel(key))?.importance == NotificationManager.IMPORTANCE_NONE) return ReminderTiming.BLOCKED
            val alarm = context.getSystemService(AlarmManager::class.java)
            return if (timeSensitive && key != ReminderRules.CYCLE && (Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms())) ReminderTiming.EXACT else ReminderTiming.APPROXIMATE
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ReminderScheduler.FIRE, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = ReminderScheduler.from(context)
            try {
                withTimeout(8000) {
                    if (intent.action == ReminderScheduler.FIRE) {
                        val key = intent.getStringExtra("key") ?: return@withTimeout
                        val token = intent.getStringExtra("token") ?: return@withTimeout
                        scheduler.deliver(key, token)
                    } else scheduler.reconcile()
                }
            } catch (error: Exception) {
                scheduler.reportFailure()
            } finally {
                pending.finish()
            }
        }
    }
}