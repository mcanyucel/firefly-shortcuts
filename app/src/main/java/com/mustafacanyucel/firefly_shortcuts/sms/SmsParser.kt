package com.mustafacanyucel.firefly_shortcuts.sms

import com.mustafacanyucel.firefly_shortcuts.model.normalizeAmount
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQuery

/** What a purchase SMS said. Fields the template doesn't have are null. */
data class ParsedSms(
    val action: SmsAction,
    /**
     * As Firefly expects it, e.g. "1234.56". Null when the bank leaves it out (some online purchase
     * messages do); such a message can't post automatically and needs the amount typed in.
     */
    val amount: String?,
    /** As the bank wrote it, e.g. "TL". */
    val currency: String?,
    val date: LocalDate?,
    val time: LocalTime?,
    val merchant: String?,
    val cardEnding: String?,
)

sealed interface SmsParseResult {
    /** The message isn't this kind of SMS. */
    data object NoMatch : SmsParseResult

    /** The message is this kind of SMS, and the template says to drop it. */
    data object Ignored : SmsParseResult

    /** The wording matched, but a field couldn't be read, e.g. a date like 32.13.2026. */
    data class BadField(val kind: FieldKind, val text: String) : SmsParseResult

    data class Parsed(val sms: ParsedSms) : SmsParseResult
}

/** A template that recognised a message, and what it read. */
data class SmsMatch(val template: SmsTemplate, val result: SmsParseResult)

/**
 * Finds the first template for [sender] that matches [body]. Null when none does, which is the
 * normal case for every message that isn't from the bank.
 */
fun matchSms(sender: String, body: String, templates: List<SmsTemplate>): SmsMatch? =
    templates.asSequence()
        .filter { template -> template.senders.any { sameSender(it, sender) } }
        .map { SmsMatch(it, it.parse(body)) }
        .firstOrNull { it.result != SmsParseResult.NoMatch }

/** What the first matching template reads from [body], or null if none matches. */
fun parseSms(sender: String, body: String, templates: List<SmsTemplate>): SmsParseResult? =
    matchSms(sender, body, templates)?.result

/** Reads [body] with this template. A template with problems never matches. */
fun SmsTemplate.parse(body: String): SmsParseResult {
    if (problems().isNotEmpty()) return SmsParseResult.NoMatch
    val fields = parts.filterIsInstance<TemplatePart.Field>()
    val match = toRegex().matchEntire(body.trim()) ?: return SmsParseResult.NoMatch
    if (action == SmsAction.IGNORE) return SmsParseResult.Ignored

    var amount: String? = null
    var currency: String? = null
    var date: LocalDate? = null
    var time: LocalTime? = null
    var merchant: String? = null
    var cardEnding: String? = null
    fields.forEachIndexed { index, field ->
        val text = match.groupValues[index + 1]
        val bad = SmsParseResult.BadField(field.kind, text)
        when (field.kind) {
            FieldKind.AMOUNT -> amount = readAmount(text, decimalMark) ?: return bad
            FieldKind.CURRENCY -> currency = text
            FieldKind.DATE -> date = readDateTime(text, field.format!!, LocalDate::from) ?: return bad
            FieldKind.TIME -> time = readDateTime(text, field.format!!, LocalTime::from) ?: return bad
            FieldKind.DATE_TIME -> {
                val dateTime = readDateTime(text, field.format!!, LocalDateTime::from) ?: return bad
                date = dateTime.toLocalDate()
                time = dateTime.toLocalTime()
            }
            FieldKind.MERCHANT -> merchant = text.trim().replace(WHITESPACE, " ")
            FieldKind.CARD_ENDING -> cardEnding = text
            FieldKind.IGNORE -> Unit
        }
    }
    return SmsParseResult.Parsed(
        ParsedSms(action, amount, currency, date, time, merchant, cardEnding)
    )
}

/** Why this template can't be used yet. Empty when it's ready. */
fun SmsTemplate.problems(): List<TemplateProblem> {
    val problems = mutableListOf<TemplateProblem>()
    val fields = parts.filterIsInstance<TemplatePart.Field>()
    val kinds = fields.map { it.kind }
    if (senders.all { it.isBlank() }) problems += TemplateProblem.NO_SENDER
    if (parts.none { it is TemplatePart.Text && it.text.isNotBlank() }) {
        problems += TemplateProblem.NO_FIXED_TEXT
    }
    if (kinds.filter { it != FieldKind.IGNORE }.let { it.size != it.toSet().size }) {
        problems += TemplateProblem.DUPLICATE_FIELD
    }
    if (FieldKind.DATE_TIME in kinds && (FieldKind.DATE in kinds || FieldKind.TIME in kinds)) {
        problems += TemplateProblem.DATE_TWICE
    }
    if (fields.any { it.kind in DATE_KINDS && !isUsableDateFormat(it.kind, it.format) }) {
        problems += TemplateProblem.BAD_DATE_FORMAT
    }
    return problems
}

