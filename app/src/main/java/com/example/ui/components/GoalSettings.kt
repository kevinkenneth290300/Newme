package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyGoalKind
import com.example.viewmodel.NewMeViewModel

@Composable
fun GoalSettings(viewModel: NewMeViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    var kind by rememberSaveable(settings?.dailyGoalType) { mutableStateOf(settings?.dailyGoalType ?: DailyGoalKind.TASKS.name) }
    var target by rememberSaveable(settings?.dailyGoalTarget) { mutableStateOf((settings?.dailyGoalTarget ?: 3).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily Streak Goal", style = MaterialTheme.typography.titleMedium)
        DailyGoalKind.entries.forEach { option ->
            Row {
                RadioButton(selected = kind == option.name, onClick = { kind = option.name; target = if (option == DailyGoalKind.TASKS) "3" else "20" })
                Text(option.label, modifier = Modifier.padding(top = 12.dp).weight(1f))
            }
        }
        OutlinedTextField(target, { target = it }, label = { Text("Daily target") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val goal = DailyGoalKind.valueOf(kind)
                val value = target.toIntOrNull() ?: throw IllegalArgumentException("Enter a whole-number target.")
                goal.validate(value)
                viewModel.setDailyGoal(goal, value)
                error = null
            } catch (failure: IllegalArgumentException) { error = failure.message }
        }) {
            Icon(Icons.Default.Flag, null)
            Spacer(Modifier.width(8.dp))
            Text("Set Goal")
        }
        Text("Changes apply to today and future days. Past goals stay unchanged. Earned XP is never removed after a missed day.", style = MaterialTheme.typography.bodySmall)
    }
}