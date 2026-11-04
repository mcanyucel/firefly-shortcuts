package com.mustafacanyucel.firefly_shortcuts.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import com.mustafacanyucel.firefly_shortcuts.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(shortcutId: Long, onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: EditorViewModel = viewModel { EditorViewModel(container, shortcutId) }
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(vm.finished) {
        if (vm.finished) currentOnDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New shortcut" else "Edit shortcut") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("Name") },
                isError = vm.nameError,
                supportingText = if (vm.nameError) { { Text("Required") } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = vm.type == type,
                        onClick = { vm.selectType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                    ) { Text(type.label) }
                }
            }

            OutlinedTextField(
                value = vm.amount,
                onValueChange = { vm.amount = it },
                label = { Text("Amount") },
                isError = vm.amountError,
                supportingText = if (vm.amountError) { { Text("Enter an amount above zero, like 12.50") } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Accounts")
            OptionPicker(
                label = fromLabel(vm.type),
                options = vm.fromOptions,
                selectedId = vm.fromId,
                onSelect = vm::selectFrom,
                isError = vm.fromError,
            )
            OptionPicker(
                label = toLabel(vm.type),
                options = vm.toOptions,
                selectedId = vm.toId,
                onSelect = { vm.toId = it },
                isError = vm.toError,
            )

            SectionTitle("Optional")
            if (vm.categories.isNotEmpty()) {
                OptionPicker("Category", vm.categories, vm.categoryId, { vm.categoryId = it }, allowNone = true)
            }
            if (vm.budgets.isNotEmpty()) {
                OptionPicker("Budget", vm.budgets, vm.budgetId, { vm.budgetId = it }, allowNone = true)
            }
            if (vm.bills.isNotEmpty()) {
                OptionPicker("Bill", vm.bills, vm.billId, { vm.billId = it }, allowNone = true)
            }
            if (vm.piggybanks.isNotEmpty()) {
                OptionPicker("Piggy bank", vm.piggybanks, vm.piggybankId, { vm.piggybankId = it }, allowNone = true)
            }
            OutlinedTextField(
                value = vm.description,
                onValueChange = { vm.description = it },
                label = { Text("Description") },
                supportingText = { Text("Sent to Firefly. When empty, the name is sent instead.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.icon,
                onValueChange = { vm.icon = it },
                label = { Text("Icon") },
                supportingText = { Text("An emoji, like 🍔 or ☕") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Tags")
            if (vm.tags.isEmpty()) {
                Text("No tags synced", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in vm.tagIds,
                            onClick = { vm.toggleTag(tag.id) },
                            label = { Text(tag.name) },
                        )
                    }
                }
            }

            vm.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, enabled = !vm.saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.isNew) "Create" else "Save")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

private fun fromLabel(type: TransactionType) = when (type) {
    TransactionType.WITHDRAWAL -> "Source account"
    TransactionType.DEPOSIT -> "Revenue account"
    TransactionType.TRANSFER -> "From account"
}

private fun toLabel(type: TransactionType) = when (type) {
    TransactionType.WITHDRAWAL -> "Expense account"
    TransactionType.DEPOSIT -> "Destination account"
    TransactionType.TRANSFER -> "To account"
}
