package com.mustafacanyucel.firefly_shortcuts.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountTest {
    @Test
    fun keepsPlainAmounts() {
        assertEquals("12", normalizeAmount("12"))
        assertEquals("12.50", normalizeAmount(" 12.50 "))
    }

    @Test
    fun acceptsDecimalComma() {
        assertEquals("644.00", normalizeAmount("644,00"))
    }

    @Test
    fun dropsThousandsSeparators() {
        assertEquals("1234.56", normalizeAmount("1.234,56"))
        assertEquals("1234.56", normalizeAmount("1,234.56"))
        assertEquals("1234567", normalizeAmount("1.234.567"))
    }

    @Test
    fun rejectsZeroNegativeAndText() {
        listOf("", "0", "0,00", "-5", "12a").forEach { assertNull(it, normalizeAmount(it)) }
    }
}
