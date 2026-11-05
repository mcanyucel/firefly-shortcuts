package com.mustafacanyucel.firefly_shortcuts.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

const val AMOUNT_ERROR = "Enter an amount above zero, like 12.50"

/**
 * An amount input with the number keyboard. Accepts a decimal comma too
 * ("12,50"); callers turn it into Firefly's format with normalizeAmount.
 */
@Composable
fun AmountField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isError: Boolean,
    currency: String?,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = currency?.let { { Text(it) } },
        isError = isError,
        supportingText = if (isError) { { Text(AMOUNT_ERROR) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Text field state starting at [initial], all selected so typing replaces it. */
@Composable
fun rememberAmountState(initial: String): MutableState<TextFieldValue> =
    rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length)))
    }
