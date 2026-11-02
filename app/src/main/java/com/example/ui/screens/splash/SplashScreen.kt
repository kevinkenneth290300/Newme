package com.example.ui.screens.splash
import com.example.ui.components.NewMeIllustration
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
    onNavigateToToday: () -> Unit,
    isLoading: Boolean = false
) {
    // Automatically transition after a brief branded moment
    LaunchedEffect(userProfile, isLoading) {
        if (isLoading) return@LaunchedEffect
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
                NewMeBackground
            )
            .padding(24.dp)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NewMeIllustration(size = 144.dp)

            Spacer(modifier = Modifier.height(28.dp))

            // App Brand Headline
            Text(
                text = "NEWME",
                color = NewMePrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Philosophy Tagline
            Text(
                text = "DISCIPLINE • MOMENTUM • PROTOCOL",
                color = NewMeTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.heightIn(min = 48.dp))

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
                text = androidx.compose.ui.res.stringResource(if (isLoading) com.example.R.string.loading_local else com.example.R.string.ready),
                color = NewMeTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }

        // Fast tap to proceed button at bottom
        Button(
            enabled = !isLoading,
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
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("splash_continue_button")
        ) {
            Text(
                text = "Continue",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
    }
}
