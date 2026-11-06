package com.mustafacanyucel.firefly_shortcuts.inbox

import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.sms.SmsParseResult
import com.mustafacanyucel.firefly_shortcuts.sms.matchSms

/** Turns incoming SMS into pending bank transactions. */
class BankMessageInbox(private val database: AppDatabase) {
    /**
     * Stores what a template reads from [body]. Anything else, including messages an ignore
     * template recognises, is dropped without a trace. Returns whether something was stored.
     */
    suspend fun handle(sender: String, body: String, receivedAt: Long): Boolean {
        val templates = database.smsTemplateDao().getAll().map { it.template }
        val match = matchSms(sender, body, templates) ?: return false
        // TODO(step 4): a recognised message with an unreadable field (BadField) should tell the
        // user instead of being dropped.
        val sms = (match.result as? SmsParseResult.Parsed)?.sms ?: return false
        database.bankMessageDao().insert(
            BankMessageEntity(
                receivedAt = receivedAt,
                sender = sender,
                templateName = match.template.name,
                action = sms.action,
                amount = sms.amount,
                currency = sms.currency,
                date = sms.date,
                time = sms.time,
                merchant = sms.merchant,
                cardEnding = sms.cardEnding,
            )
        )
        return true
    }
}
