package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class Obstacle(
    val title: String,
    val description: String,
    val minimumViableActions: List<String>
)

val OBSTACLES = listOf(
    Obstacle(
        title = "Too tired",
        description = "Fatigue is signaling your central nervous system needs restorative rest.",
        minimumViableActions = listOf(
            "Hit daily protein target (helps body recover)",
            "Drink 2.5L water",
            "Eat at least one proper nutritious meal",
            "10–15 minute light outdoor walk (no intense gym)",
            "Sleep 7.5+ hours tonight"
        )
    ),
    Obstacle(
        title = "Work pressure",
        description = "High cognitive stress drains willpower. Strip down to essentials.",
        minimumViableActions = listOf(
            "Quick protein shake / boiled eggs",
            "Keep water bottle filled at desk",
            "15-minute bodyweight mobility or brisk walk",
            "No late-night work past 10:30 PM"
        )
    ),
    Obstacle(
        title = "Travel",
        description = "Environment disruption doesn't mean momentum disruption.",
        minimumViableActions = listOf(
            "Prioritize protein at every airport/road meal",
            "Drink 500ml water every 3 hours",
            "Take stairs and hit 6,000 steps",
            "Pack protein bars or whey isolate"
        )
    ),
    Obstacle(
        title = "No food available",
        description = "Solve decision fatigue with simple convenience choices.",
        minimumViableActions = listOf(
            "Quick grocery stop: eggs, curd, rotisserie chicken, or paneer",
            "Order grilled protein over fast food",
            "Double scoop whey protein",
            "Stay hydrated to manage cravings"
        )
    ),
    Obstacle(
        title = "Missed gym",
        description = "Never turn a missed session into a skipped mindset.",
        minimumViableActions = listOf(
            "Perform 50 pushups + 50 bodyweight squats at home",
            "20-minute brisk walk or cardio",
            "Keep nutrition strictly on plan",
            "Reschedule session tomorrow with clear intent"
        )
    ),
    Obstacle(
        title = "Junk food cravings",
        description = "Cravings usually signal low protein or dehydration.",
        minimumViableActions = listOf(
            "Drink 500ml cold water immediately",
            "Eat 25g solid protein first",
            "If still craving, allow a small portion without guilt",
            "Return to plan on the very next meal"
        )
    ),
    Obstacle(
        title = "Poor sleep",
        description = "Sleep debt impairs glucose regulation and impulse control.",
        minimumViableActions = listOf(
            "Reduce caffeine after 2:00 PM",
            "Skip heavy lifts in favor of light pump work",
            "Hit protein and water targets",
            "Bedtime at 10:00 PM tonight"
        )
    ),
    Obstacle(
        title = "Lost motivation",
        description = "Discipline is doing what needs to be done when motivation is gone.",
        minimumViableActions = listOf(
            "Pick the single easiest win: drink 500ml water",
            "Check off just 1 meal",
            "Remember why you started Day 001",
            "Tomorrow is already waiting"
        )
    ),
    Obstacle(
        title = "Sick / rest day",
        description = "Illness requires complete recovery, not guilt.",
        minimumViableActions = listOf(
            "Zero workouts - full bodily rest",
            "Warm fluids, electrolytes, and water",
            "Nourishing soups, eggs, or light protein",
            "Sleep as much as your body requests"
        )
    )
)

@Composable
fun FallingOffDialog(
    onDismiss: () -> Unit,
    onApplyPlan: (obstacle: String, planText: String) -> Unit
) {
    var selectedObstacle by remember { mutableStateOf<Obstacle?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("falling_off_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Falling off alert",
                            tint = NewMeCoral,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I'm Falling Off",
                            color = NewMeTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedObstacle == null) {
                    Text(
                        text = "What is getting in your way right now?",
                        color = NewMeTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(OBSTACLES) { obstacle ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NewMeSurfaceElevated)
                                    .border(1.dp, NewMeBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedObstacle = obstacle }
                                    .padding(14.dp)
                                    .testTag("obstacle_item_${obstacle.title.replace(" ", "_").lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = obstacle.title,
                                        color = NewMeTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Solve →",
                                        color = NewMePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val obs = selectedObstacle!!
                    Text(
                        text = obs.title.uppercase(),
                        color = NewMePrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = obs.description,
                        color = NewMeTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MINIMUM VIABLE DAY:",
                        color = NewMeTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NewMeSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        obs.minimumViableActions.forEach { action ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Check",
                                    tint = NewMeTertiary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = action,
                                    color = NewMeTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedObstacle = null },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back", color = NewMeTextSecondary)
                        }
                        Button(
                            onClick = {
                                val planText = obs.minimumViableActions.joinToString("\n")
                                onApplyPlan(obs.title, planText)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NewMeTertiary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("activate_recovery_plan_button")
                        ) {
                            Text(
                                "ACTIVATE PLAN",
                                color = Color(0xFF003824),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
