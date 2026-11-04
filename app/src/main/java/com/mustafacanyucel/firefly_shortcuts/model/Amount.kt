package com.mustafacanyucel.firefly_shortcuts.model

/**
 * The amount as Firefly expects it ("1234.56"), or null if it isn't a positive number.
 * Accepts a decimal comma and thousands separator of the other kind ("1.234,56"). A lone separator
 * is the decimal mark, one used several times ("1.234.567) can only be a thousand separator.
 */
fun normalizeAmount(input: String): String? {
    var text = input.filterNot { it.isWhitespace() }
    val decimalIndex = maxOf(text.lastIndexOf('.'), text.lastIndexOf(','))
    if (decimalIndex >= 0) {
        val mark = text[decimalIndex]
        val thousands = if (mark == '.') ',' else '.'
        text = if (text.count { it == mark } > 1) {
            text.filterNot { it == mark || it == thousands }
        } else {
            text.filterNot { it == thousands }.replace(mark, '.')
        }
    }
    val value = text.toBigDecimalOrNull() ?: return null
    return if (value.signum() > 0) value.toPlainString() else null
}
