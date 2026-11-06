package com.mustafacanyucel.firefly_shortcuts.ui.sms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.BuildConfig
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.SmsTemplateEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SmsTemplatesViewModel(private val container: AppContainer) : ViewModel() {
    /** Null until the first read. */
    val templates: StateFlow<List<SmsTemplateEntity>?> = container.database.smsTemplateDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bankMessages: StateFlow<List<BankMessageEntity>> = container.database.bankMessageDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearBankMessages() {
        viewModelScope.launch { container.database.bankMessageDao().deleteAll() }
    }
}

fun Context.canReceiveSms(): Boolean =
    BuildConfig.SMS_ENABLED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTemplatesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val vm: SmsTemplatesViewModel = viewModel { SmsTemplatesViewModel(container) }
    val templates by vm.templates.collectAsStateWithLifecycle()
    val bankMessages by vm.bankMessages.collectAsStateWithLifecycle()

    // Checked again on return from the app's settings, where it may have been allowed.
    var canReceive by remember { mutableStateOf(context.canReceiveSms()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canReceive = context.canReceiveSms() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS templates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Add template")
            }
        },
    ) { padding ->
        val list = templates ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            if (BuildConfig.SMS_ENABLED && !canReceive) {
                item { SmsPermissionCard(onGranted = { canReceive = true }) }
            }
            item {
                Text(
                    "A template tells the app how to read one kind of SMS from your bank. Paste a " +
                        "message, then mark the parts that change: the amount, the merchant, the date. " +
                        "Every message with the same wording is read the same way. Make one template " +
                        "per kind of message, like purchases and refunds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "No templates yet. Tap + to make one.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(list, key = { it.id }) { entity ->
                val t = entity.template
                ListItem(
                    headlineContent = { Text(t.name) },
                    supportingContent = {
                        Text((listOf(t.action.label) + t.senders).joinToString(" · "))
                    },
                    modifier = Modifier.clickable { onEdit(entity.id) },
                )
            }
            if (bankMessages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Read from SMS", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Not sent to Firefly yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = vm::clearBankMessages) { Text("Clear") }
                    }
                }
                items(bankMessages, key = { "message-${it.id}" }) { BankMessageRow(it) }
            }
        }
    }
}

@Composable
private fun SmsPermissionCard(onGranted: () -> Unit) {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        asked = true
        if (granted) onGranted()
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SMS access is off", style = MaterialTheme.typography.titleMedium)
            Text(
                "The app reads each incoming SMS once, keeps what a template recognises, and drops " +
                    "everything else without storing it."
            )
            Button(onClick = { launcher.launch(Manifest.permission.RECEIVE_SMS) }) { Text("Allow SMS access") }
            if (asked) {
                Text(
                    "If Android doesn't ask, allow SMS in the app's settings. On Android 13 and newer, " +
                        "an app installed from outside a store may first need App info → ⋮ → " +
                        "Allow restricted settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                }) { Text("Open app settings") }
            }
        }
    }
}

@Composable
private fun BankMessageRow(message: BankMessageEntity) {
    val amount = message.amount?.let { listOfNotNull(it, message.currency).joinToString(" ") } ?: "No amount"
    val date = message.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val time = message.time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val details = listOfNotNull(
        message.templateName,
        listOfNotNull(date, time).joinToString(" ").ifEmpty { null },
        message.cardEnding?.let { "card $it" },
    )
    ListItem(
        headlineContent = { Text(listOfNotNull(amount, message.merchant).joinToString(" · ")) },
        supportingContent = { Text(details.joinToString(" · ")) },
        overlineContent = { Text(message.action.label) },
    )
}
