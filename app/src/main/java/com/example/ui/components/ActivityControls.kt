package com.example.ui.components

import android.Manifest
import android.os.Build
import android.hardware.Sensor
import android.hardware.SensorManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.activity.ActivityTrackingService
import com.example.data.local.entity.RoutePointEntity
import com.example.data.local.entity.ActivityTrackEntity
import com.example.data.model.ActivityKind
import com.example.viewmodel.NewMeViewModel
import kotlin.math.cos

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityControls(viewModel: NewMeViewModel) {
    val context = LocalContext.current
    val active by viewModel.activeTracking.collectAsStateWithLifecycle()
    val history by viewModel.trackedHistory.collectAsStateWithLifecycle()
    val busy by viewModel.trackingBusy.collectAsStateWithLifecycle()
    val savingManual by viewModel.savingActivityLog.collectAsStateWithLifecycle()
    val savingDistance by viewModel.savingActivityDistance.collectAsStateWithLifecycle()
    var correcting by remember { mutableStateOf<ActivityTrackEntity?>(null) }
    var kindName by rememberSaveable { mutableStateOf(ActivityKind.WALKING.name) }
    var gps by rememberSaveable { mutableStateOf(false) }
    var permissionMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var routeKey by remember { mutableStateOf<String?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val kind = ActivityKind.valueOf(kindName)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        viewModel.moments.permissionOpen.value = false
        val fine = ActivityTrackingService.canUseGps(context)
        if (gps && !fine) permissionMessage = "Precise GPS was not granted or is unavailable. Starting timer only; distance can be entered manually."
        if (kind.countsSteps && !ActivityTrackingService.hasStepAccess(context)) permissionMessage = "Step permission denied. Timer/manual logging remains available."
        viewModel.startActivity(kind, gps && fine)
    }
    fun start() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val sensor = (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (kind.countsSteps && sensor != null && Build.VERSION.SDK_INT >= 29 && !ActivityTrackingService.hasStepAccess(context)) permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (gps && kind.outdoor && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION); permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.isEmpty()) {
            val fine = ActivityTrackingService.canUseGps(context)
            if (gps && !fine) permissionMessage = "GPS is unavailable. Starting timer only; distance can be entered manually."
            viewModel.startActivity(kind, gps && fine)
        }
        else { viewModel.moments.permissionOpen.value = true; permissionLauncher.launch(permissions.toTypedArray()) }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Live Activity", style = MaterialTheme.typography.titleMedium)
        val track = active
        if (track == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ActivityKind.WALKING, ActivityKind.RUNNING, ActivityKind.CYCLING, ActivityKind.DANCING, ActivityKind.SWIMMING).forEach { activity ->
                    FilterChip(selected = kind == activity, onClick = { kindName = activity.name; if (!activity.outdoor) gps = false }, label = { Text(activity.label) })
                }
            }
            if (kind.outdoor) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = gps, onCheckedChange = { gps = it }); Text("Optional local GPS route", modifier = Modifier.weight(1f))
            }
            if (kind == ActivityKind.SWIMMING) Text("Use a timer or manual log without carrying an unsuitable phone into water.", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = ::start, enabled = !busy) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Start") }
                OutlinedButton(onClick = { showManual = true }, enabled = !busy) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Manual Log") }
            }
        } else {
            Text(ActivityKind.fromStored(track.activityType).label, style = MaterialTheme.typography.bodyLarge)
            Text("%02d:%02d:%02d".format(track.elapsedMillis / 3_600_000, track.elapsedMillis / 60_000 % 60, track.elapsedMillis / 1000 % 60), style = MaterialTheme.typography.headlineSmall)
            Text(if (track.steps > 0) "${track.steps} sensor steps" else "No sensor steps recorded", style = MaterialTheme.typography.bodySmall)
            Text(if (track.distanceMeters > 0) "${"%.2f".format(track.distanceMeters / 1000)} km recorded" else "No GPS distance recorded", style = MaterialTheme.typography.bodySmall)
            if (track.warning.isNotBlank()) Text(track.warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (track.status == "RUNNING") Button(enabled = !busy, onClick = viewModel::pauseActivity) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(8.dp)); Text("Pause") }
                else Button(enabled = !busy, onClick = viewModel::resumeActivity) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Resume") }
                OutlinedButton(enabled = !busy, onClick = viewModel::stopActivity) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("Stop & Save") }
                IconButton(enabled = !busy, onClick = { discard = true }) { Icon(Icons.Default.Delete, "Discard active session") }
                if (track.gpsEnabled) IconButton(onClick = { routeKey = track.sessionKey }) { Icon(Icons.Default.Route, "Preview local route") }
            }
        }
        permissionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (history.isNotEmpty()) TextButton(onClick = { showHistory = true }) { Text("Tracked History (${history.size})") }
        HorizontalDivider()
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard Session?") },
        text = { Text("This removes the active timer, day totals, and local route points.") },
        confirmButton = { TextButton(onClick = { viewModel.discardActivity(); discard = false }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Cancel") } })

    if (showHistory) AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("Tracked Activity History") },
        text = { LazyColumn(Modifier.heightIn(max = 440.dp)) {
            items(history, key = { it.sessionKey }) { entry ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(ActivityKind.fromStored(entry.activityType).label, style = MaterialTheme.typography.titleSmall)
                    Text("${java.time.Instant.ofEpochMilli(entry.startEpochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}  |  ${entry.elapsedMillis / 60_000} minutes", style = MaterialTheme.typography.bodySmall)
                    Text("${"%.2f".format(entry.distanceMeters / 1000.0)} km recorded", style = MaterialTheme.typography.bodySmall)
                    Row {
                        IconButton(onClick = { correcting = entry }) { Icon(Icons.Default.Straighten, "Correct recorded distance") }
                        if (entry.gpsEnabled) IconButton(onClick = { routeKey = entry.sessionKey }) { Icon(Icons.Default.Route, "Preview local route") }
                        IconButton(onClick = { viewModel.deleteRoute(entry.sessionKey) }) { Icon(Icons.Default.LocationOff, "Delete route points") }
                        IconButton(onClick = { viewModel.deleteTrackedActivity(entry.sessionKey) }) { Icon(Icons.Default.Delete, "Delete tracked activity") }
                    }
                }
            }
        } }, confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } })
    correcting?.let { entry -> key(entry.sessionKey) {
        var distance by rememberSaveable { mutableStateOf(String.format(java.util.Locale.ROOT, "%.3f", entry.distanceMeters / 1000.0)) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { if (!savingDistance) correcting = null }, title = { Text("Recorded Distance") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(ActivityKind.fromStored(entry.activityType).label, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(distance, { distance = it }, label = { Text("Distance (km)") }, singleLine = true,
                    enabled = !savingDistance, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text("Saved duration, steps, rewards, and route points stay unchanged.", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !savingDistance, onClick = {
            val kilometers = distance.toDoubleOrNull()
            if (kilometers == null || !kilometers.isFinite() || kilometers !in 0.0..1000.0) error = "Enter a distance between 0 and 1000 km."
            else viewModel.correctActivityDistance(entry.sessionKey, kilometers) { correcting = null }
        }) { Text(if (savingDistance) "Saving..." else "Save") } }, dismissButton = {
            TextButton(enabled = !savingDistance, onClick = { correcting = null }) { Text("Cancel") }
        })
    } }
    routeKey?.let { key ->
        val points by viewModel.route(key).collectAsStateWithLifecycle(initialValue = emptyList())
        AlertDialog(onDismissRequest = { routeKey = null }, title = { Text("Local Route") },
            text = { Column { OfflineRoute(points); Text("${points.size} recorded GPS points. No map tiles or network service.", style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { TextButton(onClick = { routeKey = null }) { Text("Close") } })
    }
    if (showManual) ManualActivityDialog(kind, savingManual, onDismiss = { showManual = false }, onSave = { duration, distance, steps, notes ->
        viewModel.logActivitySession(kind.name, kind.label, duration, "Manual", distance, notes, steps) { showManual = false }
    })
}

@Composable
private fun OfflineRoute(points: List<RoutePointEntity>) {
    val routeColor = MaterialTheme.colorScheme.secondary
    val endpointColor = MaterialTheme.colorScheme.tertiary
    if (points.size < 2) { Text("Not enough GPS points for a route preview."); return }
    val center = points.map { it.latitude }.average()
    val projected = points.map { (it.longitude * cos(Math.toRadians(center))) to it.latitude }
    val minX = projected.minOf { it.first }; val maxX = projected.maxOf { it.first }
    val minY = projected.minOf { it.second }; val maxY = projected.maxOf { it.second }
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.4f)) {
        val range = maxOf(maxX - minX, maxY - minY, 0.0001)
        val scale = minOf(size.width, size.height) * 0.85 / range
        fun point(index: Int) = Offset((size.width / 2 + (projected[index].first - (minX + maxX) / 2) * scale).toFloat(),
            (size.height / 2 - (projected[index].second - (minY + maxY) / 2) * scale).toFloat())
        for (index in 1 until points.size) if (points[index].segment == points[index - 1].segment) drawLine(routeColor, point(index - 1), point(index), 4.dp.toPx(), StrokeCap.Round)
        drawCircle(endpointColor, 5.dp.toPx(), point(0)); drawCircle(routeColor, 5.dp.toPx(), point(points.lastIndex))
    }
}

@Composable
private fun ManualActivityDialog(kind: ActivityKind, saving: Boolean, onDismiss: () -> Unit, onSave: (Int, Float, Int, String) -> Unit) {
    var minutes by rememberSaveable { mutableStateOf("") }; var distance by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }; var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Manual ${kind.label}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(minutes, { minutes = it }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(distance, { distance = it }, label = { Text("Distance km (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            if (kind.countsSteps) OutlinedTextField(steps, { steps = it }, label = { Text("Steps (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = {
        val duration = minutes.toIntOrNull()
        val kilometers = if (distance.isBlank()) 0f else distance.toFloatOrNull()
        val stepCount = if (steps.isBlank()) 0 else steps.toIntOrNull()
        if (duration == null || duration !in 1..1440 || kilometers == null || !kilometers.isFinite() || kilometers < 0f || stepCount == null || stepCount < 0)
            error = "Enter a positive duration and valid optional distance or steps."
        else onSave(duration, kilometers, stepCount, notes)
    }) { Text(if (saving) "Saving..." else "Save") } }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}