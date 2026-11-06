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
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

private val SENDERS = listOf("YAPIKREDI", "927457334")

/** Real messages from one bank, read by the templates a user would build for them. */
class BankSmsSamplesTest {
    private val cardPurchase = SmsTemplate(
        name = "Card purchase",
        senders = SENDERS,
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

    /** These messages don't say the amount. */
    private val onlinePurchase = SmsTemplate(
        name = "Online purchase",
        senders = SENDERS,
        parts = listOf(
            Field(CARD_ENDING),
            Text(" ile biten kartinizla "),
            Field(MERCHANT),
            Text(" firmasinda "),
            Field(DATE_TIME, "dd.MM.yyyy HH:mm"),
            Text(" tarihinde islem yapilmistir. Isleme kart hareketlerinden ulasabilirsiniz. "),
            Field(IGNORE),
        ),
    )

    /** Nothing was spent. The reason is ignored too, so other reasons are caught as well. */
    private val declined = SmsTemplate(
        name = "Declined",
        senders = SENDERS,
        action = SmsAction.IGNORE,
        parts = listOf(
            Field(IGNORE),
            Text(" firmasindan yapmak istediginiz "),
            Field(IGNORE),
            Text(" tutarindaki islem, "),
            Field(IGNORE),
            Text(" onaylanmamistir."),
            Field(IGNORE),
        ),
    )

    /**
     * Money back on the card. The message has two amounts: the original purchase (ignored) and the
     * refunded part. This bank writes a decimal point here but a decimal comma in purchases.
     */
    private val refund = SmsTemplate(
        name = "Refund",
        senders = SENDERS,
        action = SmsAction.DEPOSIT,
        decimalMark = '.',
        parts = listOf(
            Text("Degerli Musterimiz, "),
            Field(CARD_ENDING),
            Text(" ile biten MASTER DIJITAL KART kartinizla "),
            Field(DATE, "dd.MM.yyyy"),
            Text(" tarihinde "),
            Field(MERCHANT),
            Text(" firmasindan yaptiginiz "),
            Field(IGNORE),
            Text(" isleminiz icin "),
            Field(AMOUNT),
            Text(" "),
            Field(CURRENCY),
            Text(" kartiniza iade edilmistir. "),
            Field(IGNORE),
        ),
    )

    private val templates = listOf(cardPurchase, onlinePurchase, declined, refund)

    private fun expected(
        amount: String?,
        currency: String?,
        date: String,
        time: String?,
        merchant: String,
        card: String,
        action: SmsAction = SmsAction.WITHDRAWAL,
    ) = SmsParseResult.Parsed(
        ParsedSms(
            action = action,
            amount = amount,
            currency = currency,
            date = LocalDate.parse(date),
            time = time?.let(LocalTime::parse),
            merchant = merchant,
            cardEnding = card,
        )
    )

    private val samples = listOf(
        "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 31.10.2026 saat 21:48'de,GASTROBIL GIDA is yerinden 1.590,00 TL islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. B002"
            to expected("1590.00", "TL", "2026-10-31", "21:48", "GASTROBIL GIDA", "1111"),
        "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 16.10.2026 saat 19:40'de,ANKARA CATERING VE R is yerinden 140,00 TL islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. B002"
            to expected("140.00", "TL", "2026-10-16", "19:40", "ANKARA CATERING VE R", "1111"),
        "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 10.10.2026 saat 13:47'de,Avolta BCN M4 Express Sho is yerinden 19,65 EUR islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. B001"
            to expected("19.65", "EUR", "2026-10-10", "13:47", "Avolta BCN M4 Express Sho", "1111"),
        "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 09.10.2026 saat 18:40'de,ALEHOP 145 BARCELONA is yerinden 8,00 EUR islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. B001"
            to expected("8.00", "EUR", "2026-10-09", "18:40", "ALEHOP 145 BARCELONA", "1111"),
        "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 07.10.2026 saat 15:24'de,LASEN ACCESORIOS is yerinden 25,00 EUR islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. B016"
            to expected("25.00", "EUR", "2026-10-07", "15:24", "LASEN ACCESORIOS", "1111"),
        "2222 ile biten kartinizla GOOGLE *Duolingo Learn firmasinda 25.10.2026 20:40 tarihinde islem yapilmistir. Isleme kart hareketlerinden ulasabilirsiniz. B002"
            to expected(null, null, "2026-10-25", "20:40", "GOOGLE *Duolingo Learn", "2222"),
        "3333 ile biten World Platinum kartinizla 25.10.2026 20:40 tarihinde Google Duolingo Learn firmasindan yapmak istediginiz 329,99 TL tutarindaki islem, eski kart ile denediginiz icin onaylanmamistir.Islemi yeni kartiniz ile tekrar deneyebilirsiniz. B002"
            to SmsParseResult.Ignored,
        "3333 ile biten World Platinum kartinizla 21.10.2026 16:40 tarihinde APPLE.COM/BILL firmasindan yapmak istediginiz 89,99 TL tutarindaki islem, eski kart ile denediginiz icin onaylanmamistir.Islemi yeni kartiniz ile tekrar deneyebilirsiniz. B002"
            to SmsParseResult.Ignored,
        "Degerli Musterimiz, 2222 ile biten MASTER DIJITAL KART kartinizla 17.10.2026 tarihinde IYZICO *AMAZON.COM.T firmasindan yaptiginiz 1191.01 TL isleminiz icin 215.91 TL kartiniza iade edilmistir. B002"
            to expected("215.91", "TL", "2026-10-17", null, "IYZICO *AMAZON.COM.T", "2222", SmsAction.DEPOSIT),
    )

    @Test
    fun templatesHaveNoProblems() {
        templates.forEach { assertEquals(it.name, emptyList<TemplateProblem>(), it.problems()) }
    }

    @Test
    fun readsEverySample() {
        samples.forEach { (body, result) -> assertEquals(body, result, parseSms("YAPIKREDI", body, templates)) }
    }

    @Test
    fun eachSampleMatchesExactlyOneTemplate() {
        samples.forEach { (body, _) ->
            val matching = templates.filter { it.parse(body) != SmsParseResult.NoMatch }.map { it.name }
            assertEquals(body, 1, matching.size)
        }
    }
}
