package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.AchievementProgress
import com.example.util.UiWriteQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class JourneyCelebration(val badgeId: String? = null, val level: Int? = null)

class CelebrationQueue {
    private val seen = mutableSetOf<String>()
    private val pending = mutableListOf<JourneyCelebration>()
    fun offer(key: String, event: JourneyCelebration) { if (seen.add(key)) pending.add(event) }
    fun peek(): JourneyCelebration? = pending.firstOrNull()
    fun remove() { if (pending.isNotEmpty()) pending.removeAt(0) }
}

class BadgesViewModel(private val database: NewMeDatabase, context: Context, scope: CoroutineScope,
    private val writes: UiWriteQueue, progress: StateFlow<List<AchievementProgress>>, profiles: StateFlow<UserProfileEntity?>) : ViewModel() {
    private val preferences = context.getSharedPreferences("journey_celebrations", Context.MODE_PRIVATE)
    private val queue = CelebrationQueue()
    private val _current = MutableStateFlow<JourneyCelebration?>(null)
    val current = _current.asStateFlow()
    val achievements = progress
    init {
        scope.launch {
            progress.collect { rows ->
                if (rows.isEmpty()) return@collect
                val unlocked = rows.filter { it.achievement.isUnlocked }.map { it.achievement.id }.toSet()
                val seen = preferences.getStringSet("seen", emptySet()).orEmpty().toSet()
                if (preferences.getBoolean("initialized", false)) {
                    (unlocked - seen).forEach { id -> queue.offer("badge:$id", JourneyCelebration(badgeId = id)) }
                }
                preferences.edit().putStringSet("seen", seen + unlocked).putBoolean("initialized", true).apply()
                _current.value = queue.peek()
            }
        }
        scope.launch {
            profiles.filterNotNull().distinctUntilChangedBy { it.currentLevel }.collect { profile ->
                val previous = preferences.getInt("level", 0)
                if (previous > 0 && profile.currentLevel > previous) queue.offer("level:${profile.currentLevel}", JourneyCelebration(level = profile.currentLevel))
                preferences.edit().putInt("level", maxOf(previous, profile.currentLevel)).apply()
                _current.value = queue.peek()
            }
        }
    }
    fun dismiss() { queue.remove(); _current.value = queue.peek() }
    fun resetPresentation() { preferences.edit().clear().apply(); while (queue.peek() != null) queue.remove(); _current.value = null }
    fun pin(id: String) { writes.launch {
        check(database.achievementDao().getAll().any { it.id == id && it.isUnlocked }) { "Unlock a badge before pinning it." }
        val settings = database.settingsDao().get() ?: AppSettingsEntity()
        val pins = settings.pinnedBadges.split(',').filter { it.isNotBlank() }.toMutableList()
        if (id in pins) pins.remove(id) else { require(pins.size < 3) { "Choose up to three pinned badges." }; pins.add(id) }
        database.settingsDao().save(settings.copy(pinnedBadges = pins.joinToString(",")))
    } }
}