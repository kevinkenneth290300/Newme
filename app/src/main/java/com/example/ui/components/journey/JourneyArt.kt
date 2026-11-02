package com.example.ui.components.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalReducedMotion
import kotlin.math.cos
import kotlin.math.sin

enum class EmberExpression { HAPPY, FOCUSED, SLEEPY, CHEER, REST }
enum class EmberStage { SPARK, EMBER, FLAME, BLAZE, NOVA;
    companion object { fun forLevel(level: Int) = when (level) { in 1..2 -> SPARK; in 3..4 -> EMBER; in 5..6 -> FLAME; in 7..8 -> BLAZE; else -> NOVA } }
}
enum class PosterSlot(val resource: String) { WELCOME("poster_welcome"), MORNING("poster_morning"), MIDDAY("poster_midday"), EVENING("poster_evening"), NIGHT("poster_night"), COMEBACK("poster_comeback"), LEVELUP("poster_levelup"), EMPTY_ROUTINES("poster_empty_routines"), EMPTY_PROGRESS("poster_empty_progress") }

@Composable
fun EmberMascot(stage: EmberStage = EmberStage.SPARK, expression: EmberExpression = EmberExpression.HAPPY, size: Dp = 96.dp, visible: Boolean = true) {
    var breath = 1f
    var blink = false
    if (visible && !LocalReducedMotion.current) {
        val transition = rememberInfiniteTransition(label = "ember idle")
        val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "ember breath")
        breath = 1f + sin(pulse * Math.PI.toFloat() * 2f) * 0.025f
        blink = pulse in 0.92f..0.94f
    }
    val description = stringResource(R.string.ember_description, stage.name.lowercase(), expression.name.lowercase())
    Canvas(Modifier.size(size).semantics { contentDescription = description }) {
        scale(this.size.width / 100f, this.size.height / 120f, Offset.Zero) {
            scale(breath, breath, Offset(50f, 110f)) { ember(stage, expression, blink) }
        }
    }
}

private fun DrawScope.ember(stage: EmberStage, expression: EmberExpression, blink: Boolean = false) {
    val ink = Color(0xFF2B1B4A)
    val flame = Path().apply {
        moveTo(50f, 5f); cubicTo(60f, 27f, 86f, 40f, 88f, 74f)
        cubicTo(91f, 100f, 71f, 113f, 50f, 113f); cubicTo(28f, 113f, 9f, 97f, 12f, 74f)
        cubicTo(14f, 55f, 27f, 44f, 34f, 29f); cubicTo(39f, 40f, 47f, 44f, 49f, 35f)
        cubicTo(52f, 25f, 48f, 15f, 50f, 5f); close()
    }
    if (stage.ordinal >= 2) {
        drawCircle(Color(0xFFFFD166).copy(alpha = 0.16f), 43f, Offset(50f, 67f))
        drawLine(Color(0xFFFFD166), Offset(9f, 37f), Offset(14f, 28f), 3f, StrokeCap.Round)
        drawLine(Color(0xFFFF7EB6), Offset(86f, 28f), Offset(92f, 37f), 3f, StrokeCap.Round)
    }
    drawPath(flame, Color(0xFFFF8A3D)); drawPath(flame, ink, style = Stroke(3f, join = StrokeJoin.Round))
    val belly = Path().apply { moveTo(50f, 52f); cubicTo(58f, 65f, 74f, 68f, 73f, 86f); cubicTo(72f, 105f, 29f, 109f, 27f, 86f); cubicTo(26f, 70f, 44f, 63f, 50f, 52f); close() }
    drawPath(belly, Color(0xFFFFD98A))
    drawLine(Color(0xFFFFEFB0), Offset(63f, 41f), Offset(72f, 54f), 4f, StrokeCap.Round)
    val resting = expression == EmberExpression.REST || expression == EmberExpression.SLEEPY || blink
    for (eyeX in listOf(40f, 60f)) {
        if (resting) drawLine(ink, Offset(eyeX - 4f, 81f), Offset(eyeX + 4f, 81f), 3f, StrokeCap.Round)
        else {
            drawOval(ink, Offset(eyeX - 5f, 72f), Size(10f, 15f))
            drawCircle(Color.White, 2f, Offset(eyeX + 1f, 76f))
        }
    }
    val mouth = Path().apply { moveTo(43f, 92f); quadraticTo(50f, if (expression == EmberExpression.CHEER) 106f else 98f, 57f, 92f) }
    drawPath(mouth, ink, style = Stroke(2.5f, cap = StrokeCap.Round))
    drawCircle(Color(0xFFFF7EB6).copy(alpha = 0.7f), 4f, Offset(32f, 90f)); drawCircle(Color(0xFFFF7EB6).copy(alpha = 0.7f), 4f, Offset(68f, 90f))
    if (expression == EmberExpression.FOCUSED) drawLine(ink, Offset(33f, 69f), Offset(43f, 72f), 2.5f, StrokeCap.Round)
    if (stage.ordinal >= 1) drawCircle(Color(0xFF3DE0FF), 3f, Offset(50f, 106f))
    if (stage.ordinal >= 3) drawLine(Color(0xFF3DE0FF), Offset(34f, 105f), Offset(66f, 105f), 2f, StrokeCap.Round)
    if (stage == EmberStage.NOVA) for (index in 0..4) {
        val angle = index * Math.PI.toFloat() / 2.5f
        val center = Offset(50f + cos(angle) * 45f, 52f + sin(angle) * 32f)
        drawLine(Color(0xFFFF7EB6), center - Offset(3f, 0f), center + Offset(3f, 0f), 2f)
        drawLine(Color(0xFFFF7EB6), center - Offset(0f, 3f), center + Offset(0f, 3f), 2f)
    }
}

