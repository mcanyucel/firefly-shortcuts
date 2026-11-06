package com.mustafacanyucel.firefly_shortcuts.sms

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How to read one kind of bank SMS. The message must match [parts] from start to end: fixed text
 * where the bank's wording never changes, fields where it does.
 */
@Serializable
data class SmsTemplate(
    val name: String,
    /**
     * The names the bank's SMS can come from, as Android reports them, e.g. "YAPIKREDI". A list,
     * because the same sender can show up differently (a name on the phone, a number elsewhere).
     */
    val senders: List<String>,
    val parts: List<TemplatePart>,
    val action: SmsAction = SmsAction.WITHDRAWAL,
    /**
     * The bank's decimal mark, when known. Without it, "1.234" is read as 1.234 rather than 1234,
     * because a lone separator is taken as the decimal mark.
     */
    val decimalMark: Char? = null,
    /**
     * Send matching messages to Firefly without asking, when nothing needs the user: the amount,
     * the merchant and the card's account are known, and the amount is in that account's currency.
     */
    val autoPost: Boolean = false,
)

@Serializable
sealed interface TemplatePart {
    /** Text the bank always writes. Any run of whitespace matches any run of whitespace. */
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : TemplatePart

    /** Text that changes from message to message. [format] is the date/time pattern, e.g. "dd.MM.yyyy". */
    @Serializable
    @SerialName("field")
    data class Field(val kind: FieldKind, val format: String? = null) : TemplatePart
}

@Serializable
enum class FieldKind {
    AMOUNT,
    CURRENCY,
    DATE,
    TIME,
    DATE_TIME,
    MERCHANT,
    CARD_ENDING,

    /** Changes between messages but isn't needed, like a reference code. */
    IGNORE,
}

@Serializable
enum class SmsAction {
    WITHDRAWAL,
    DEPOSIT,

    /** Messages of this kind (a declined payment, an OTP) are recognised and deliberately dropped. */
    IGNORE,
}

enum class TemplateProblem {
    NO_SENDER,

    /** Without fixed text the template would match every message from the sender. */
    NO_FIXED_TEXT,
    DUPLICATE_FIELD,

    /** A date and time field together with a separate date or time field. */
    DATE_TWICE,
    BAD_DATE_FORMAT,
}
