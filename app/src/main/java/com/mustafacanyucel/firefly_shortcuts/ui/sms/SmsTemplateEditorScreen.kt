package com.mustafacanyucel.firefly_shortcuts.ui.sms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import com.mustafacanyucel.firefly_shortcuts.sms.SmsParseResult
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_KINDS = setOf(FieldKind.DATE, FieldKind.TIME, FieldKind.DATE_TIME)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTemplateEditorScreen(templateId: Long, onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: SmsTemplateEditorViewModel = viewModel { SmsTemplateEditorViewModel(container, templateId) }
    val currentOnDone by rememberUpdatedState(onDone)
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(vm.finished) {
        if (vm.finished) currentOnDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New SMS template" else "Edit SMS template") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (!vm.isNew) {
                        TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
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
                placeholder = { Text("Card purchase") },
                isError = vm.nameError,
                supportingText = if (vm.nameError) { { Text("Required") } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.senders,
                onValueChange = { vm.senders = it },
                label = { Text("Sender") },
                placeholder = { Text("YAPIKREDI") },
                supportingText = {
                    Text("As your SMS app shows it. Separate several names with commas.")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("These messages are")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SmsAction.entries.forEachIndexed { index, action ->
                    SegmentedButton(
                        selected = vm.action == action,
                        onClick = { vm.action = action },
                        shape = SegmentedButtonDefaults.itemShape(index, SmsAction.entries.size),
                    ) { Text(action.label) }
                }
            }
            if (vm.action == SmsAction.IGNORE) {
                Hint("For messages that should never become a transaction, like declined payments.")
            }

            SectionTitle("Message")
            if (vm.sample.isEmpty()) {
                SamplePaste(vm)
            } else {
                MarkingArea(vm)
            }

            if (vm.marks.isNotEmpty()) {
                FieldList(vm)
            }

            vm.sampleResult?.let {
                SectionTitle("Read from this message")
                ResultCard(it)
            }

            if (vm.sample.isNotEmpty()) {
                SectionTitle("Test another message")
                OutlinedTextField(
                    value = vm.testMessage,
                    onValueChange = { vm.testMessage = it },
                    placeholder = { Text("Paste another message from the bank") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                vm.testResult?.let { ResultCard(it) }
                if (vm.testMessage.isNotBlank()) {
                    TextButton(onClick = vm::markDifferencesFromTest) {
                        Text("Mark what differs from this message")
                    }
                }
            }

            vm.problems.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            vm.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, enabled = !vm.saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.isNew) "Create" else "Save")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete template?") },
            text = { Text("Messages worded like this won't be read any more.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; vm.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SamplePaste(vm: SmsTemplateEditorViewModel) {
    OutlinedTextField(
        value = vm.draftSample,
        onValueChange = { vm.draftSample = it },
        placeholder = { Text("Copy a message from your SMS app and paste it here") },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = vm.draftSecond,
        onValueChange = { vm.draftSecond = it },
        label = { Text("Another message of the same kind (optional)") },
        supportingText = { Text("What differs between the two messages gets marked for you.") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = vm::useDraftSample,
        enabled = vm.draftSample.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Use this message") }
}

@Composable
private fun MarkingArea(vm: SmsTemplateEditorViewModel) {
    if (vm.marksLost) {
        Text(
            "This template's fields couldn't be found in its message. Mark them again.",
            color = MaterialTheme.colorScheme.error,
        )
    }
    Hint(
        "Tap a part of the message to select it, or tap its first and last piece to select " +
            "everything between. Then choose what it is. Leave the bank's fixed wording unmarked."
    )
    FlowRow(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        vm.pieces.forEachIndexed { index, piece ->
            val selected = vm.selection?.let { index in it } == true
            val mark = vm.markAt(index)
            val colors = MaterialTheme.colorScheme
            val (background, content) = when {
                selected -> colors.inverseSurface to colors.inverseOnSurface
                mark != null -> mark.kind.colors
                // Fixed wording: outlined only, so the marked fields stand out.
                else -> Color.Transparent to colors.onSurface
            }
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .padding(start = if (piece.spaceBefore) 6.dp else 1.dp)
                    .clip(shape)
                    .background(background)
                    .border(1.dp, if (mark == null && !selected) colors.outlineVariant else Color.Transparent, shape)
                    .clickable { vm.tapPiece(index) }
                    .padding(horizontal = 5.dp, vertical = 3.dp),
            ) {
                Text(piece.text, color = content, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    vm.suggestionNote?.let { Hint(it) }
    if (vm.selection != null) {
        SelectionPanel(vm)
    }
    TextButton(onClick = vm::changeSample) { Text("Use a different message") }
}

@Composable
private fun SelectionPanel(vm: SmsTemplateEditorViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("\"${vm.selectedText}\" is the", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldKind.entries.forEach { kind ->
                    AssistChip(onClick = { vm.markSelection(kind) }, label = { Text(kind.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vm.selectionIsMarked) {
                    OutlinedButton(onClick = vm::unmarkSelection) { Text("Unmark") }
                }
                TextButton(onClick = vm::clearSelection) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun FieldList(vm: SmsTemplateEditorViewModel) {
    SectionTitle("Fields")
    vm.marks.sortedBy { it.first }.forEach { mark ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(mark.kind.colors.first)
            )
            Text(mark.kind.label, style = MaterialTheme.typography.labelLarge)
            Text(vm.textOf(mark), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (mark.kind in DATE_KINDS) {
            OutlinedTextField(
                value = mark.format.orEmpty(),
                onValueChange = { vm.setFormat(mark, it) },
                label = { Text("${mark.kind.label} pattern") },
                supportingText = {
                    Text("d day, M month, y year, H hour, m minute. Like dd.MM.yyyy or HH:mm")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (vm.marks.any { it.kind == FieldKind.AMOUNT }) {
        Text("Decimal mark", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf('.' to "Point: 1,234.56", ',' to "Comma: 1.234,56")
            options.forEachIndexed { index, (mark, label) ->
                SegmentedButton(
                    selected = vm.decimalMark == mark,
                    onClick = { vm.decimalMark = mark },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label) }
            }
        }
        if (vm.decimalMark == null) {
            Hint("Pick the one your bank uses. Without it, 1.234 could be read as 1.234 instead of 1234.")
        }
    }
}

@Composable
private fun ResultCard(result: SmsParseResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (result) {
                SmsParseResult.NoMatch -> Text("Doesn't match this template.")
                SmsParseResult.Ignored -> Text("Matches. Messages like this are ignored.")
                is SmsParseResult.BadField -> Text(
                    "Matches, but the ${result.kind.label.lowercase()} \"${result.text}\" can't be read.",
                    color = MaterialTheme.colorScheme.error,
                )
                is SmsParseResult.Parsed -> {
                    val sms = result.sms
                    Text(sms.action.label, style = MaterialTheme.typography.titleSmall)
                    Line(
                        "Amount",
                        sms.amount?.let { listOfNotNull(it, sms.currency).joinToString(" ") }
                            ?: "Not in the message. You'll type it when reviewing.",
                    )
                    val date = sms.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    val time = sms.time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                    listOfNotNull(date, time).takeIf { it.isNotEmpty() }?.let {
                        Line("Date", it.joinToString(" "))
                    }
                    sms.merchant?.let { Line("Merchant", it) }
                    sms.cardEnding?.let { Line("Card ending", it) }
                }
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
