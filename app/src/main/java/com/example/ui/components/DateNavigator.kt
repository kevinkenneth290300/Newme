package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DateNavigator(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val today = LocalDate.now()
    val parsedDate = try {
        LocalDate.parse(selectedDate)
    } catch (e: Exception) {
        today
    }

    val isToday = parsedDate == today
    val isYesterday = parsedDate == today.minusDays(1)

    val label = when {
        isToday -> "TODAY"
        isYesterday -> "YESTERDAY"
        else -> parsedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NewMeSurface)
            .border(1.dp, NewMeBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val prev = parsedDate.minusDays(1).toString()
                onDateSelected(prev)
            },
            modifier = Modifier.testTag("date_nav_prev")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Day",
                tint = NewMeTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (!isToday) onDateSelected(today.toString())
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("date_nav_label")
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = "Calendar",
                tint = if (isToday) NewMePrimary else NewMeTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isToday) NewMePrimary else NewMeTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (!isToday) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${parsedDate.format(DateTimeFormatter.ofPattern("MMM d"))})",
                    color = NewMeTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        IconButton(
            onClick = {
                val next = parsedDate.plusDays(1).toString()
                onDateSelected(next)
            },
            modifier = Modifier.testTag("date_nav_next")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Day",
                tint = NewMeTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
