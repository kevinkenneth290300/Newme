package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.content.ReflectionItem
import com.example.ui.theme.*

@Composable
fun DailyReflectionCard(
    reflection: ReflectionItem?,
    isVoiceSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit,
    onNextReflection: () -> Unit,
    onOpenCheckin: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (reflection == null) return

    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_reflection_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Category Badge & Control Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NewMePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, NewMePrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = reflection.category.uppercase(),
                        color = NewMePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Voice Companion TTS button
                    IconButton(
                        onClick = {
                            if (isVoiceSpeaking) {
                                onStopSpeak()
                            } else {
                                val spokenText = "${reflection.principle}. ${reflection.text}"
                                onSpeak(spokenText)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("voice_companion_speak_button")
                    ) {
                        Icon(
                            imageVector = if (isVoiceSpeaking) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isVoiceSpeaking) "Mute Reflection" else "Listen to Reflection",
                            tint = if (isVoiceSpeaking) NewMeCoral else NewMePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Cycle Reflection button
                    IconButton(
                        onClick = onNextReflection,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("next_reflection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Cycle Principle",
                            tint = NewMeTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Principle Title
            Text(
                text = reflection.principle,
                color = NewMeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reflection Text (Philosophical, Calm, Actionable)
            Text(
                text = "\"${reflection.text}\"",
                color = NewMeTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Source/Author and Check-In Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— ${reflection.authorOrInspiration}",
                    color = NewMeTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onOpenCheckin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NewMePrimaryDark,
                        contentColor = NewMePrimary
                    ),
                    border = BorderStroke(1.dp, NewMePrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("open_checkin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NewMePrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CHECK-IN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
