package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.local.NewMeDatabase
import com.example.data.moment.*
import com.example.util.UiWriteQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Clock

class MomentsViewModel(database: NewMeDatabase, private val clock: Clock, private val scope: CoroutineScope, private val writes: UiWriteQueue, private val onError: (String) -> Unit) : ViewModel() {
    private val repository = MomentsRepository(database, clock)
    private var coldStart = true
    private var backgroundAt: Long? = null
    private var pendingOpen = false
    private val _current = MutableStateFlow<PreparedMoment?>(null)
    val current = _current.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    val permissionOpen = MutableStateFlow(false)

    fun resumed() {
        if (MomentRules.isNewOpen(clock.millis(), backgroundAt, coldStart)) pendingOpen = true
        coldStart = false
        backgroundAt = null
        consider()
    }
    fun backgrounded() { backgroundAt = clock.millis() }
    fun consider() {
        if (!pendingOpen || _current.value != null || _busy.value) return
        _busy.value = true
        scope.launch {
            try {
                val prepared = repository.prepare(permissionOpen.value)
                if (prepared != null && prepared.card != null) { _current.value = prepared; pendingOpen = false }
                else pendingOpen = false
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { onError("Moments could not be opened. Your saved records were kept."); pendingOpen = false }
            finally { _busy.value = false }
        }
    }
    fun answer(type: String, value: String) {
        val current = _current.value ?: return
        if (_busy.value || permissionOpen.value || writes.isMaintaining) return
        _busy.value = true
        writes.launch { try { _current.value = repository.answer(current.candidate, type, value) } finally { _busy.value = false } }
    }
    fun later() {
        val current = _current.value ?: return
        if (_busy.value || writes.isMaintaining) return
        _busy.value = true
        writes.launch { try { repository.later(current.candidate); _current.value = null } finally { _busy.value = false } }
    }
    fun skip() {
        val current = _current.value ?: return
        if (_busy.value || writes.isMaintaining) return
        _busy.value = true
        writes.launch { try { repository.skip(current.candidate); _current.value = null } finally { _busy.value = false } }
    }
}