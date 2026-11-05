package com.mustafacanyucel.firefly_shortcuts.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType

val TransactionType.label: String
    get() = when (this) {
        TransactionType.WITHDRAWAL -> "Withdrawal"
        TransactionType.DEPOSIT -> "Deposit"
        TransactionType.TRANSFER -> "Transfer"
    }

val TransactionType.iconRes: Int
    get() = when (this) {
        TransactionType.WITHDRAWAL -> R.drawable.ic_arrow_upward
        TransactionType.DEPOSIT -> R.drawable.ic_arrow_downward
        TransactionType.TRANSFER -> R.drawable.ic_swap_horiz
    }

val TransactionType.color: Color
    @Composable get() = when (this) {
        TransactionType.WITHDRAWAL -> MaterialTheme.colorScheme.error
        TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
    }
