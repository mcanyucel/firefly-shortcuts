package com.mustafacanyucel.firefly_shortcuts.run

import com.mustafacanyucel.firefly_shortcuts.data.api.FireflyApi
import com.mustafacanyucel.firefly_shortcuts.data.api.toTransactionRequest
import com.mustafacanyucel.firefly_shortcuts.data.api.transactionDate
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDao
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutEntity
import com.mustafacanyucel.firefly_shortcuts.model.normalizeAmount

/** Creates a shortcut's transaction in Firefly. Used by the app and the widget. */
class ShortcutRunner(
    private val api: FireflyApi,
    private val dao: ShortcutDao,
) {
    /**
     * Throws if Firefly doesn't confirm it. A caller that retries must pass the
     * same [date] and [amount] each time, so Firefly can tell a retry from a new
     * transaction.
     *
     * [amount] is the typed amount for a shortcut that asks for one; it's
     * ignored for the others.
     */
    suspend fun run(item: ShortcutDetail, date: String = transactionDate(), amount: String? = null) {
        val value = runAmount(item.shortcut, amount)
            ?: throw IllegalArgumentException("Enter an amount above zero")
        api.createTransaction(item.toTransactionRequest(date, value))
        dao.updateLastUsed(item.shortcut.id, System.currentTimeMillis())
    }
}

/**
 * The amount to send, or null if there's no valid one. A shortcut that asks
 * for the amount only uses [typed], never its saved suggestion, so a missing
 * amount can't silently post the suggestion.
 */
internal fun runAmount(shortcut: ShortcutEntity, typed: String?): String? =
    normalizeAmount(if (shortcut.askAmount) typed.orEmpty() else shortcut.amount)
