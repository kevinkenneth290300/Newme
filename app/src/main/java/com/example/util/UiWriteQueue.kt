package com.example.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class UiWriteQueue(private val scope: CoroutineScope, private val onError: (String) -> Unit, private val afterWrite: suspend () -> Unit = {}) {
    private val pending = mutableSetOf<Job>()
    private val maintenance = AtomicBoolean(false)
    val isMaintaining: Boolean get() = maintenance.get()

    fun launch(block: suspend () -> Unit): Job {
        if (isMaintaining) {
            onError("Wait for the backup, restore, or reset to finish before changing records.")
            return Job().apply { complete() }
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
                afterWrite()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError("Could not save locally. Please try again; existing records were kept.")
            }
        }
        synchronized(pending) { pending.add(job) }
        job.invokeOnCompletion { synchronized(pending) { pending.remove(job) } }
        job.start()
        return job
    }

    suspend fun awaitIdle() {
        while (true) {
            val jobs = synchronized(pending) { pending.toList() }
            if (jobs.isEmpty()) return
            jobs.joinAll()
        }
    }

    suspend fun withMaintenance(block: suspend () -> Unit) {
        check(maintenance.compareAndSet(false, true)) { "Another local data operation is in progress." }
        try {
            awaitIdle()
            block()
        } finally {
            maintenance.set(false)
        }
    }
}