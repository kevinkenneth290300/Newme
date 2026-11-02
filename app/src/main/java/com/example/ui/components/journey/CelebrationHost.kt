package com.example.ui.components.journey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.BadgeCatalog
import com.example.viewmodel.NewMeViewModel

@Composable
fun CelebrationHost(viewModel: NewMeViewModel, visible: Boolean) {
    val celebration by viewModel.badges.current.collectAsStateWithLifecycle()
    val moment by viewModel.moments.current.collectAsStateWithLifecycle()
    val permission by viewModel.moments.permissionOpen.collectAsStateWithLifecycle()
    val tracking by viewModel.activeTracking.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    if (!visible || moment != null || permission || tracking != null) return
    val event = celebration ?: return
    val haptic = rememberJourneyHaptic(settings?.hapticsEnabled == true)
    LaunchedEffect(event) { haptic() }
    val badge = BadgeCatalog.all.firstOrNull { it.id == event.badgeId }
    AlertDialog(onDismissRequest = viewModel.badges::dismiss, title = { Text(stringResource(if (badge != null) R.string.badge_unlocked else R.string.level_up)) }, text = {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EmberMascot(EmberStage.forLevel(event.level ?: viewModel.levelInfo.value.level), EmberExpression.CHEER)
            badge?.let { BadgeEmblem(BadgeCatalog.all.indexOf(it), it.category, it.tier, true, 1f); Text(stringResource(it.title), style = MaterialTheme.typography.titleLarge) }
            event.level?.let { Text(stringResource(BadgeCatalog.rank(it)), style = MaterialTheme.typography.titleLarge) }
            ConfettiBurst(true)
        }
    }, confirmButton = { TextButton(onClick = viewModel.badges::dismiss) { Text(stringResource(R.string.done)) } })
}