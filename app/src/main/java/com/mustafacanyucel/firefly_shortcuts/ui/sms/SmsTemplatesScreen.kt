package com.mustafacanyucel.firefly_shortcuts.ui.sms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.SmsTemplateEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SmsTemplatesViewModel(container: AppContainer) : ViewModel() {
    /** Null until the first read. */
    val templates: StateFlow<List<SmsTemplateEntity>?> = container.database.smsTemplateDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTemplatesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val container = LocalContext.current.appContainer
    val vm: SmsTemplatesViewModel = viewModel { SmsTemplatesViewModel(container) }
    val templates by vm.templates.collectAsStateWithLifecycle()

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
        }
    }
}
