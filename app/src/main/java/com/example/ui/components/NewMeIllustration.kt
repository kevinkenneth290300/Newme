package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun NewMeIllustration(activity: Boolean = false, size: Dp = 112.dp) {
    Image(painter = painterResource(if (activity) R.drawable.ill_activity else R.drawable.ill_growth),
        contentDescription = if (activity) "Movement illustration" else "Growing plant illustration", modifier = Modifier.size(size))
}