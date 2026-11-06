package com.mustafacanyucel.firefly_shortcuts.inbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.mustafacanyucel.firefly_shortcuts.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives every incoming SMS (GitHub build only; declared in src/github). Android wakes the app
 * for it even when it's closed. Messages that no template recognises are dropped here and never
 * stored.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)?.filterNotNull().orEmpty()
        if (parts.isEmpty()) return
        // A long SMS arrives in several parts.
        val messages = parts.groupBy { it.originatingAddress.orEmpty() }
            .mapValues { (_, pieces) -> pieces.joinToString("") { it.messageBody.orEmpty() } }
        val receivedAt = parts.first().timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

        val container = context.appContainer
        messages.forEach { (sender, body) -> container.smsCapture.offer(sender, body) }

        // Reading the templates needs the database, so finish off the main thread.
        val pending = goAsync()
        container.appScope.launch(Dispatchers.IO) {
            try {
                messages.forEach { (sender, body) -> container.bankMessages.handle(sender, body, receivedAt) }
            } finally {
                pending.finish()
            }
        }
    }
}
