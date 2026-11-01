package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.data.local.entity.DailyCheckinEntity

@Composable
fun DailyCheckinDialog(
    initialTimeOfDay: String = "MORNING",
    onDismiss: () -> Unit,
    onSaveCheckin: (
        timeOfDay: String,
        mood: String,
        practiceVirtue: String,
        mattersMost: String,
        refuseToLeaveUnfinished: String,
        middayStatus: String,
        completedPercent: Int
    ) -> Unit,
    initialEntry: DailyCheckinEntity? = null,
    actualCompletedPercent: Int = 0,
    isSaving: Boolean = false
) {
    var selectedTimeOfDay by rememberSaveable { mutableStateOf(initialEntry?.timeOfDay ?: initialTimeOfDay) }
    var selectedMood by rememberSaveable { mutableStateOf(initialEntry?.mood.orEmpty()) }
    var selectedVirtue by rememberSaveable { mutableStateOf(initialEntry?.practiceVirtue.orEmpty()) }
    var mattersMost by rememberSaveable { mutableStateOf(initialEntry?.mattersMost.orEmpty()) }
    var refuseToLeaveUnfinished by rememberSaveable { mutableStateOf(initialEntry?.refuseToLeaveUnfinished.orEmpty()) }
    var middayStatus by rememberSaveable { mutableStateOf(initialEntry?.middayStatus.orEmpty()) }
    var completedPercent by rememberSaveable { mutableStateOf(initialEntry?.completedPercent ?: actualCompletedPercent) }

    val moods = listOf("Focused", "Calm", "Energized", "Tired", "Stressed", "Drifting")
    val virtues = listOf("Discipline", "Courage", "Wisdom", "Temperance", "Patience", "Grit")
    val middayStatuses = listOf("On Track", "Slight Drift", "Resetting Now", "Recovered")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurface,
            border = BorderStroke(1.dp, NewMeBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("daily_checkin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HONEST DAILY CHECK-IN",
                            color = NewMePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        )
                        Text(
                            text = "Acknowledge reality. Align intent.",
                            color = NewMeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_checkin_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time of Day Selector (Morning, Midday, Evening)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MORNING" to "Morning Anchor", "MIDDAY" to "Midday Reset", "EVENING" to "Evening Review").forEach { (code, label) ->
                        val isSelected = selectedTimeOfDay == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NewMePrimaryDark else NewMeSurfaceDarker,
                            border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTimeOfDay = code }
                                .testTag("checkin_tab_$code")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NewMePrimary else NewMeTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mood Selection
                Text("CURRENT INTERNAL STATE", color = NewMeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moods) { mood ->
                        val isSelected = selectedMood == mood
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NewMePrimary.copy(alpha = 0.15f) else NewMeSurfaceDarker,
                            border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                            modifier = Modifier
                                .clickable { selectedMood = mood }
                                .testTag("mood_option_$mood")
                        ) {
                            Text(
                                text = mood,
                                color = if (isSelected) NewMePrimary else NewMeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time-specific Form Fields
                when (selectedTimeOfDay) {
                    "MORNING" -> {
                        Text("VIRTUE TO PRACTICE TODAY", color = NewMeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(virtues) { virtue ->
                                val isSelected = selectedVirtue == virtue
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NewMeTertiary.copy(alpha = 0.15f) else NewMeSurfaceDarker,
                                    border = BorderStroke(1.dp, if (isSelected) NewMeTertiary else NewMeBorder),
                                    modifier = Modifier.clickable { selectedVirtue = virtue }
                                ) {
                                    Text(
                                        text = virtue,
                                        color = if (isSelected) NewMeTertiary else NewMeTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = mattersMost,
                            onValueChange = { mattersMost = it },
                            label = { Text("What matters most today?") },
                            placeholder = { Text("e.g., Maintain diet, deep work for 2 hours") },
                            modifier = Modifier.fillMaxWidth().testTag("matters_most_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NewMePrimary,
                                unfocusedBorderColor = NewMeBorder,
                                focusedTextColor = NewMeTextPrimary,
                                unfocusedTextColor = NewMeTextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = refuseToLeaveUnfinished,
                            onValueChange = { refuseToLeaveUnfinished = it },
                            label = { Text("I refuse to leave unfinished:") },
                            placeholder = { Text("e.g., Gym workout + 90g protein") },
                            modifier = Modifier.fillMaxWidth().testTag("refuse_unfinished_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NewMePrimary,
                                unfocusedBorderColor = NewMeBorder,
                                focusedTextColor = NewMeTextPrimary,
                                unfocusedTextColor = NewMeTextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    "MIDDAY" -> {
                        Text("MIDDAY STATUS", color = NewMeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            middayStatuses.forEach { status ->
                                val isSelected = middayStatus == status
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NewMeSecondary.copy(alpha = 0.2f) else NewMeSurfaceDarker,
                                    border = BorderStroke(1.dp, if (isSelected) NewMeSecondary else NewMeBorder),
                                    modifier = Modifier.weight(1f).clickable { middayStatus = status }
                                ) {
                                    Text(
                                        text = status,
                                        color = if (isSelected) NewMeSecondary else NewMeTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("COMPLETED DISCIPLINE SO FAR: $completedPercent%", color = NewMeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = completedPercent.toFloat(),
                            onValueChange = { completedPercent = it.toInt() },
                            valueRange = 0f..100f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = NewMePrimary,
                                activeTrackColor = NewMePrimary,
                                inactiveTrackColor = NewMeSurfaceDarker
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mattersMost,
                            onValueChange = { mattersMost = it },
                            label = { Text("Afternoon Pivot / Course Correction") },
                            placeholder = { Text("e.g., Hydrate, clear desk, no snacking") },
                            modifier = Modifier.fillMaxWidth().testTag("midday_pivot_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NewMePrimary,
                                unfocusedBorderColor = NewMeBorder,
                                focusedTextColor = NewMeTextPrimary,
                                unfocusedTextColor = NewMeTextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    "EVENING" -> {
                        OutlinedTextField(
                            value = mattersMost,
                            onValueChange = { mattersMost = it },
                            label = { Text("Honest Review: Biggest Win / Lesson") },
                            placeholder = { Text("e.g., Stuck to meal plan despite high work stress") },
                            modifier = Modifier.fillMaxWidth().testTag("evening_win_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NewMePrimary,
                                unfocusedBorderColor = NewMeBorder,
                                focusedTextColor = NewMeTextPrimary,
                                unfocusedTextColor = NewMeTextPrimary
                            ),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = refuseToLeaveUnfinished,
                            onValueChange = { refuseToLeaveUnfinished = it },
                            label = { Text("Tomorrow's Starting Intent") },
                            placeholder = { Text("e.g., Up at 6:30, water bottle ready") },
                            modifier = Modifier.fillMaxWidth().testTag("tomorrow_intent_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NewMePrimary,
                                unfocusedBorderColor = NewMeBorder,
                                focusedTextColor = NewMeTextPrimary,
                                unfocusedTextColor = NewMeTextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Check-in Button
                Button(
                    enabled = !isSaving && selectedMood.isNotBlank(),
                    onClick = {
                        onSaveCheckin(
                            selectedTimeOfDay,
                            selectedMood,
                            selectedVirtue,
                            mattersMost,
                            refuseToLeaveUnfinished,
                            middayStatus,
                            completedPercent
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag("save_checkin_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSaving) "Saving..." else "Save Check-in",
                        color = NewMePrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}
