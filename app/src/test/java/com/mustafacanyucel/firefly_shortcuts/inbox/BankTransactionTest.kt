package com.mustafacanyucel.firefly_shortcuts.inbox

import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BankTransactionTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")
    private val card = AccountEntity("7", "World Platinum", "asset", "ccAsset", "TRY", "₺", true)

    private fun message(
        action: SmsAction = SmsAction.WITHDRAWAL,
        currency: String? = "TL",
        date: LocalDate? = LocalDate.of(2026, 11, 2),
        time: LocalTime? = LocalTime.of(12, 18),
        arrived: ZonedDateTime = ZonedDateTime.of(2026, 11, 2, 12, 18, 40, 0, istanbul),
    ) = BankMessageEntity(
        receivedAt = arrived.toInstant().toEpochMilli(),
        sender = "YAPIKREDI",
        templateName = "Card purchase",
        action = action,
        amount = "644.00",
        currency = currency,
        date = date,
        time = time,
        merchant = "V A N T A",
        cardEnding = "1111",
    )

    @Test
    fun readsCurrencyNames() {
        assertEquals("TRY", currencyCode("TL"))
        assertEquals("TRY", currencyCode(" tl "))
        assertEquals("EUR", currencyCode("EUR"))
        assertEquals("EUR", currencyCode("€"))
        assertNull(currencyCode("Lira"))
        assertNull(currencyCode(null))
    }

    @Test
    fun tellsForeignCurrencies() {
        assertFalse(isForeign(message(currency = "TL"), card))
        assertTrue(isForeign(message(currency = "EUR"), card))
        // Nothing to compare: treated as the account's own currency.
        assertFalse(isForeign(message(currency = null), card))
        assertFalse(isForeign(message(currency = "EUR"), card.copy(currencyCode = null)))
    }

    @Test
    fun usesTheSmsDateAndTime() {
        assertEquals("2026-11-02T12:18+03:00", bankTransactionTime(message(), istanbul).toString())
    }

    @Test
    fun dateOnlyGetsTheArrivalTimeThatDayElseNoon() {
        assertEquals(
            "2026-11-02T12:18:40+03:00",
            bankTransactionTime(message(time = null), istanbul).toString(),
        )
        val nextDay = ZonedDateTime.of(2026, 11, 3, 8, 0, 0, 0, istanbul)
        assertEquals(
            "2026-11-02T12:00+03:00",
            bankTransactionTime(message(time = null, arrived = nextDay), istanbul).toString(),
        )
        assertEquals(
            "2026-11-03T08:00+03:00",
            bankTransactionTime(message(date = null, time = null, arrived = nextDay), istanbul).toString(),
        )
    }

    @Test
    fun withdrawalGoesFromTheCardToTheMerchant() {
        val split = bankTransactionRequest(
            message(), card, BankReview("7", "644.00", "V A N T A", tags = listOf("food")), istanbul,
        ).transactions.single()
        assertEquals("withdrawal", split.type)
        assertEquals("2026-11-02T12:18:00+03:00", split.date)
        assertEquals("7", split.sourceId)
        assertNull(split.destinationId)
        assertEquals("V A N T A", split.destinationName)
        assertEquals("V A N T A", split.description)
        assertEquals(listOf("food"), split.tags)
        assertNull(split.foreignAmount)
    }

    @Test
    fun depositGoesFromTheMerchantToTheCard() {
        val split = bankTransactionRequest(
            message(action = SmsAction.DEPOSIT), card, BankReview("7", "215.91", "IYZICO *AMAZON.COM.T"), istanbul,
        ).transactions.single()
        assertEquals("deposit", split.type)
        assertNull(split.sourceId)
        assertEquals("IYZICO *AMAZON.COM.T", split.sourceName)
        assertEquals("7", split.destinationId)
    }

    @Test
    fun foreignAmountTravelsWithItsCurrency() {
        val review = BankReview("7", "720.40", "Avolta", foreignAmount = "19.65", foreignCurrencyCode = "EUR")
        val split = bankTransactionRequest(message(currency = "EUR"), card, review, istanbul).transactions.single()
        assertEquals("720.40", split.amount)
        assertEquals("19.65", split.foreignAmount)
        assertEquals("EUR", split.foreignCurrencyCode)
    }
}