private val WHITESPACE = Regex("\\s+")
internal val DATE_KINDS = setOf(FieldKind.DATE, FieldKind.TIME, FieldKind.DATE_TIME)

private fun sameSender(a: String, b: String): Boolean =
    a.filterNot { it.isWhitespace() }.equals(b.filterNot { it.isWhitespace() }, ignoreCase = true)

/** One capturing group per field, in order. */
internal fun SmsTemplate.toRegex(): Regex {
    val pattern = StringBuilder()
    parts.forEachIndexed { index, part ->
        when (part) {
            is TemplatePart.Text -> {
                var text = part.text
                if (index == 0) text = text.trimStart()
                if (index == parts.lastIndex) text = text.trimEnd()
                pattern.append(fixedTextPattern(text))
            }
            is TemplatePart.Field -> pattern.append('(').append(fieldPattern(part)).append(')')
        }
    }
    return Regex(pattern.toString(), RegexOption.DOT_MATCHES_ALL)
}

private fun fixedTextPattern(text: String): String {
    val pieces = text.split(WHITESPACE)
    return pieces.joinToString("\\s+") { if (it.isEmpty()) "" else Regex.escape(it) }
}

private fun fieldPattern(field: TemplatePart.Field): String = when (field.kind) {
    // Digits with separators in between: 644,00 / 1.234,56 / 1 234.56 / 1'234.56
    FieldKind.AMOUNT -> "\\d(?:[\\d.,' \\u00A0]*\\d)?"
    FieldKind.CURRENCY -> "[^\\s\\d]+?"
    FieldKind.CARD_ENDING -> "\\d+"
    FieldKind.MERCHANT -> ".+?"
    FieldKind.IGNORE -> ".*?"
    FieldKind.DATE, FieldKind.TIME, FieldKind.DATE_TIME ->
        field.format?.let(::dateFormatPattern) ?: ".+?"
}

/**
 * A regex for numeric date patterns like "dd.MM.yyyy" or "H:mm". Null for anything else (month
 * names, AM/PM), in which case the field matches loosely and the formatter alone decides.
 */
internal fun dateFormatPattern(format: String): String? {
    val out = StringBuilder()
    var i = 0
    while (i < format.length) {
        val c = format[i]
        when {
            c == '\'' -> {
                val end = format.indexOf('\'', i + 1)
                if (end < 0) return null
                out.append(Regex.escape(if (end == i + 1) "'" else format.substring(i + 1, end)))
                i = end + 1
            }
            c.isLetter() -> {
                var end = i
                while (end < format.length && format[end] == c) end++
                out.append(letterPattern(c, end - i) ?: return null)
                i = end
            }
            c.isWhitespace() -> {
                out.append("\\s+")
                while (i < format.length && format[i].isWhitespace()) i++
            }
            else -> {
                out.append(Regex.escape(c.toString()))
                i++
            }
        }
    }
    return out.toString()
}

private fun letterPattern(letter: Char, count: Int): String? = when (letter) {
    'd', 'M', 'H', 'h', 'm', 's' -> when (count) {
        1 -> "\\d{1,2}"
        2 -> "\\d{2}"
        else -> null
    }
    'y', 'u' -> when (count) {
        2 -> "\\d{2}"
        4 -> "\\d{4}"
        else -> null
    }
    else -> null
}

/**
 * Checks the pattern is valid and gives the right kind of value: a date pattern without a year,
 * or a 12-hour time without AM/PM, can't be read back.
 */
private fun isUsableDateFormat(kind: FieldKind, format: String?): Boolean {
    if (format.isNullOrBlank()) return false
    val sample = LocalDateTime.of(2026, 11, 2, 12, 18)
    return try {
        val text = DateTimeFormatter.ofPattern(format).format(sample)
        val pattern = dateFormatPattern(format)
        if (pattern != null && !Regex(pattern).matches(text)) return false
        when (kind) {
            FieldKind.DATE -> readDateTime(text, format, LocalDate::from) == sample.toLocalDate()
            FieldKind.TIME -> readDateTime(text, format, LocalTime::from) == sample.toLocalTime()
            else -> readDateTime(text, format, LocalDateTime::from) == sample
        }
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: DateTimeException) {
        false
    }
}

internal fun <T> readDateTime(
    text: String,
    format: String,
    query: TemporalQuery<T>,
): T? = try {
    DateTimeFormatter.ofPattern(format).parse(text.trim(), query)
} catch (_: DateTimeException) {
    null
}

private fun readAmount(text: String, decimalMark: Char?): String? {
    val digits = text.filterNot { it.isWhitespace() || it == '\'' }
    if (decimalMark == null) return normalizeAmount(digits)
    val thousands = if (decimalMark == '.') ',' else '.'
    return normalizeAmount(digits.filterNot { it == thousands }.replace(decimalMark, '.'))
}
