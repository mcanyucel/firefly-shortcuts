package com.mustafacanyucel.firefly_shortcuts.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutEntity
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import com.mustafacanyucel.firefly_shortcuts.model.normalizeAmount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Edits the shortcut with [shortcutId], or creates one when it's 0. */
class EditorViewModel(
    private val container: AppContainer,
    private val shortcutId: Long,
) : ViewModel() {
    val isNew = shortcutId == 0L

    var name by mutableStateOf("")
    var amount by mutableStateOf("")
    /** When true, the amount is asked for at each run, and [amount] is an optional suggestion. */
    var askAmount by mutableStateOf(false)
    var description by mutableStateOf("")
    var icon by mutableStateOf("")
    var type by mutableStateOf(TransactionType.WITHDRAWAL)
        private set
    var fromId by mutableStateOf<String?>(null)
        private set
    var toId by mutableStateOf<String?>(null)
    var categoryId by mutableStateOf<String?>(null)
    var budgetId by mutableStateOf<String?>(null)
    var billId by mutableStateOf<String?>(null)
    var piggybankId by mutableStateOf<String?>(null)
    var tagIds by mutableStateOf(emptySet<String>())
        private set

    var loaded by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    /** True once the screen should close: saved, or the shortcut no longer exists. */
    var finished by mutableStateOf(false)
        private set

    // Errors only show after the first save attempt, not while typing.
    private var showErrors by mutableStateOf(false)
    val nameError get() = showErrors && name.isBlank()
    val amountError get() = showErrors && !amountValid()
    val fromError get() = showErrors && fromId == null
    val toError get() = showErrors && toId == null

    // Read once, before loaded becomes true, so they don't need to be state.
    private var accounts = emptyList<AccountEntity>()
    var categories = emptyList<Option>()
        private set
    var budgets = emptyList<Option>()
        private set
    var bills = emptyList<Option>()
        private set
    var piggybanks = emptyList<Option>()
        private set
    var tags = emptyList<Option>()
        private set
    private var lastUsed: Long? = null

    val fromOptions: List<Option>
        get() = accountOptions(sourceTypes(type), selected = fromId)

    val toOptions: List<Option>
        get() = accountOptions(destinationTypes(type), selected = toId)
            .filter { type != TransactionType.TRANSFER || it.id != fromId }

    init {
        viewModelScope.launch {
            val db = container.database
            accounts = db.accountDao().getAll()
            categories = db.categoryDao().getAll().map { Option(it.id, it.name) }
            budgets = db.budgetDao().getAll().map { Option(it.id, it.name) }
            bills = db.billDao().getAll().map { Option(it.id, it.name) }
            piggybanks = db.piggybankDao().getAll().map { Option(it.id, it.name) }
            tags = db.tagDao().getAll().map { Option(it.id, it.name) }

            if (!isNew) {
                val detail = db.shortcutDao().getById(shortcutId)
                if (detail == null) {
                    finished = true
                    return@launch
                }
                val s = detail.shortcut
                name = s.name
                amount = s.amount
                askAmount = s.askAmount
                description = s.description.orEmpty()
                icon = s.icon.orEmpty()
                type = s.transactionType
                fromId = s.fromAccountId
                toId = s.toAccountId
                categoryId = s.categoryId
                budgetId = s.budgetId
                billId = s.billId
                piggybankId = s.piggybankId
                tagIds = detail.tags.map { it.id }.toSet()
                lastUsed = s.lastUsed
            }
            loaded = true
        }
    }

    /** Keeps the picked accounts that still fit the new type. */
    fun selectType(newType: TransactionType) {
        type = newType
        if (accountType(fromId) !in sourceTypes(newType)) fromId = null
        if (accountType(toId) !in destinationTypes(newType) ||
            (newType == TransactionType.TRANSFER && toId == fromId)
        ) {
            toId = null
        }
    }

    fun selectFrom(id: String?) {
        fromId = id
        if (type == TransactionType.TRANSFER && toId == id) toId = null
    }

    fun toggleTag(id: String) {
        tagIds = if (id in tagIds) tagIds - id else tagIds + id
    }

    fun save() {
        if (saving) return
        val normalizedAmount = normalizeAmount(amount)
        val from = fromId
        val to = toId
        if (name.isBlank() || !amountValid() || from == null || to == null) {
            showErrors = true
            return
        }
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                val shortcut = ShortcutEntity(
                    id = shortcutId,
                    name = name.trim(),
                    // Empty only for a shortcut that asks for the amount and has no suggestion.
                    amount = normalizedAmount.orEmpty(),
                    fromAccountId = from,
                    toAccountId = to,
                    categoryId = categoryId,
                    billId = billId,
                    budgetId = budgetId,
                    piggybankId = piggybankId,
                    icon = icon.trim().ifEmpty { null },
                    description = description.trim().ifEmpty { null },
                    lastUsed = lastUsed,
                    transactionType = type,
                    askAmount = askAmount,
                )
                container.database.shortcutDao().save(shortcut, tagIds.toList())
                finished = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // e.g. a sync deleted the picked account meanwhile
                saveError = "Couldn't save: ${e.message}"
                saving = false
            }
        }
    }

    /** Required for a fixed amount; for one that's asked for, only checked if filled in. */
    private fun amountValid(): Boolean =
        normalizeAmount(amount) != null || (askAmount && amount.isBlank())

    private fun accountType(id: String?) = accounts.find { it.id == id }?.accountType

    /** Active accounts of these types, plus the picked one even if it's inactive. */
    private fun accountOptions(types: Set<String>, selected: String?): List<Option> =
        accounts
            .filter { it.accountType in types && (it.active || it.id == selected) }
            .map { Option(it.id, if (it.active) it.name else "${it.name} (inactive)") }
}

private fun sourceTypes(type: TransactionType): Set<String> = when (type) {
    TransactionType.WITHDRAWAL, TransactionType.TRANSFER -> setOf("asset")
    TransactionType.DEPOSIT -> setOf("revenue")
}

private fun destinationTypes(type: TransactionType): Set<String> = when (type) {
    TransactionType.WITHDRAWAL -> setOf("expense")
    TransactionType.DEPOSIT, TransactionType.TRANSFER -> setOf("asset")
}
