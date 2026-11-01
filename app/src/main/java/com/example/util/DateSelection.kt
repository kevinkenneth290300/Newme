package com.example.util

import java.time.LocalDate

class DateSelection(initialToday: LocalDate) {
    var selected: LocalDate = initialToday
        private set
    var today: LocalDate = initialToday
        private set
    private var followingToday = true

    fun select(date: LocalDate) {
        selected = date
        followingToday = date == today
    }

    fun refresh(newToday: LocalDate): LocalDate {
        today = newToday
        if (followingToday) selected = today
        return selected
    }
}