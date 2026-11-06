package com.mustafacanyucel.firefly_shortcuts.inbox

import com.mustafacanyucel.firefly_shortcuts.data.api.TransactionRequest
import com.mustafacanyucel.firefly_shortcuts.data.api.TransactionSplit
import com.mustafacanyucel.firefly_shortcuts.data.api.transactionDate
import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** What the user chose for a bank message, or the defaults when it's sent without review. */
data class BankReview(
    /** The asset account: the source of a withdrawal, the destination of a deposit. */
    val accountId: String,
    /** In the account's currency, as Firefly expects it ("12.50"). */
    val amount: String,
    /** The merchant: the expense account of a withdrawal, the revenue account of a deposit. */
    val counterparty: String,
    /** The amount the SMS gave, when it's in another currency than the account. */
    val foreignAmount: String? = null,
    val foreignCurrencyCode: String? = null,
    val categoryId: String? = null,
    val budgetId: String? = null,
    val tags: List<String> = emptyList(),
)

private val CURRENCY_NAMES = mapOf(
    "TL" to "TRY", "YTL" to "TRY", "₺" to "TRY",
    "€" to "EUR", "$" to "USD", "£" to "GBP", "¥" to "JPY",
)

/** The ISO code for a currency as a bank writes it: "TL" → "TRY". Null when it can't be told. */
fun currencyCode(written: String?): String? {
    val text = written?.trim()?.uppercase()?.ifEmpty { null } ?: return null
    return CURRENCY_NAMES[text] ?: text.takeIf { Regex("[A-Z]{3}").matches(it) }
}

/**
 * True when the SMS amount is in another currency than [account]. Unknown currencies on either
 * side count as the same, since there's nothing to compare.
 */
fun isForeign(message: BankMessageEntity, account: AccountEntity): Boolean {
    val sms = currencyCode(message.currency) ?: return false
    val own = account.currencyCode ?: return false
    return !sms.equals(own, ignoreCase = true)
}

/**
 * When the transaction happened: the SMS's date and time. A message with a date but no time gets
 * the time it arrived if it arrived that day, else noon. One with no date gets its arrival time.
 * Always the same for the same message, so a retry is caught as a duplicate.
 */
fun bankTransactionTime(message: BankMessageEntity, zone: ZoneId = ZoneId.systemDefault()): OffsetDateTime {
    val arrived = Instant.ofEpochMilli(message.receivedAt).atZone(zone)
    val date = message.date ?: return arrived.toOffsetDateTime()
    val time = message.time ?: if (arrived.toLocalDate() == date) arrived.toLocalTime() else LocalTime.NOON
    return date.atTime(time).atZone(zone).toOffsetDateTime()
}

fun bankTransactionRequest(
    message: BankMessageEntity,
    account: AccountEntity,
    review: BankReview,
    zone: ZoneId = ZoneId.systemDefault(),
): TransactionRequest {
    val deposit = message.action == SmsAction.DEPOSIT
    val counterparty = review.counterparty.trim()
    return TransactionRequest(
        transactions = listOf(
            TransactionSplit(
                type = if (deposit) "deposit" else "withdrawal",
                date = transactionDate(bankTransactionTime(message, zone)),
                amount = review.amount,
                description = counterparty,
                sourceId = if (deposit) null else account.id,
                sourceName = if (deposit) counterparty else account.name,
                destinationId = if (deposit) account.id else null,
                destinationName = if (deposit) account.name else counterparty,
                foreignAmount = review.foreignAmount,
                foreignCurrencyCode = review.foreignAmount?.let { review.foreignCurrencyCode },
                categoryId = review.categoryId,
                budgetId = review.budgetId,
                tags = review.tags.ifEmpty { null },
            )
        )
    )
}
