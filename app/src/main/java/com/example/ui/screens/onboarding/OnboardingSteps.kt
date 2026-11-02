package com.example.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.routine.AppModule
import com.example.data.routine.StarterPacks
import com.example.ui.components.journey.JourneyIcons
import com.example.ui.components.journey.OptionTile

fun moduleLabel(module: AppModule): Int = when (module) {
    AppModule.ROUTINES -> R.string.nav_routines; AppModule.MEALS -> R.string.log_meal; AppModule.GYM -> R.string.nav_gym
    AppModule.ACTIVITY -> R.string.log_move; AppModule.WATER -> R.string.log_water; AppModule.SLEEP -> R.string.log_sleep; AppModule.CYCLE -> R.string.settings_cycle
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModuleChoices(selected: Set<String>, onChange: (Set<String>) -> Unit, includeCycle: Boolean = true) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppModule.entries.filter { includeCycle || it != AppModule.CYCLE }.forEach { module ->
            val icon = when (module) { AppModule.ROUTINES -> "sun"; AppModule.MEALS -> "meal"; AppModule.GYM -> "move"; AppModule.ACTIVITY -> "run"; AppModule.WATER -> "water"; AppModule.SLEEP -> "moon"; AppModule.CYCLE -> "calendar" }
            OptionTile(stringResource(moduleLabel(module)), JourneyIcons.get(icon), module.name in selected,
                { onChange(if (module.name in selected) selected - module.name else selected + module.name) })
        }
    }
}

@Composable
fun NumberStepper(label: String, value: Float, range: ClosedFloatingPointRange<Float>, increment: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyMedium); Text(if (increment >= 1f) value.toInt().toString() else "%.1f".format(value), style = MaterialTheme.typography.titleLarge) }
        IconButton(enabled = value > range.start, onClick = { onChange((value - increment).coerceIn(range.start, range.endInclusive)) }) { Icon(Icons.Default.Remove, stringResource(R.string.decrease)) }
        IconButton(enabled = value < range.endInclusive, onClick = { onChange((value + increment).coerceIn(range.start, range.endInclusive)) }) { Icon(Icons.Default.Add, stringResource(R.string.increase)) }
    }
}

@Composable
fun StarterChoices(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    Column {
        StarterPacks.catalog.forEach { pack -> Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(pack.id in selected, { checked -> onChange(if (checked) selected + pack.id else selected - pack.id) })
            Text(stringResource(pack.titleRes), modifier = Modifier.weight(1f))
        } }
    }
}