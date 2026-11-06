package com.mustafacanyucel.firefly_shortcuts.inbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An incoming SMS, with the sender exactly as Android reports it. */
data class CapturedSms(val sender: String, val body: String)

/**
 * Hands the next incoming SMS, from anyone, to the template editor when the user asks it to wait
 * for one. Kept in memory only, and only between [start] and [stop].
 */
class SmsCapture {
    private val _message = MutableStateFlow<CapturedSms?>(null)
    val message: StateFlow<CapturedSms?> = _message.asStateFlow()

    @Volatile
    var waiting = false
        private set

    fun start() {
        _message.value = null
        waiting = true
    }

    fun stop() {
        waiting = false
        _message.value = null
    }

    /** Called for every incoming SMS. Keeps it only if someone is waiting, and only the first. */
    fun offer(sender: String, body: String) {
        if (!waiting) return
        waiting = false
        _message.value = CapturedSms(sender, body)
    }
}
