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
