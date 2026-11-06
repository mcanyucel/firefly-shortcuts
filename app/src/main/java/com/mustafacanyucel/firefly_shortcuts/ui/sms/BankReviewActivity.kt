package com.mustafacanyucel.firefly_shortcuts.ui.sms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageStatus
import com.mustafacanyucel.firefly_shortcuts.data.db.CardAccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.inbox.BankNotifications
import com.mustafacanyucel.firefly_shortcuts.inbox.BankPostWorker
import com.mustafacanyucel.firefly_shortcuts.inbox.BankReview
import com.mustafacanyucel.firefly_shortcuts.inbox.currencyCode
import com.mustafacanyucel.firefly_shortcuts.inbox.isForeign
import com.mustafacanyucel.firefly_shortcuts.inbox.summary
import com.mustafacanyucel.firefly_shortcuts.model.normalizeAmount
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import com.mustafacanyucel.firefly_shortcuts.ui.AmountField
import com.mustafacanyucel.firefly_shortcuts.ui.editor.Option
import com.mustafacanyucel.firefly_shortcuts.ui.editor.OptionPicker
import com.mustafacanyucel.firefly_shortcuts.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Review of one bank SMS before it goes to Firefly, over whatever is on screen. Opened from its
 * notification or the list on the SMS templates screen. Not exported: only the app opens it, and
 * the message itself is read from the database.
 */
class BankReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_MESSAGE_ID, 0)
        val container = appContainer
        setContent {
            AppTheme {
                val vm: BankReviewViewModel = viewModel { BankReviewViewModel(container, id) }
                LaunchedEffect(vm.closeReason) {
                    vm.closeReason?.let { reason ->
                        reason.toast?.let { Toast.makeText(this@BankReviewActivity, it, Toast.LENGTH_SHORT).show() }
                        finish()
                    }
                }
                if (vm.loaded) ReviewCard(vm, onCancel = ::finish)
            }
        }
    }

    companion object {
        private const val EXTRA_MESSAGE_ID = "message_id"

        fun intent(context: Context, messageId: Long): Intent =
            Intent(context, BankReviewActivity::class.java)
                .putExtra(EXTRA_MESSAGE_ID, messageId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

enum class CloseReason(val toast: String?) {
    SENT(null),
    DISCARDED(null),
    GONE("This message was already sent or discarded"),
    SENDING("This message is being sent"),
}

class BankReviewViewModel(private val container: AppContainer, private val messageId: Long) : ViewModel() {
    var loaded by mutableStateOf(false)
        private set
    var closeReason by mutableStateOf<CloseReason?>(null)
        private set

    lateinit var message: BankMessageEntity
        private set
    private var accounts = emptyList<AccountEntity>()
    var shortcuts = emptyList<ShortcutDetail>()
        private set
    private var mappedAccountId: String? = null

    var accountId by mutableStateOf<String?>(null)
        private set
    var amount by mutableStateOf(TextFieldValue(""))
    var counterparty by mutableStateOf("")
    var shortcutId by mutableStateOf<String?>(null)
    var rememberCard by mutableStateOf(true)
    private var showErrors by mutableStateOf(false)

    init {
        viewModelScope.launch {
            val db = container.database
            val found = db.bankMessageDao().getById(messageId)
            when {
                found == null -> closeReason = CloseReason.GONE
                found.status == BankMessageStatus.SENDING -> closeReason = CloseReason.SENDING
                else -> {
                    message = found
                    mappedAccountId = found.cardEnding?.let { db.bankMessageDao().accountForCard(it) }
                    accounts = db.accountDao().getAll()
                        .filter { it.accountType == "asset" && (it.active || it.id == mappedAccountId) }
                    shortcuts = db.shortcutDao().getAll()
                    counterparty = found.merchant.orEmpty()
                    selectAccount(mappedAccountId?.takeIf { id -> accounts.any { it.id == id } })
                    loaded = true
                }
            }
        }
    }

    val accountOptions: List<Option>
        get() = accounts.map { Option(it.id, if (it.active) it.name else "${it.name} (inactive)") }

    val shortcutOptions: List<Option>
        get() = shortcuts.map { s ->
            Option(s.shortcut.id.toString(), listOfNotNull(s.shortcut.icon?.ifBlank { null }, s.shortcut.name).joinToString(" "))
        }

    private val account: AccountEntity? get() = accounts.firstOrNull { it.id == accountId }

    /** The SMS amount is in another currency than the picked account. */
    val foreign: Boolean get() = account?.let { isForeign(message, it) } == true

    val accountCurrency: String? get() = account?.currencySymbol ?: account?.currencyCode

    val accountError get() = showErrors && accountId == null
    val amountError get() = showErrors && normalizeAmount(amount.text) == null
    val counterpartyError get() = showErrors && counterparty.isBlank()

    /** Shown when the picked account isn't yet the one for this card. */
    val canRememberCard: Boolean
        get() = message.cardEnding != null && accountId != null && accountId != mappedAccountId

    fun selectAccount(id: String?) {
        accountId = id
        // The SMS amount fits only an account in its own currency; a foreign one needs typing.
        val text = if (id != null && !foreign) message.amount.orEmpty() else ""
        amount = TextFieldValue(text, TextRange(0, text.length))
    }

    fun send() {
        val id = accountId
        val value = normalizeAmount(amount.text)
        if (id == null || value == null || counterparty.isBlank()) {
            showErrors = true
            return
        }
        val shortcut = shortcuts.firstOrNull { it.shortcut.id.toString() == shortcutId }
        val review = BankReview(
            accountId = id,
            amount = value,
            counterparty = counterparty.trim(),
            foreignAmount = if (foreign) message.amount else null,
            foreignCurrencyCode = if (foreign) currencyCode(message.currency) else null,
            categoryId = shortcut?.shortcut?.categoryId,
            budgetId = shortcut?.shortcut?.budgetId,
            tags = shortcut?.tags?.map { it.name }.orEmpty(),
        )
        val card = message.cardEnding
        val remember = rememberCard && canRememberCard
        // The app scope: this window closes right away.
        container.appScope.launch {
            if (remember && card != null) {
                container.database.bankMessageDao().setCardAccount(CardAccountEntity(card, id))
            }
            BankPostWorker.send(container.appContext, messageId, review)
        }
        BankNotifications.cancel(container.appContext, messageId)
        closeReason = CloseReason.SENT
    }

    fun discard() {
        container.appScope.launch { container.database.bankMessageDao().delete(messageId) }
        BankNotifications.cancel(container.appContext, messageId)
        closeReason = CloseReason.DISCARDED
    }
}

/** A card over a dimmed screen. Tapping outside it closes it and leaves the message for later. */
@Composable
private fun ReviewCard(vm: BankReviewViewModel, onCancel: () -> Unit) {
    val message = vm.message
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            )
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(message.summary, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        message.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    message.error?.let {
                        Text("Last try failed: $it", color = MaterialTheme.colorScheme.error)
                    }
                }

                val deposit = message.action == SmsAction.DEPOSIT
                OptionPicker(
                    label = if (deposit) "To account" else "From account",
                    options = vm.accountOptions,
                    selectedId = vm.accountId,
                    onSelect = vm::selectAccount,
                    isError = vm.accountError,
                )
                if (vm.canRememberCard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(vm.rememberCard, role = Role.Checkbox, onValueChange = { vm.rememberCard = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = vm.rememberCard, onCheckedChange = null)
                        Text("Use this account for card ${message.cardEnding} from now on")
                    }
                }

                AmountField(
                    value = vm.amount,
                    onValueChange = { vm.amount = it },
                    isError = vm.amountError,
                    currency = vm.accountCurrency,
                    label = if (vm.foreign) "Amount in the account's currency" else "Amount",
                )
                if (vm.foreign) {
                    Text(
                        "The SMS says ${message.amount} ${message.currency}. That's sent as the foreign amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (message.amount == null) {
                    Text(
                        "The SMS doesn't say the amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = vm.counterparty,
                    onValueChange = { vm.counterparty = it },
                    label = { Text(if (deposit) "From (revenue account)" else "Merchant (expense account)") },
                    isError = vm.counterpartyError,
                    supportingText = { Text("Also the description. Firefly creates the account if it's new.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (vm.shortcutOptions.isNotEmpty()) {
                    OptionPicker(
                        label = "Category, budget and tags of",
                        options = vm.shortcutOptions,
                        selectedId = vm.shortcutId,
                        onSelect = { vm.shortcutId = it },
                        allowNone = true,
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = vm::discard) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    Box(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Later") }
                    Button(onClick = vm::send) { Text("Send") }
                }
            }
        }
    }
}

/** "Card purchase · 2 Nov 2026 12:18 · card 1111" */
val BankMessageEntity.details: String
    get() {
        val date = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        val time = time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        return listOfNotNull(
            templateName,
            listOfNotNull(date, time).joinToString(" ").ifEmpty { null },
            cardEnding?.let { "card $it" },
        ).joinToString(" · ")
    }
