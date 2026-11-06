package com.mustafacanyucel.firefly_shortcuts.inbox

import android.content.Context
import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.sms.SmsParseResult
import com.mustafacanyucel.firefly_shortcuts.sms.matchSms
import com.mustafacanyucel.firefly_shortcuts.ui.sms.label

/** Turns incoming SMS into bank transactions to review, or sends them right away. */
class BankMessageInbox(context: Context, private val database: AppDatabase) {
    private val context = context.applicationContext

    /**
     * Stores what a template reads from [body], then asks for a review or sends it. Anything
     * else, including messages an ignore template recognises, is dropped without a trace.
     * Returns whether something was stored.
     */
    suspend fun handle(sender: String, body: String, receivedAt: Long): Boolean {
        val templates = database.smsTemplateDao().getAll().map { it.template }
        val match = matchSms(sender, body, templates) ?: return false
        val sms = when (val result = match.result) {
            is SmsParseResult.Parsed -> result.sms
            is SmsParseResult.BadField -> {
                BankNotifications.unreadable(
                    context, sender, match.template.name,
                    "the ${result.kind.label.lowercase()} \"${result.text}\" can't be read. Check its pattern.",
                )
                return false
            }
            else -> return false
        }
        val dao = database.bankMessageDao()
        val draft = BankMessageEntity(
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
        val message = draft.copy(id = dao.insert(draft))

        val review = if (match.template.autoPost) automaticReview(message) else null
        if (review != null) {
            BankPostWorker.send(context, message.id, review, announce = true)
        } else {
            BankNotifications.review(context, message)
        }
        return true
    }

    /**
     * The choices for sending [message] without asking, or null when something needs the user:
     * no amount or merchant, a card without a known account, or an amount in another currency.
     */
    private suspend fun automaticReview(message: BankMessageEntity): BankReview? {
        val amount = message.amount ?: return null
        val merchant = message.merchant?.ifBlank { null } ?: return null
        val card = message.cardEnding ?: return null
        val accountId = database.bankMessageDao().accountForCard(card) ?: return null
        val account = database.accountDao().getAll().firstOrNull { it.id == accountId } ?: return null
        if (isForeign(message, account)) return null
        return BankReview(accountId = account.id, amount = amount, counterparty = merchant)
    }
}
