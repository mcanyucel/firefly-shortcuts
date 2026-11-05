package com.mustafacanyucel.firefly_shortcuts.run

import com.mustafacanyucel.firefly_shortcuts.data.api.toTransactionRequest
import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutEntity
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunAmountTest {
    private fun shortcut(amount: String, ask: Boolean) = ShortcutEntity(
        id = 1,
        name = "Burger",
        amount = amount,
        fromAccountId = "1",
        toAccountId = "2",
        transactionType = TransactionType.WITHDRAWAL,
        askAmount = ask,
    )

    @Test
    fun fixedShortcutUsesItsOwnAmount() {
        assertEquals("12.50", runAmount(shortcut("12.50", ask = false), typed = null))
        // A typed amount can't change a fixed shortcut.
        assertEquals("12.50", runAmount(shortcut("12.50", ask = false), typed = "99"))
    }

    @Test
    fun askingShortcutUsesTheTypedAmount() {
        assertEquals("644.00", runAmount(shortcut("", ask = true), typed = "644,00"))
        assertEquals("7", runAmount(shortcut("5", ask = true), typed = "7"))
    }

    @Test
    fun askingShortcutNeverFallsBackToTheSuggestion() {
        assertNull(runAmount(shortcut("5", ask = true), typed = null))
        assertNull(runAmount(shortcut("5", ask = true), typed = ""))
        assertNull(runAmount(shortcut("5", ask = true), typed = "0"))
    }

    @Test
    fun requestCarriesTheGivenAmount() {
        val detail = ShortcutDetail(
            shortcut = shortcut("", ask = true),
            fromAccount = AccountEntity("1", "Checking", "asset", null, "EUR", "€", true),
            toAccount = AccountEntity("2", "Burger place", "expense", null, null, null, true),
            tags = emptyList(),
        )
        val split = detail.toTransactionRequest("2026-11-05T12:00:00+03:00", "18.90").transactions.single()
        assertEquals("18.90", split.amount)
        assertEquals("Burger", split.description)
    }
}
