package com.example.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    userProfile: UserProfileEntity?,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToToday: () -> Unit
) {
    // Pulse animation for the emblem
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Automatically transition after a brief branded moment
    LaunchedEffect(userProfile) {
        delay(1200)
        if (userProfile == null || !userProfile.isOnboarded) {
            onNavigateToOnboarding()
        } else {
            onNavigateToToday()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NewMeBackground,
                        NewMeSurfaceVariant,
                        NewMeBackground
                    )
                )
            )
            .padding(24.dp)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
            ) {
                // Outer Glow ring
                Surface(
                    shape = CircleShape,
                    color = NewMePrimary.copy(alpha = glowAlpha * 0.2f),
                    modifier = Modifier.fillMaxSize()
                ) {}

                // Inner Container
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = NewMeSurface,
                    border = BorderStroke(1.5.dp, NewMePrimary),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "NewMe Bolt",
                            tint = NewMePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Brand Headline
            Text(
                text = "NEWME",
                color = NewMePrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Philosophy Tagline
            Text(
                text = "DISCIPLINE • MOMENTUM • PROTOCOL",
                color = NewMeTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Subtle loading indicator
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .alpha(0.7f),
                color = NewMePrimary,
                trackColor = NewMeSurfaceDarker
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CALIBRATING PROTOCOLS...",
                color = NewMeTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        // Fast tap to proceed button at bottom
        Button(
            onClick = {
                if (userProfile == null || !userProfile.isOnboarded) {
                    onNavigateToOnboarding()
                } else {
                    onNavigateToToday()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = NewMeSurfaceVariant,
                contentColor = NewMeTextSecondary
            ),
            border = BorderStroke(1.dp, NewMeBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("splash_continue_button")
        ) {
            Text(
                text = "ENTER SYSTEM →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
