package com.mustafacanyucel.firefly_shortcuts.sms

import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.AMOUNT
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.CARD_ENDING
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.CURRENCY
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.DATE
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.DATE_TIME
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.IGNORE
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.MERCHANT
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind.TIME
import com.mustafacanyucel.firefly_shortcuts.sms.TemplatePart.Field
import com.mustafacanyucel.firefly_shortcuts.sms.TemplatePart.Text
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SmsParserTest {
    private val realSms = "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 02.11.2026 " +
        "saat 12:18'de,V A N T A is yerinden 644,00 TL islem yapilmistir. Islem size ait degilse " +
        "bankamizla iletisime gecebilirsiniz. B002"

    private val purchase = SmsTemplate(
        name = "Card purchase",
        senders = listOf("BANK"),
        parts = listOf(
            Text("Degerli Musterimiz, "),
            Field(CARD_ENDING),
            Text(" ile biten World Platinum kartinizla "),
            Field(DATE, "dd.MM.yyyy"),
            Text(" saat "),
            Field(TIME, "HH:mm"),
            Text("'de,"),
            Field(MERCHANT),
            Text(" is yerinden "),
            Field(AMOUNT),
            Text(" "),
            Field(CURRENCY),
            Text(" islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. "),
            Field(IGNORE),
        ),
    )

    private fun parsed(result: SmsParseResult?): ParsedSms {
        assertTrue("expected a parsed SMS, got $result", result is SmsParseResult.Parsed)
        return (result as SmsParseResult.Parsed).sms
    }

    @Test
    fun readsTheRealSms() {
        val sms = parsed(purchase.parse(realSms))
        assertEquals(SmsAction.WITHDRAWAL, sms.action)
        assertEquals("644.00", sms.amount)
        assertEquals("TL", sms.currency)
        assertEquals(LocalDate.of(2026, 11, 2), sms.date)
        assertEquals(LocalTime.of(12, 18), sms.time)
        assertEquals("V A N T A", sms.merchant)
        assertEquals("1111", sms.cardEnding)
    }

    @Test
    fun readsAnotherPurchaseWithTheSameTemplate() {
        val body = realSms
            .replace("1111", "4410")
            .replace("02.11.2026", "31.12.2026")
            .replace("12:18", "09:05")
            .replace("V A N T A", "MIGROS SANAL MARKET")
            .replace("644,00", "1.234,56")
            .replace("B002", "B417")
        val sms = parsed(purchase.parse(body))
        assertEquals("1234.56", sms.amount)
        assertEquals(LocalDate.of(2026, 12, 31), sms.date)
        assertEquals(LocalTime.of(9, 5), sms.time)
        assertEquals("MIGROS SANAL MARKET", sms.merchant)
        assertEquals("4410", sms.cardEnding)
    }

    @Test
    fun toleratesLineBreaksAndExtraSpaces() {
        val body = "  " + realSms.replace(" saat ", "\nsaat  ").replace(" islem ", "\r\nislem ") + "\n"
        assertEquals("644.00", parsed(purchase.parse(body)).amount)
    }

    @Test
    fun ignoresOtherMessages() {
        assertEquals(SmsParseResult.NoMatch, purchase.parse("Your code is 123456"))
        assertEquals(SmsParseResult.NoMatch, purchase.parse(realSms.replace("is yerinden", "ATM'den")))
        assertNull(parseSms("MOM", realSms, listOf(purchase)))
        assertNull(parseSms("BANK", "Kampanya! Puanlarinizi kullanin.", listOf(purchase)))
    }

    @Test
    fun matchesTheSenderLoosely() {
        assertTrue(parseSms(" bank ", realSms, listOf(purchase)) is SmsParseResult.Parsed)
    }

    @Test
    fun matchesAnyOfTheSenders() {
        val template = purchase.copy(senders = listOf("YAPIKREDI", "927457334"))
        assertTrue(parseSms("YAPIKREDI", realSms, listOf(template)) is SmsParseResult.Parsed)
        assertTrue(parseSms("927457334", realSms, listOf(template)) is SmsParseResult.Parsed)
        assertNull(parseSms("BANK", realSms, listOf(template)))
    }

    @Test
    fun reportsAFieldThatCantBeRead() {
        assertEquals(
            SmsParseResult.BadField(DATE, "32.13.2026"),
            purchase.parse(realSms.replace("02.11.2026", "32.13.2026")),
        )
    }

    @Test
    fun firstMatchingTemplateWins() {
        val refund = SmsTemplate(
            name = "Refund",
            senders = listOf("BANK"),
            action = SmsAction.IGNORE,
            parts = listOf(Text("Degerli Musterimiz,"), Field(IGNORE), Text("iade edilmistir."), Field(IGNORE)),
        )
        val refundSms = "Degerli Musterimiz, 1111 ile biten kartiniza 644,00 TL iade edilmistir. B003"
        assertEquals(SmsParseResult.Ignored, parseSms("BANK", refundSms, listOf(purchase, refund)))
        assertTrue(parseSms("BANK", realSms, listOf(refund, purchase)) is SmsParseResult.Parsed)
    }

    @Test
    fun decimalMarkSettlesAmbiguousAmounts() {
        val template = SmsTemplate(
            name = "Short",
            senders = listOf("BANK"),
            parts = listOf(Text("Spent "), Field(AMOUNT), Field(CURRENCY), Text(" at "), Field(MERCHANT)),
        )
        // Amount and currency can touch: "1.234TL".
        assertEquals("1.234", parsed(template.parse("Spent 1.234TL at Shop")).amount)
        val comma = template.copy(decimalMark = ',')
        assertEquals("1234", parsed(comma.parse("Spent 1.234TL at Shop")).amount)
        assertEquals("1234.5", parsed(comma.parse("Spent 1.234,5TL at Shop")).amount)
        assertEquals("TL", parsed(comma.parse("Spent 1.234,5TL at Shop")).currency)
    }

    @Test
    fun readsADateAndTimeField() {
        val template = SmsTemplate(
            name = "Combined",
            senders = listOf("BANK"),
            parts = listOf(Text("On "), Field(DATE_TIME, "d/M/yy H:mm"), Text(" you paid "), Field(AMOUNT)),
        )
        val sms = parsed(template.parse("On 2/11/26 7:05 you paid 12.50"))
        assertEquals(LocalDate.of(2026, 11, 2), sms.date)
        assertEquals(LocalTime.of(7, 5), sms.time)
        assertEquals("12.50", sms.amount)
    }

    @Test
    fun realTemplateHasNoProblems() {
        assertEquals(emptyList<TemplateProblem>(), purchase.problems())
    }

    @Test
    fun findsProblems() {
        fun check(t: SmsTemplate) = t.problems()
        assertEquals(listOf(TemplateProblem.NO_SENDER), check(purchase.copy(senders = listOf(" "))))
        assertEquals(
            listOf(TemplateProblem.NO_FIXED_TEXT),
            check(purchase.copy(parts = listOf(Field(AMOUNT), Text(" "), Field(MERCHANT)))),
        )
        assertEquals(
            listOf(TemplateProblem.DUPLICATE_FIELD),
            check(purchase.copy(parts = purchase.parts + Text(" ") + Field(MERCHANT))),
        )
        assertEquals(
            listOf(TemplateProblem.DATE_TWICE),
            check(purchase.copy(parts = purchase.parts + Text(" ") + Field(DATE_TIME, "dd.MM.yyyy HH:mm"))),
        )
        // A date without a year, a time pattern in a date field, a missing or invalid pattern.
        listOf(Field(DATE, "dd.MM"), Field(DATE, "HH:mm"), Field(DATE), Field(DATE, "bb")).forEach { bad ->
            val parts = purchase.parts.map { if (it == Field(DATE, "dd.MM.yyyy")) bad else it }
            assertEquals(bad.toString(), listOf(TemplateProblem.BAD_DATE_FORMAT), check(purchase.copy(parts = parts)))
        }
        // No amount is fine: some messages leave it out.
        assertEquals(
            emptyList<TemplateProblem>(),
            check(purchase.copy(parts = purchase.parts.filterNot { it == Field(AMOUNT) })),
        )
        assertEquals(
            emptyList<TemplateProblem>(),
            check(SmsTemplate("Ads", listOf("BANK"), listOf(Text("Kampanya"), Field(IGNORE)), SmsAction.IGNORE)),
        )
    }

    @Test
    fun templateWithProblemsNeverMatches() {
        assertEquals(SmsParseResult.NoMatch, purchase.copy(senders = emptyList()).parse(realSms))
    }

    @Test
    fun survivesJsonRoundTrip() {
        val json = Json.encodeToString(SmsTemplate.serializer(), purchase.copy(decimalMark = ','))
        assertEquals(purchase.copy(decimalMark = ','), Json.decodeFromString(SmsTemplate.serializer(), json))
    }
}
