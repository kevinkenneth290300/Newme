package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CircularDisciplineRing(
    score: Int,
    maxScore: Int = 100,
    size: Dp = 80.dp,
    strokeWidth: Dp = 5.dp,
    onClick: () -> Unit = {}
) {
    val progress = (score.toFloat() / maxScore.coerceAtLeast(1)).coerceIn(0f, 1f)
    val trackColor = NewMeSurfaceDarker
    val progressColor = NewMePrimary
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (LocalReducedMotion.current) 0 else 800),
        label = "disciplineRing"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clickable { onClick() }
            .testTag("discipline_score_ring")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Animated progress track
            val sweep = animatedProgress * 360f
            if (sweep > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score",
                color = NewMeTextPrimary,
                fontSize = if (size < 120.dp) 20.sp else 38.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (size < 120.dp) 22.sp else 40.sp
            )
            Text(
                text = "TODAY",
                color = NewMeTextMuted,
                fontSize = if (size < 120.dp) 8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
    }
}