@Composable
fun PosterImage(slot: PosterSlot, modifier: Modifier = Modifier, expression: EmberExpression = EmberExpression.HAPPY) {
    val context = LocalContext.current
    val drawable = remember(slot) { context.resources.getIdentifier(slot.resource, "drawable", context.packageName) }
    val description = stringResource(R.string.poster_description, slot.name.lowercase().replace('_', ' '))
    if (drawable != 0) Image(painterResource(drawable), description, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    else {
        val colors = MaterialTheme.colorScheme
        Canvas(modifier.semantics { contentDescription = description }) {
            val night = slot == PosterSlot.NIGHT || slot == PosterSlot.EVENING
            drawRect(colors.surfaceVariant)
            drawCircle(if (night) colors.tertiary.copy(alpha = 0.3f) else colors.primary.copy(alpha = 0.25f), size.minDimension * 0.2f, Offset(size.width * 0.78f, size.height * 0.3f))
            val horizon = Path().apply { moveTo(0f, size.height * 0.73f); cubicTo(size.width * 0.3f, size.height * 0.5f, size.width * 0.6f, size.height * 0.9f, size.width, size.height * 0.67f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(horizon, colors.secondary.copy(alpha = 0.15f))
            val back = Path().apply { moveTo(0f, size.height * 0.88f); quadraticTo(size.width * 0.6f, size.height * 0.62f, size.width, size.height * 0.9f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(back, colors.tertiary.copy(alpha = 0.12f))
            when (slot) {
                PosterSlot.MORNING, PosterSlot.WELCOME -> drawLine(colors.primary, Offset(size.width * 0.1f, size.height * 0.6f), Offset(size.width * 0.9f, size.height * 0.6f), 2.dp.toPx())
                PosterSlot.MIDDAY -> for (ray in 0..7) {
                    val angle = ray * Math.PI / 4
                    val sun = Offset(size.width * 0.78f, size.height * 0.3f)
                    drawLine(colors.primary, sun + Offset((cos(angle) * 30.dp.toPx()).toFloat(), (sin(angle) * 30.dp.toPx()).toFloat()), sun + Offset((cos(angle) * 36.dp.toPx()).toFloat(), (sin(angle) * 36.dp.toPx()).toFloat()), 2.dp.toPx())
                }
                PosterSlot.NIGHT -> drawCircle(colors.surfaceVariant, size.minDimension * 0.18f, Offset(size.width * 0.85f, size.height * 0.22f))
                PosterSlot.EVENING -> drawLine(colors.tertiary, Offset(size.width * 0.7f, size.height * 0.45f), Offset(size.width * 0.92f, size.height * 0.45f), 3.dp.toPx())
                PosterSlot.COMEBACK -> drawLine(colors.secondary, Offset(size.width * 0.14f, size.height * 0.85f), Offset(size.width * 0.3f, size.height * 0.65f), 4.dp.toPx(), StrokeCap.Round)
                PosterSlot.LEVELUP -> drawRect(colors.tertiary.copy(alpha = 0.15f), Offset(size.width * 0.2f, size.height * 0.74f), Size(size.width * 0.6f, size.height * 0.18f))
                PosterSlot.EMPTY_ROUTINES, PosterSlot.EMPTY_PROGRESS -> {
                    for (column in 0..2) drawRoundRect(colors.surface, Offset(size.width * (0.1f + column * 0.27f), size.height * 0.72f), Size(size.width * 0.2f, size.height * 0.15f), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                }
            }
            val factor = size.height * 0.72f / 120f
            translate(size.width * 0.5f - 50f * factor, size.height * 0.27f) {
                scale(factor, factor, Offset.Zero) { ember(if (slot == PosterSlot.LEVELUP) EmberStage.FLAME else EmberStage.SPARK, expression) }
            }
            for (index in 1..5) {
                val point = Offset(size.width * index / 6f, size.height * (if (index % 2 == 0) 0.18f else 0.34f))
                drawLine(colors.secondary, point - Offset(3f, 0f), point + Offset(3f, 0f), 2f)
                drawLine(colors.secondary, point - Offset(0f, 3f), point + Offset(0f, 3f), 2f)
            }
        }
    }
}