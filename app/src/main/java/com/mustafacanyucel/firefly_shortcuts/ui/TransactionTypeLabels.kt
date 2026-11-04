package com.mustafacanyucel.firefly_shortcuts.ui

import com.mustafacanyucel.firefly_shortcuts.model.TransactionType

val TransactionType.label: String
    get() = when (this) {
        TransactionType.WITHDRAWAL -> "Withdrawal"
        TransactionType.DEPOSIT -> "Deposit"
        TransactionType.TRANSFER -> "Transfer"
    }
