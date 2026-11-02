package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

data class JourneyColors(val success: Color, val warning: Color, val bronze: Color, val silver: Color, val gold: Color, val nova: Color, val glow: Color) {
    companion object {
        val dark = JourneyColors(Color(0xFF5CF2B0), Color(0xFFFFD166), Color(0xFFE4A66B), Color(0xFFC8D5E8), Color(0xFFFFD166), Color(0xFFFF7EB6), Color(0xFFFF8A3D))
        val light = JourneyColors(Color(0xFF047857), Color(0xFF8A5900), Color(0xFF854619), Color(0xFF526076), Color(0xFF806100), Color(0xFF9B277A), Color(0xFFC2410C))
    }
}

val LocalJourneyColors = staticCompositionLocalOf { JourneyColors.light }

object JourneyContrast {
    fun ratio(foreground: Long, background: Long): Double {
        fun luminance(color: Long): Double {
            fun channel(shift: Int): Double {
                val value = ((color shr shift) and 255).toDouble() / 255
                return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
        val first = luminance(foreground); val second = luminance(background)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }
    val textPairs = listOf(0xF1EEFFL to 0x0F0D24L, 0xF1EEFFL to 0x181538L, 0xBDB6E6L to 0x211D4AL,
        0x2B1000L to 0xFF8A3DL, 0x002F39L to 0x3DE0FFL, 0x4D0026L to 0xFF7EB6L,
        0x1F1A3DL to 0xFFF8F0L, 0x1F1A3DL to 0xFFFFFFL, 0x5A5380L to 0xFFEFE0L,
        0xFFFFFFL to 0xC2410CL, 0xFFFFFFL to 0x0E7490L, 0xFFFFFFL to 0xBE185DL,
        0x5CF2B0L to 0x181538L, 0x047857L to 0xFFFFFFL)
}