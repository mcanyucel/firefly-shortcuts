package com.mustafacanyucel.firefly_shortcuts.sms

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A tappable piece of a sample SMS: a number with its separators ("1.590,00", "31.10.2026",
 * "12:18"), a word, or a single other character. Whitespace isn't a piece.
 */
data class Piece(
    val start: Int,
    /** Exclusive. */
    val end: Int,
    val text: String,
    val spaceBefore: Boolean,
)

/** A field marked on pieces [first]..[last] of the sample. */
data class FieldMark(
    val first: Int,
    val last: Int,
    val kind: FieldKind,
    val format: String? = null,
) {
    val range: IntRange get() = first..last
}

private val PIECE = Regex("""\d+(?:[.,:/\-]\d+)*|\p{L}+|\S""")

fun splitSample(sample: String): List<Piece> =
    PIECE.findAll(sample).map { match ->
        val start = match.range.first
        Piece(start, match.range.last + 1, match.value, start > 0 && sample[start - 1].isWhitespace())
    }.toList()

/** The sample's text with [marks] turned into fields and the rest kept as fixed text. */
fun buildParts(sample: String, pieces: List<Piece>, marks: List<FieldMark>): List<TemplatePart> {
    val parts = mutableListOf<TemplatePart>()
    var cursor = 0
    marks.sortedBy { it.first }.forEach { mark ->
        val start = pieces[mark.first].start
        if (start > cursor) parts += TemplatePart.Text(sample.substring(cursor, start))
        parts += TemplatePart.Field(mark.kind, mark.format)
        cursor = pieces[mark.last].end
    }
    if (cursor < sample.length) parts += TemplatePart.Text(sample.substring(cursor))
    return parts
}

/**
 * The marks that would build [template] from [sample], for editing a saved template. Null if the
 * sample no longer lines up with the template. Ignore fields that matched nothing are dropped.
 */
fun marksFor(template: SmsTemplate, sample: String, pieces: List<Piece>): List<FieldMark>? {
    val match = template.toRegex().matchEntire(sample) ?: return null
    val fields = template.parts.filterIsInstance<TemplatePart.Field>()
    return fields.mapIndexedNotNull { index, field ->
        val range = match.groups[index + 1]?.range ?: return null
        if (range.isEmpty()) return@mapIndexedNotNull null
        val first = pieces.indexOfFirst { it.start == range.first }
        val last = pieces.indexOfFirst { it.end == range.last + 1 }
        if (first < 0 || last < first) return null
        FieldMark(first, last, field.kind, field.format)
    }
}

/**
 * A java.time pattern that reads [text], e.g. "dd.MM.yyyy" for "31.10.2026". Day-first unless the
 * year comes first. Null when the text has letters or an unusual shape.
 */
fun guessDateFormat(kind: FieldKind, text: String): String? {
    val trimmed = text.trim()
    val format = when (kind) {
        FieldKind.DATE -> guessDate(trimmed)
        FieldKind.TIME -> guessTime(trimmed)
        FieldKind.DATE_TIME -> {
            val split = Regex("""\s+|T""").find(trimmed) ?: return null
            val date = guessDate(trimmed.substring(0, split.range.first)) ?: return null
            val time = guessTime(trimmed.substring(split.range.last + 1)) ?: return null
            date + (if (split.value == "T") "'T'" else " ") + time
        }
        else -> null
    } ?: return null
    val readable = when (kind) {
        FieldKind.DATE -> readDateTime(trimmed, format, LocalDate::from)
        FieldKind.TIME -> readDateTime(trimmed, format, LocalTime::from)
        else -> readDateTime(trimmed, format, LocalDateTime::from)
    }
    return format.takeIf { readable != null }
}

private fun guessDate(text: String): String? {
    val groups = digitGroups(text) ?: return null
    if (groups.size != 3) return null
    val lengths = groups.map { it.value.length }
    val yearFirst = lengths[0] == 4
    val dayMonth = if (yearFirst) lengths.drop(1) else lengths.take(2)
    if (dayMonth.any { it > 2 }) return null
    // One unpadded day or month means the bank doesn't pad, so 2/11 can be 2/9 next time.
    val width = if (1 in dayMonth) 1 else 2
    val letters = if (yearFirst) {
        listOf("yyyy", twoOrOne('M', width), twoOrOne('d', width))
    } else {
        listOf(twoOrOne('d', width), twoOrOne('M', width), year(lengths[2]))
    }
    return replaceGroups(text, groups, letters)
}

private fun guessTime(text: String): String? {
    // A comma never separates hours and minutes, but often separates decimals: 5,00
    if (',' in text) return null
    val groups = digitGroups(text) ?: return null
    if (groups.size !in 2..3 || groups.drop(1).any { it.value.length != 2 }) return null
    val letters = listOf(twoOrOne('H', groups[0].value.length), "mm", "ss").take(groups.size)
    return replaceGroups(text, groups, letters)
}

/** The digit runs in [text], or null if anything but digits and plain separators is in it. */
private fun digitGroups(text: String): List<MatchResult>? {
    if (text.isEmpty() || text.any { !it.isDigit() && it !in ".,:/- " }) return null
    return Regex("""\d+""").findAll(text).toList()
}

private fun twoOrOne(letter: Char, length: Int): String? = when (length) {
    1 -> "$letter"
    2 -> "$letter$letter"
    else -> null
}

private fun year(length: Int): String? = when (length) {
    2 -> "yy"
    4 -> "yyyy"
    else -> null
}

