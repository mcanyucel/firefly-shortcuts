package com.mustafacanyucel.firefly_shortcuts.ui

import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail

/** The amount with the currency of whichever account has one. */
val ShortcutDetail.amountText: String
    get() {
        val currency = listOf(fromAccount, toAccount)
            .firstNotNullOfOrNull { it.currencySymbol ?: it.currencyCode }
        return if (currency == null) shortcut.amount else "${shortcut.amount} $currency"
    }
