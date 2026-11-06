package com.mustafacanyucel.firefly_shortcuts.ui.sms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction

val FieldKind.label: String
    get() = when (this) {
        FieldKind.AMOUNT -> "Amount"
        FieldKind.CURRENCY -> "Currency"
        FieldKind.DATE -> "Date"
        FieldKind.TIME -> "Time"
        FieldKind.DATE_TIME -> "Date and time"
        FieldKind.MERCHANT -> "Merchant"
        FieldKind.CARD_ENDING -> "Card ending"
        FieldKind.IGNORE -> "Ignore"
    }

val SmsAction.label: String
    get() = when (this) {
        SmsAction.WITHDRAWAL -> "Withdrawal"
        SmsAction.DEPOSIT -> "Deposit"
        SmsAction.IGNORE -> "Ignore"
    }

/**
 * Background and text color of a marked piece. Related fields share a color; the amount is the
 * strongest, and Ignore is plain grey.
 */
val FieldKind.colors: Pair<Color, Color>
    @Composable get() {
        val c = MaterialTheme.colorScheme
        return when (this) {
            FieldKind.AMOUNT, FieldKind.CURRENCY -> c.primary to c.onPrimary
            FieldKind.DATE, FieldKind.TIME, FieldKind.DATE_TIME -> c.tertiaryContainer to c.onTertiaryContainer
            FieldKind.MERCHANT, FieldKind.CARD_ENDING -> c.secondaryContainer to c.onSecondaryContainer
            FieldKind.IGNORE -> c.outlineVariant to c.onSurface
        }
    }
