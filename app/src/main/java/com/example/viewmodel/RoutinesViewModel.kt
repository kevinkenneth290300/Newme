package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.RoutineTemplateEntity
import com.example.data.reminder.ReminderRepository
import com.example.data.routine.RoutinesRepository
import com.example.util.UiWriteQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.time.Clock

class RoutinesViewModel(
    private val database: NewMeDatabase,
    private val application: Application,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val writes: UiWriteQueue
) : ViewModel() {
    private val repository = RoutinesRepository(database, clock)
    private val reminders = ReminderRepository(database, clock)
    val templates = repository.templates.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val schedules = database.reminderDao().observe().stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    fun quests(date: String) = repository.quests(date)
    fun toggle(id: Long, date: String, completed: Boolean) { writes.launch { repository.complete(id, date, completed) } }
    fun delete(id: Long) { writes.launch { repository.delete(id) } }
    fun move(id: Long, direction: Int) { writes.launch { repository.move(id, direction) } }
    fun applyPacks(ids: Set<String>, onSaved: () -> Unit = {}) { writes.launch { repository.applyPacks(application, ids); onSaved() } }
    fun save(template: RoutineTemplateEntity, remind: Boolean, minute: Int, sensitive: Boolean, onSaved: () -> Unit) {
        if (_saving.value || writes.isMaintaining) return
        _saving.value = true
        writes.launch {
            try {
                database.withTransaction {
                    val id = repository.save(template)
                    reminders.save("routine:$id", minute, remind, sensitive, routineId = id)
                }
                onSaved()
            } finally { _saving.value = false }
        }
    }
}