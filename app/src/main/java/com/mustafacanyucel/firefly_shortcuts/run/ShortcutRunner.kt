package com.mustafacanyucel.firefly_shortcuts.run

import com.mustafacanyucel.firefly_shortcuts.data.api.FireflyApi
import com.mustafacanyucel.firefly_shortcuts.data.api.toTransactionRequest
import com.mustafacanyucel.firefly_shortcuts.data.api.transactionDate
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDao
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail

/** Creates a shortcut's transaction in Firefly. Used by the app and, later, the widget. */
class ShortcutRunner(
    private val api: FireflyApi,
    private val dao: ShortcutDao,
) {
    /**
     * Throws if Firefly doesn't confirm it. A caller that retries must pass the
     * same [date] each time, so Firefly can tell a retry from a new transaction.
     */
    suspend fun run(item: ShortcutDetail, date: String = transactionDate()) {
        api.createTransaction(item.toTransactionRequest(date))
        dao.updateLastUsed(item.shortcut.id, System.currentTimeMillis())
    }
}
