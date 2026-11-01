package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cycle.CyclePrediction
import com.example.data.local.entity.CycleEntryEntity
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate

@Composable
fun CycleControls(viewModel: NewMeViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val entries by viewModel.cycleEntries.collectAsStateWithLifecycle()
    val prediction by viewModel.cyclePrediction.collectAsStateWithLifecycle()
    val saving by viewModel.savingCycle.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CycleEntryEntity?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CycleEntryEntity?>(null) }
    val enabled = settings?.cycleEnabled == true

    LaunchedEffect(enabled) {
        if (!enabled) {
            showEditor = false
            showHistory = false
            editing = null
            deleting = null
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Optional Cycle History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = viewModel::setCycleEnabled,
                modifier = Modifier.semantics { contentDescription = "Cycle tracking" })
        }
        Text("Records stay on this phone. Turning tracking off does not delete your history.", style = MaterialTheme.typography.bodySmall)
        if (enabled) {
            when (val estimate = prediction) {
                is CyclePrediction.Estimate -> Text("Estimated next start: ${estimate.date}\nBased on ${estimate.intervalCount} recent intervals (median ${estimate.medianDays} days).", style = MaterialTheme.typography.bodyMedium)
                CyclePrediction.Insufficient -> Text("Insufficient history for an estimate. At least three recorded starts are needed.", style = MaterialTheme.typography.bodyMedium)
                CyclePrediction.Uncertain -> Text("History is uncertain or out of date; no next-start estimate is shown.", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Estimates only. Not fertility, pregnancy, contraception, diagnosis, or medical guidance. Cycle reminders are independently opt-in.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Period Dates")
            }
            entries.take(4).forEach { entry ->
                CycleRow(entry, onEdit = { editing = entry; showEditor = true }, onDelete = { deleting = entry })
            }
            if (entries.size > 4) TextButton(onClick = { showHistory = true }) { Text("All Period Entries (${entries.size})") }
        }
        HorizontalDivider()
    }

    if (enabled && showHistory) AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("Period History") },
        text = { LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            items(entries, key = { it.id }) { entry -> CycleRow(entry,
                { editing = entry; showEditor = true }, { deleting = entry }) }
        } }, confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } })

    if (enabled && showEditor) key(editing?.id) {
        var start by rememberSaveable { mutableStateOf(editing?.startDate ?: LocalDate.now().toString()) }
        var end by rememberSaveable { mutableStateOf(editing?.endDate.orEmpty()) }
        var symptoms by rememberSaveable { mutableStateOf(editing?.symptoms.orEmpty()) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { if (!saving) showEditor = false }, title = { Text("Period Dates") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(start, { start = it }, label = { Text("Start (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(end, { end = it }, label = { Text("End (optional)") }, singleLine = true)
                OutlinedTextField(symptoms, { symptoms = it }, label = { Text("Symptoms (optional)") }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !saving, onClick = {
            try {
                val today = LocalDate.now()
                val startDate = LocalDate.parse(start)
                require(startDate.year >= 1900 && !startDate.isAfter(today)) { "Start must be no later than today." }
                val endDate = end.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                require(endDate == null || (!endDate.isBefore(startDate) && !endDate.isAfter(today))) { "End must be between start and today." }
                viewModel.saveCycle(CycleEntryEntity(editing?.id ?: 0, start, end.takeIf { it.isNotBlank() }, symptoms)) { showEditor = false }
                error = null
            } catch (failure: Exception) { error = "Enter valid dates in YYYY-MM-DD format." }
        }) { Text(if (saving) "Saving..." else "Save") } }, dismissButton = {
            TextButton(enabled = !saving, onClick = { showEditor = false }) { Text("Cancel") }
        })
    }
    deleting?.takeIf { enabled }?.let { entry -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete Period Entry?") },
        text = { Text("This removes the selected dates and symptoms from local history.") },
        confirmButton = { TextButton(onClick = { viewModel.deleteCycle(entry); deleting = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}

@Composable
private fun CycleRow(entry: CycleEntryEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.startDate + (entry.endDate?.let { " to $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
            if (entry.symptoms.isNotBlank()) Text(entry.symptoms, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit period entry") }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete period entry") }
    }
}