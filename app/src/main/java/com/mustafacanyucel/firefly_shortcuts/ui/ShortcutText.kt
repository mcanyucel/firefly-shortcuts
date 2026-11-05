package com.mustafacanyucel.firefly_shortcuts.ui

import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail

/** The currency of whichever account has one, as a symbol if possible. */
val ShortcutDetail.currencyText: String?
    get() = listOf(fromAccount, toAccount).firstNotNullOfOrNull { it.currencySymbol ?: it.currencyCode }

/** The amount with its currency, or "?" for a shortcut that asks for the amount. */
val ShortcutDetail.amountText: String
    get() {
        val amount = if (shortcut.askAmount) "?" else shortcut.amount
        val currency = currencyText
        return if (currency == null) amount else "$amount $currency"
    }
