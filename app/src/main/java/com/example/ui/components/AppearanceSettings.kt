package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeMode
import com.example.viewmodel.NewMeViewModel

@Composable
fun AppearanceSettings(viewModel: NewMeViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val voiceAvailable by viewModel.offlineVoiceAvailable.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = (settings?.themeMode ?: "SYSTEM") == mode.name,
                    onClick = { viewModel.setAppearance(mode, settings?.reducedMotion ?: false) })
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reduced motion", modifier = Modifier.weight(1f))
            Switch(checked = settings?.reducedMotion ?: false, onCheckedChange = {
                viewModel.setAppearance(ThemeMode.entries.firstOrNull { mode -> mode.name == settings?.themeMode } ?: ThemeMode.SYSTEM, it)
            })
        }
        Text(if (voiceAvailable) "Offline voice available" else "Offline voice unavailable; network voices are never used.", style = MaterialTheme.typography.bodySmall)
    }
}