package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DailyChecklistItemEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.reminder.ReminderRules
import com.example.data.reminder.ReminderScheduler
import com.example.data.reminder.ReminderTiming
import com.example.viewmodel.NewMeViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private fun timeLabel(minute: Int) = LocalTime.ofSecondOfDay(minute * 60L).format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun TimeControl(label: String, minute: Int, enabled: Boolean = true, onChanged: (Int) -> Unit) {
    val context = LocalContext.current
    val style = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) android.R.style.Theme_Material_Dialog_Alert
        else android.R.style.Theme_Material_Light_Dialog_Alert
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ${timeLabel(minute)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(enabled = enabled, onClick = {
            android.app.TimePickerDialog(context, style, { _, hour, minutes -> onChanged(hour * 60 + minutes) },
                minute / 60, minute % 60, true).show()
        }) { Icon(Icons.Default.Schedule, "$label time") }
    }
}

@Composable
internal fun notificationConsent(viewModel: NewMeViewModel): ((() -> Unit) -> Unit) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.moments.permissionOpen.value = false
        if (granted) pending?.invoke() else viewModel.reminderPermissionDenied()
        pending = null
    }
    return { action ->
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pending = action
            viewModel.moments.permissionOpen.value = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else action()
    }
}

@Composable
private fun timing(sensitive: Boolean, key: String): ReminderTiming {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return remember(sensitive, key, revision) { ReminderScheduler.timing(context, sensitive, key) }
}

@Composable
private fun Toggle(label: String, checked: Boolean, enabled: Boolean = true, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked, onChanged, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun TimingStatus(sensitive: Boolean, key: String) {
    val context = LocalContext.current
    val access = timing(sensitive, key)
    Text(when (access) {
        ReminderTiming.BLOCKED -> "Notifications are blocked in Android."
        ReminderTiming.APPROXIMATE -> if (sensitive) "Exact access is off. Approximate timing will be used." else "Approximate timing. Android may delay delivery."
        ReminderTiming.EXACT -> "Exact timing is allowed."
    }, style = MaterialTheme.typography.bodySmall)
    if (access == ReminderTiming.BLOCKED) {
        TextButton(onClick = {
            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
        }) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text("Android Notifications") }
    }
    if (sensitive && access != ReminderTiming.EXACT && Build.VERSION.SDK_INT >= 31) {
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text("Exact Timing Access") }
    }
}

@Composable
fun ReminderControls(viewModel: NewMeViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val schedules by viewModel.reminders.collectAsStateWithLifecycle()
    val error by viewModel.reminderError.collectAsStateWithLifecycle()
    val consent = notificationConsent(viewModel)
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }
    var quiet by rememberSaveable { mutableStateOf(false) }
    val daily = schedules.firstOrNull { it.key == ReminderRules.DAILY } ?: ReminderEntity("daily", "DAILY", enabled = true)
    val cycle = schedules.firstOrNull { it.key == ReminderRules.CYCLE } ?: ReminderEntity("cycle", "CYCLE")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        Toggle("Task notifications", settings?.taskRemindersEnabled == true) { enabled ->
            if (enabled) consent { viewModel.setTaskReminders(true) } else viewModel.setTaskReminders(false)
        }
        if (settings?.taskRemindersEnabled == true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily check-in: ${timeLabel(daily.minuteOfDay)}", modifier = Modifier.weight(1f))
                Switch(daily.enabled, { viewModel.saveReminder(daily.key, daily.minuteOfDay, it, daily.timeSensitive) },
                    modifier = Modifier.semantics { contentDescription = "Daily task check-in" })
                IconButton(onClick = { editing = daily }) { Icon(Icons.Default.Edit, "Edit daily reminder") }
            }
            TimingStatus(daily.timeSensitive, daily.key)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quiet hours: ${timeLabel(settings?.quietStartMinutes ?: 1320)} to ${timeLabel(settings?.quietEndMinutes ?: 420)}",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { quiet = true }) { Icon(Icons.Default.Edit, "Edit quiet hours") }
        }
        if (settings?.cycleEnabled == true) {
            Toggle("Private cycle reminders", settings?.cycleRemindersEnabled == true) { enabled ->
                if (enabled) consent { viewModel.setCycleReminders(true) } else viewModel.setCycleReminders(false)
            }
            if (settings?.cycleRemindersEnabled == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Private check-in: ${timeLabel(cycle.minuteOfDay)}", modifier = Modifier.weight(1f))
                    IconButton(onClick = { editing = cycle }) { Icon(Icons.Default.Edit, "Edit private reminder") }
                }
                TimingStatus(false, cycle.key)
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = viewModel::refreshReminders) { Text("Retry") }
        }
        HorizontalDivider()
    }
    editing?.let { reminder -> key(reminder.key) { ReminderDialog(viewModel, reminder, null) { editing = null } } }
    if (quiet) {
        var start by rememberSaveable { mutableIntStateOf(settings?.quietStartMinutes ?: 1320) }
        var end by rememberSaveable { mutableIntStateOf(settings?.quietEndMinutes ?: 420) }
        AlertDialog(onDismissRequest = { quiet = false }, title = { Text("Quiet Hours") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeControl("Start", start) { start = it }
                TimeControl("End", end) { end = it }
                Text("Matching times turn quiet hours off.", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = {
            viewModel.setQuietHours(start, end) { quiet = false }
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { quiet = false }) { Text("Cancel") } })
    }
}

@Composable
fun TaskReminderDialog(viewModel: NewMeViewModel, task: DailyChecklistItemEntity, onDismiss: () -> Unit) {
    val schedules by viewModel.reminders.collectAsStateWithLifecycle()
    val reminder = schedules.firstOrNull { it.taskId == task.id }
        ?: ReminderEntity("task:${task.id}", "TASK", taskId = task.id)
    key(task.id) { ReminderDialog(viewModel, reminder, task, onDismiss) }
}

@Composable
private fun ReminderDialog(viewModel: NewMeViewModel, reminder: ReminderEntity, task: DailyChecklistItemEntity?, onDismiss: () -> Unit) {
    val saving by viewModel.savingReminder.collectAsStateWithLifecycle()
    val consent = notificationConsent(viewModel)
    var minute by rememberSaveable { mutableIntStateOf(reminder.minuteOfDay) }
    var enabled by rememberSaveable { mutableStateOf(reminder.enabled) }
    var sensitive by rememberSaveable { mutableStateOf(reminder.timeSensitive) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Reminder") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            task?.let { Text(it.title, style = MaterialTheme.typography.bodyMedium); Text(it.date, style = MaterialTheme.typography.bodySmall) }
            Toggle("Enabled", enabled, !saving) { enabled = it }
            TimeControl("Local time", minute, !saving) { minute = it }
            if (reminder.kind != "CYCLE") Toggle("Time-sensitive", sensitive, !saving) { sensitive = it }
            TimingStatus(sensitive, reminder.key)
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = {
        val save = { viewModel.saveReminder(reminder.key, minute, enabled, sensitive, reminder.taskId, onDismiss) }
        if (enabled) consent(save) else save()
    }) { Text(if (saving) "Saving..." else "Save") } }, dismissButton = {
        TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
    })
}