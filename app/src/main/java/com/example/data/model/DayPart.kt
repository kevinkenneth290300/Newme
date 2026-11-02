package com.example.data.model

enum class DayPart(val checkinPeriod: String) {
    MORNING("MORNING"), MIDDAY("MIDDAY"), EVENING("EVENING"), NIGHT("EVENING");

    companion object {
        fun at(hour: Int): DayPart {
            require(hour in 0..23)
            return when (hour) {
                in 4..11 -> MORNING
                in 12..16 -> MIDDAY
                in 17..21 -> EVENING
                else -> NIGHT
            }
        }
    }
}