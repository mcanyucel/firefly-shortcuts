package com.mustafacanyucel.firefly_shortcuts.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import com.mustafacanyucel.firefly_shortcuts.model.normalizeAmount
import com.mustafacanyucel.firefly_shortcuts.ui.AmountField
import com.mustafacanyucel.firefly_shortcuts.ui.StatusSnackbarHost
import com.mustafacanyucel.firefly_shortcuts.ui.amountText
import com.mustafacanyucel.firefly_shortcuts.ui.color
import com.mustafacanyucel.firefly_shortcuts.ui.currencyText
import com.mustafacanyucel.firefly_shortcuts.ui.iconRes
import com.mustafacanyucel.firefly_shortcuts.ui.label
import com.mustafacanyucel.firefly_shortcuts.ui.rememberAmountState
import com.mustafacanyucel.firefly_shortcuts.ui.show
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSync: () -> Unit,
    onAddShortcut: () -> Unit,
    onEditShortcut: (Long) -> Unit,
) {
    val container = LocalContext.current.appContainer
    val vm: HomeViewModel = viewModel { HomeViewModel(container) }
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    val neverSynced by vm.neverSynced.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.messages.collect { launch { snackbar.show(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firefly Shortcuts") },
                actions = {
                    IconButton(onClick = onOpenSync) {
                        Icon(painterResource(R.drawable.ic_sync), contentDescription = "Sync")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val list = shortcuts
            when {
                list == null -> {}  // the first database read hasn't finished
                list.isEmpty() -> EmptyState(neverSynced, onOpenSync, Modifier)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Bottom space so the last card can scroll out from under the + button.
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.shortcut.id }) { item ->
                        ShortcutCard(
                            item = item,
                            running = item.shortcut.id in vm.running,
                            onRun = { vm.askRun(item) },
                            onEdit = { onEditShortcut(item.shortcut.id) },
                            onDelete = { vm.askDelete(item) },
                        )
                    }
                }
            }

            // Scaffold puts the snackbar above the FAB. Stacking them here
            // instead keeps the snackbar at the bottom and lifts the FAB.
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().animateContentSize(),
                horizontalAlignment = Alignment.End,
            ) {
                FloatingActionButton(onClick = onAddShortcut, modifier = Modifier.padding(16.dp)) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = "New shortcut")
                }
                StatusSnackbarHost(snackbar)
            }
        }
    }

    vm.confirmRun?.let { item ->
        if (item.shortcut.askAmount) {
            AskAmountDialog(item, onRun = vm::run, onDismiss = vm::dismissDialog)
        } else {
            AlertDialog(
                onDismissRequest = vm::dismissDialog,
                title = { Text("Run \"${item.shortcut.name}\"?") },
                text = {
                    Text(
                        "${item.shortcut.transactionType.label} · ${item.amountText}\n" +
                                "${item.fromAccount.name} → ${item.toAccount.name}"
                    )
                },
                confirmButton = { TextButton(onClick = { vm.run() }) { Text("Run") } },
                dismissButton = { TextButton(onClick = vm::dismissDialog) { Text("Cancel") } },
            )
        }
    }

    vm.confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = vm::dismissDialog,
            title = { Text("Delete shortcut?") },
            text = { Text("\"${item.shortcut.name}\" will be deleted from this phone. Firefly isn't affected.") },
            confirmButton = {
                TextButton(
                    onClick = vm::delete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = vm::dismissDialog) { Text("Cancel") } },
        )
    }
}

/** The run dialog for a shortcut that asks for the amount, with the keyboard up. */
@Composable
private fun AskAmountDialog(item: ShortcutDetail, onRun: (String) -> Unit, onDismiss: () -> Unit) {
    var amount by rememberAmountState(item.shortcut.amount)
    var showError by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val run: () -> Unit = {
        val normalized = normalizeAmount(amount.text)
        if (normalized == null) {
            showError = true
        } else {
            onRun(normalized)
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Run \"${item.shortcut.name}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${item.shortcut.transactionType.label}\n" +
                            "${item.fromAccount.name} → ${item.toAccount.name}"
                )
                AmountField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        showError = false
                    },
                    isError = showError,
                    currency = item.currencyText,
                    modifier = Modifier.focusRequester(focus),
                    onDone = run,
                )
            }
        },
        confirmButton = { TextButton(onClick = run) { Text("Run") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyState(neverSynced: Boolean, onOpenSync: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No shortcuts yet", style = MaterialTheme.typography.titleMedium)
        if (neverSynced) {
            Text("Sync first, so there are accounts to pick from.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpenSync) { Text("Open sync") }
        } else {
            Text("Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShortcutCard(
    item: ShortcutDetail,
    running: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = item.shortcut
    val inactiveAccount = !item.fromAccount.active || !item.toAccount.active

    Card(onClick = onRun, enabled = !running, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                val emoji = s.icon
                if (!emoji.isNullOrBlank()) {
                    Text(emoji, fontSize = 28.sp)
                } else {
                    Icon(painterResource(s.transactionType.iconRes), contentDescription = null, tint = s.transactionType.color)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${item.fromAccount.name} → ${item.toAccount.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeBadge(s.transactionType)
                    Text(item.amountText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                if (inactiveAccount) {
                    Text(
                        "Uses an account that's inactive or deleted in Firefly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (running) {
                CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
            } else {
                ShortcutMenu(onEdit, onDelete)
            }
        }
    }
}

@Composable
private fun ShortcutMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { open = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun TypeBadge(type: TransactionType) {
    val color = type.color
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(type.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
