package com.mustafacanyucel.firefly_shortcuts.data.api

import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class TransactionRequest(
    val transactions: List<TransactionSplit>,
    // Firefly rejects a request identical to an existing transaction, so a resubmitted request
    // can't create a second one. Only sent because FireflyApi's Json has encodeDefaults =  true
    @SerialName("error_if_duplicate_hash") val errorIfDuplicateHash: Boolean = true
)
@Serializable
data class TransactionSplit(
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    // An id, or only a name: Firefly finds or creates an expense or revenue account by name.
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("destination_id") val destinationId: String? = null,
    @SerialName("destination_name") val destinationName: String? = null,
    @SerialName("foreign_amount") val foreignAmount: String? = null,
    @SerialName("foreign_currency_code") val foreignCurrencyCode: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("budget_id") val budgetId: String? = null,
    @SerialName("bill_id") val billId: String? = null,
    @SerialName("piggy_bank_id") val piggyBankId: String? = null,
    val tags: List<String>? = null,
)

/**
 * ISO 8601 with the UTC offset, to the second. Without an offset, Firefly reads
 * the time in the server's timezone.
 */
fun transactionDate(time: OffsetDateTime = OffsetDateTime.now()): String =
    time.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/** [amount] defaults to the shortcut's own; shortcuts that ask for it pass the typed one. */
fun ShortcutDetail.toTransactionRequest(date: String, amount: String = shortcut.amount) = TransactionRequest(
    transactions = listOf(
        TransactionSplit(
            type = shortcut.transactionType.apiValue,
            date = date,
            amount = amount,
            description = shortcut.description?.ifBlank { null } ?: shortcut.name,
            sourceId = fromAccount.id,
            sourceName = fromAccount.name,
            destinationId = toAccount.id,
            destinationName = toAccount.name,
            categoryId = shortcut.categoryId,
            budgetId = shortcut.budgetId,
            billId = shortcut.billId,
            piggyBankId = shortcut.piggybankId,
            tags = tags.map { it.name }.ifEmpty { null },
        )
    )
)
