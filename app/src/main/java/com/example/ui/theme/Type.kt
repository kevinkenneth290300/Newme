package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

private val Barlow = FontFamily(Font(R.font.barlow_regular), Font(R.font.barlow_semibold, FontWeight.SemiBold))
private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Barlow, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = 0.sp)

val Typography = Typography(
    displayLarge = textStyle(40, 48, FontWeight.SemiBold), displayMedium = textStyle(34, 42, FontWeight.SemiBold), displaySmall = textStyle(30, 36, FontWeight.SemiBold),
    headlineLarge = textStyle(28, 34, FontWeight.SemiBold), headlineMedium = textStyle(24, 30, FontWeight.SemiBold), headlineSmall = textStyle(22, 28, FontWeight.SemiBold),
    titleLarge = textStyle(20, 26, FontWeight.SemiBold), titleMedium = textStyle(16, 22, FontWeight.SemiBold), titleSmall = textStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = textStyle(16, 24), bodyMedium = textStyle(14, 20), bodySmall = textStyle(12, 18),
    labelLarge = textStyle(14, 20, FontWeight.SemiBold), labelMedium = textStyle(12, 18, FontWeight.SemiBold), labelSmall = textStyle(11, 16, FontWeight.SemiBold)
)
