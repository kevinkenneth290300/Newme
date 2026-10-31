package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.DisciplineScoreBreakdown
import com.example.ui.theme.*

@Composable
fun DisciplineBreakdownDialog(
    breakdown: DisciplineScoreBreakdown,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("discipline_breakdown_dialog")
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
                    Column {
                        Text(
                            text = "Today's Score",
                            color = NewMeTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${breakdown.totalScore} / 100",
                            color = NewMePrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_breakdown_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NewMeBorder)
                Spacer(modifier = Modifier.height(16.dp))

                ScoreRow(name = "Nutrition", score = breakdown.nutritionScore, max = breakdown.nutritionMax, color = NewMeTertiary)
                ScoreRow(name = "Protein Target", score = breakdown.proteinScore, max = breakdown.proteinMax, color = NewMePrimary)
                ScoreRow(name = "Workout & Training", score = breakdown.workoutScore, max = breakdown.workoutMax, color = NewMePurple)
                ScoreRow(name = "Water Intake", score = breakdown.waterScore, max = breakdown.waterMax, color = NewMeSecondary)
                ScoreRow(name = "Sleep & Recovery", score = breakdown.sleepScore, max = breakdown.sleepMax, color = Color(0xFF818CF8))
                ScoreRow(name = "Supplements", score = breakdown.supplementScore, max = breakdown.supplementMax, color = Color(0xFFFBBF24))
                ScoreRow(name = "Meal Prep Inventory", score = breakdown.mealPrepScore, max = breakdown.mealPrepMax, color = Color(0xFF34D399))
                ScoreRow(name = "Daily Checklist Habits", score = breakdown.habitScore, max = breakdown.habitMax, color = Color(0xFF38BDF8))

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NewMeSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Consistency over perfection. Missing one target won't ruin your transformation. Win the next decision.",
                        color = NewMeTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("breakdown_got_it_button")
                ) {
                    Text("GOT IT", color = Color(0xFF1A0E00), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(
    name: String,
    score: Int,
    max: Int,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, color = NewMeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "$score / $max", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        val ratio = (score.toFloat() / max).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = NewMeSurfaceElevated
        )
    }
}
