package com.example.data.activity

import java.time.Instant
import java.time.ZoneId

enum class TrackingPhase { RUNNING, PAUSED, FINISHED }
data class StepDelta(val baseline: Long, val added: Int)

object TrackingRules {
    fun steps(previous: Long, reading: Double): StepDelta? {
        if (!reading.isFinite() || reading < 0.0 || reading > Long.MAX_VALUE.toDouble()) return null
        val current = reading.toLong()
        val added = if (previous < 0 || current < previous) 0 else (current - previous).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return StepDelta(current, added)
    }

    fun credibleElapsed(previous: Long, current: Long): Long? {
        val delta = current - previous
        return delta.takeIf { it in 0..30_000 }
    }

    fun splitDuration(endEpochMillis: Long, durationMillis: Long, zone: ZoneId): Map<String, Long> {
        require(durationMillis >= 0)
        var cursor = Instant.ofEpochMilli(endEpochMillis - durationMillis)
        val end = Instant.ofEpochMilli(endEpochMillis)
        val totals = linkedMapOf<String, Long>()
        while (cursor < end) {
            val date = cursor.atZone(zone).toLocalDate()
            val midnight = date.plusDays(1).atStartOfDay(zone).toInstant()
            val next = minOf(midnight, end)
            val delta = next.toEpochMilli() - cursor.toEpochMilli()
            totals[date.toString()] = (totals[date.toString()] ?: 0) + delta
            cursor = next
        }
        return totals
    }
}