package com.mustafacanyucel.firefly_shortcuts.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateBuilderTest {
    private val sample = "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 31.10.2026 saat " +
        "21:48'de,V A N T A is yerinden 1.590,00 TL islem yapilmistir. Islem size ait degilse " +
        "bankamizla iletisime gecebilirsiniz. B002"
    private val pieces = splitSample(sample)

    private fun index(text: String, from: Int = 0) =
        (from until pieces.size).first { pieces[it].text == text }

    @Test
    fun splitsIntoNumbersWordsAndSymbols() {
        val texts = pieces.map { it.text }
        // Numbers keep their separators; "12:18'de,V" splits at the apostrophe and comma.
        assertTrue(texts.containsAll(listOf("1111", "31.10.2026", "21:48", "1.590,00", "B", "002")))
        val time = index("21:48")
        assertEquals(listOf("21:48", "'", "de", ",", "V", "A"), texts.subList(time, time + 6))
        assertTrue(pieces[time].spaceBefore)
        assertFalse(pieces[time + 1].spaceBefore)
        assertTrue(pieces[time + 5].spaceBefore)
    }

    private val marks = listOf(
        FieldMark(index("1111"), index("1111"), FieldKind.CARD_ENDING),
        FieldMark(index("31.10.2026"), index("31.10.2026"), FieldKind.DATE, "dd.MM.yyyy"),
        FieldMark(index("21:48"), index("21:48"), FieldKind.TIME, "HH:mm"),
        FieldMark(index("V"), index("A", index("V") + 3), FieldKind.MERCHANT),
        FieldMark(index("1.590,00"), index("1.590,00"), FieldKind.AMOUNT),
        FieldMark(index("TL"), index("TL"), FieldKind.CURRENCY),
        FieldMark(index("B"), index("002"), FieldKind.IGNORE),
    )

    private val template = SmsTemplate(
        name = "Card purchase",
        senders = listOf("YAPIKREDI"),
        parts = buildParts(sample, pieces, marks),
        decimalMark = ',',
    )

    @Test
    fun buildsATemplateThatReadsTheSample() {
        assertEquals(
            listOf(
                TemplatePart.Text("Degerli Musterimiz, "),
                TemplatePart.Field(FieldKind.CARD_ENDING),
                TemplatePart.Text(" ile biten World Platinum kartinizla "),
                TemplatePart.Field(FieldKind.DATE, "dd.MM.yyyy"),
                TemplatePart.Text(" saat "),
                TemplatePart.Field(FieldKind.TIME, "HH:mm"),
                TemplatePart.Text("'de,"),
                TemplatePart.Field(FieldKind.MERCHANT),
                TemplatePart.Text(" is yerinden "),
                TemplatePart.Field(FieldKind.AMOUNT),
                TemplatePart.Text(" "),
                TemplatePart.Field(FieldKind.CURRENCY),
                TemplatePart.Text(
                    " islem yapilmistir. Islem size ait degilse bankamizla iletisime gecebilirsiniz. "
                ),
                TemplatePart.Field(FieldKind.IGNORE),
            ),
            template.parts,
        )
        val sms = (template.parse(sample) as SmsParseResult.Parsed).sms
        assertEquals("1590.00", sms.amount)
        assertEquals("V A N T A", sms.merchant)
    }

    @Test
    fun findsTheMarksAgainForASavedTemplate() {
        assertEquals(marks, marksFor(template, sample, pieces))
    }

    @Test
    fun noMarksWhenTheSampleDoesntFit() {
        assertNull(marksFor(template, "Something else", splitSample("Something else")))
    }

    @Test
    fun guessesDateFormats() {
        assertEquals("dd.MM.yyyy", guessDateFormat(FieldKind.DATE, "31.10.2026"))
        assertEquals("d/M/yy", guessDateFormat(FieldKind.DATE, "2/11/26"))
        assertEquals("yyyy-MM-dd", guessDateFormat(FieldKind.DATE, "2026-10-31"))
        assertEquals("HH:mm", guessDateFormat(FieldKind.TIME, "21:48"))
        assertEquals("H:mm:ss", guessDateFormat(FieldKind.TIME, "7:05:09"))
        assertEquals("dd.MM.yyyy HH:mm", guessDateFormat(FieldKind.DATE_TIME, "25.10.2026 20:40"))
        assertEquals("yyyy-MM-dd'T'HH:mm", guessDateFormat(FieldKind.DATE_TIME, "2026-10-25T20:40"))
    }

    @Test
    fun noGuessForOddDates() {
        assertNull(guessDateFormat(FieldKind.DATE, "2 Kas 2026")) // month name
        assertNull(guessDateFormat(FieldKind.DATE, "02.11")) // no year
        assertNull(guessDateFormat(FieldKind.DATE, "31.13.2026")) // no month 13
        assertNull(guessDateFormat(FieldKind.TIME, "9:5"))
        assertNull(guessDateFormat(FieldKind.TIME, "5,00")) // an amount
        assertNull(guessDateFormat(FieldKind.AMOUNT, "12.50"))
    }

    @Test
    fun suggestsWhatDiffersFromAnotherMessage() {
        val other = "Degerli Musterimiz, 1111 ile biten World Platinum kartinizla 10.10.2026 saat " +
            "13:47'de,Avolta BCN M4 Express Sho is yerinden 19,65 EUR islem yapilmistir. Islem size " +
            "ait degilse bankamizla iletisime gecebilirsiniz. B001"
        val suggested = suggestMarks(sample, pieces, splitSample(other))
        assertEquals(
            listOf(
                // The card ending is the same in both, so it isn't found.
                FieldMark(index("31.10.2026"), index("31.10.2026"), FieldKind.DATE, "dd.MM.yyyy"),
                FieldMark(index("21:48"), index("21:48"), FieldKind.TIME, "HH:mm"),
                FieldMark(index("V"), index("A", index("V") + 3), FieldKind.MERCHANT),
                FieldMark(index("1.590,00"), index("1.590,00"), FieldKind.AMOUNT),
                FieldMark(index("TL"), index("TL"), FieldKind.CURRENCY),
                FieldMark(index("002"), index("002"), FieldKind.IGNORE),
            ),
            suggested,
        )
    }

    @Test
    fun keepsAMerchantTogetherWhenAWordMatches() {
        val a = "Spent 5,00 at ANKARA CATERING VE R today"
        val b = "Spent 7,50 at MIGROS VE SANAL today"
        val pieces = splitSample(a)
        val suggested = suggestMarks(a, pieces, splitSample(b))
        val merchant = suggested.single { it.kind == FieldKind.MERCHANT }
        assertEquals("ANKARA CATERING VE R", a.substring(pieces[merchant.first].start, pieces[merchant.last].end))
        assertEquals(listOf(FieldKind.AMOUNT, FieldKind.MERCHANT), suggested.map { it.kind })
    }

    @Test
    fun suggestsNothingForTheSameMessage() {
        assertEquals(emptyList<FieldMark>(), suggestMarks(sample, pieces, pieces))
    }

    @Test
    fun doesntSuggestAFieldThatsAlreadyMarked() {
        val other = sample.replace("1.590,00", "7,50").replace("1111", "2222")
        val suggested = suggestMarks(sample, pieces, splitSample(other), taken = setOf(FieldKind.AMOUNT))
        // The card comes first in the message; the amount is already marked, so it's ignored.
        assertEquals(listOf(FieldKind.CARD_ENDING, FieldKind.IGNORE), suggested.map { it.kind })
    }

    @Test
    fun guessesDecimalMarks() {
        assertEquals(',', guessDecimalMark("644,00"))
        assertEquals(',', guessDecimalMark("1.590,00"))
        assertEquals('.', guessDecimalMark("1191.01"))
        assertEquals('.', guessDecimalMark("1,234.56"))
        assertEquals(',', guessDecimalMark("1.234.567"))
        assertNull(guessDecimalMark("1.234"))
        assertNull(guessDecimalMark("12"))
    }
}