private fun replaceGroups(text: String, groups: List<MatchResult>, letters: List<String?>): String? {
    val out = StringBuilder()
    var cursor = 0
    groups.forEachIndexed { index, group ->
        out.append(text, cursor, group.range.first)
        out.append(letters[index] ?: return null)
        cursor = group.range.last + 1
    }
    return out.append(text.substring(cursor)).toString()
}

/**
 * The decimal mark an amount like "1.590,00" uses. Null when it can't be told, like "1.234",
 * which may be 1234 or 1.234.
 */
fun guessDecimalMark(amount: String): Char? {
    val separators = amount.filter { it == '.' || it == ',' }
    if (separators.isEmpty()) return null
    val last = amount.lastIndexOfAny(charArrayOf('.', ','))
    val mark = amount[last]
    return when {
        separators.toSet().size == 2 -> mark
        // The same separator several times can only group thousands.
        separators.length > 1 -> if (mark == '.') ',' else '.'
        amount.length - last - 1 == 3 -> null
        else -> mark
    }
}

/**
 * Fields suggested by comparing the sample with [other], another message of the same kind: the
 * pieces that differ become fields, the rest stays fixed text. Kinds in [taken] aren't suggested
 * again. Something that happens to be the same in both (often the card ending) isn't found, so
 * the user still checks the result.
 */
fun suggestMarks(
    sample: String,
    pieces: List<Piece>,
    other: List<Piece>,
    taken: Set<FieldKind> = emptySet(),
): List<FieldMark> {
    val matched = matchedPieces(pieces.map { it.text }, other.map { it.text })
    val runs = mutableListOf<IntRange>()
    var start = -1
    for (i in pieces.indices) {
        if (!matched[i] && start < 0) start = i
        if (matched[i] && start >= 0) {
            runs += start until i
            start = -1
        }
    }
    if (start >= 0) runs += start until pieces.size
    // Numbers with separators (amounts, dates, times) are fields of their own, even when they
    // touch another difference: "1.590,00 TL" is an amount and a currency.
    val split = runs.flatMap { run ->
        val parts = mutableListOf<IntRange>()
        var first = run.first
        for (i in run) {
            if (STRUCTURED_NUMBER.matches(pieces[i].text)) {
                if (i > first) parts += first until i
                parts += i..i
                first = i + 1
            }
        }
        if (first <= run.last) parts += first..run.last
        parts
    }

    fun text(range: IntRange) = sample.substring(pieces[range.first].start, pieces[range.last].end)
    fun hasLetters(range: IntRange) = text(range).any { it.isLetter() }

    // Two merchants can share a word ("CATERING VE R", "MIGROS VE SANAL"), which would split the
    // merchant in two. Join word runs that only one matching piece separates.
    val joined = mutableListOf<IntRange>()
    split.forEach { run ->
        val last = joined.lastOrNull()
        if (last != null && run.first - last.last - 1 <= 1 && hasLetters(last) && hasLetters(run)) {
            joined[joined.lastIndex] = last.first..run.last
        } else {
            joined += run
        }
    }

    val used = taken.toMutableSet()
    return joined.map { run ->
        val value = text(run)
        val kind = guessKind(value, used)
        if (kind != FieldKind.IGNORE) used += kind
        FieldMark(run.first, run.last, kind, if (kind in DATE_KINDS) guessDateFormat(kind, value) else null)
    }
}

private val STRUCTURED_NUMBER = Regex("""\d+(?:[.,:/\-]\d+)+""")
private val AMOUNT_TEXT = Regex("""\d[\d.,' ]*[.,]\d{1,2}""")
private val CURRENCY_TEXT = Regex("""[A-Z]{2,3}|[€$£₺¥]""")
private val CARD_TEXT = Regex("""\d{4}""")

private fun guessKind(text: String, used: Set<FieldKind>): FieldKind {
    fun free(kind: FieldKind) = kind !in used
    val dateKind = listOf(FieldKind.DATE_TIME, FieldKind.DATE)
        .firstOrNull { guessDateFormat(it, text) != null }
    return when {
        dateKind != null && free(dateKind) &&
            (dateKind != FieldKind.DATE_TIME || (free(FieldKind.DATE) && free(FieldKind.TIME))) -> dateKind
        AMOUNT_TEXT.matches(text) && free(FieldKind.AMOUNT) -> FieldKind.AMOUNT
        guessDateFormat(FieldKind.TIME, text) != null && free(FieldKind.TIME) -> FieldKind.TIME
        CURRENCY_TEXT.matches(text) && free(FieldKind.CURRENCY) -> FieldKind.CURRENCY
        CARD_TEXT.matches(text) && free(FieldKind.CARD_ENDING) -> FieldKind.CARD_ENDING
        text.any { it.isLetter() } && free(FieldKind.MERCHANT) -> FieldKind.MERCHANT
        else -> FieldKind.IGNORE
    }
}

/** Which of [a] are in a longest common subsequence with [b]. */
private fun matchedPieces(a: List<String>, b: List<String>): BooleanArray {
    val lengths = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) {
        for (j in b.indices.reversed()) {
            lengths[i][j] = if (a[i] == b[j]) lengths[i + 1][j + 1] + 1
            else maxOf(lengths[i + 1][j], lengths[i][j + 1])
        }
    }
    val matched = BooleanArray(a.size)
    var i = 0
    var j = 0
    while (i < a.size && j < b.size) {
        when {
            a[i] == b[j] -> { matched[i] = true; i++; j++ }
            lengths[i + 1][j] >= lengths[i][j + 1] -> i++
            else -> j++
        }
    }
    return matched
}
