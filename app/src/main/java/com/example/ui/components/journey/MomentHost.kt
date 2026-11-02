package com.example.ui.components.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.viewmodel.NewMeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentHost(viewModel: NewMeViewModel, visible: Boolean) {
    val model = viewModel.moments
    val current by model.current.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val permission by model.permissionOpen.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val active by viewModel.activeTracking.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_RESUME -> model.resumed()
            Lifecycle.Event.ON_STOP -> model.backgrounded()
            else -> Unit
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(visible, profile?.isOnboarded, settings, active, permission) {
        if (visible && profile?.isOnboarded == true && settings?.dashboardSetupDone == true && !permission && active == null) model.consider()
    }
    if (!visible || permission || active != null || settings?.momentsEnabled != true || settings?.dashboardSetupDone != true) return
    val moment = current ?: return
    val card = moment.card ?: return
    key(moment.candidate.key, card.type) {
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val haptic = rememberJourneyHaptic(settings?.hapticsEnabled == true)
        Dialog(onDismissRequest = model::later, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().testTag("moment_host"), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    val slot = when (moment.candidate.type) {
                        "COMEBACK" -> PosterSlot.COMEBACK
                        "MORNING" -> PosterSlot.MORNING
                        "MIDDAY" -> PosterSlot.MIDDAY
                        else -> PosterSlot.EVENING
                    }
                    PosterImage(slot, Modifier.fillMaxWidth().height(220.dp), if (selected == null) EmberExpression.HAPPY else EmberExpression.CHEER)
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            moment.cards.forEach { step -> LinearProgressIndicator(progress = { if (step.type in moment.answers || step.type == card.type) 1f else 0f }, modifier = Modifier.width(28.dp).height(6.dp)) }
                        }
                        Text(stringResource(card.question), style = MaterialTheme.typography.headlineMedium)
                        if (card.type == "sleep") Text(stringResource(R.string.sleep_approximate), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            card.options.forEach { option ->
                                OptionTile(option.text ?: stringResource(option.label), JourneyIcons.get(option.icon), selected == option.value,
                                    { selected = option.value; haptic() }, enabled = !busy)
                            }
                        }
                        Button(enabled = selected != null && !busy, onClick = { model.answer(card.type, selected!!) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(stringResource(if (moment.cards.last().type == card.type) R.string.save else R.string.next))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(enabled = !busy, onClick = model::later) { Text(stringResource(R.string.later)) }
                            TextButton(enabled = !busy, onClick = model::skip) { Text(stringResource(R.string.skip)) }
                        }
                    }
                }
            }
        }
    }
}