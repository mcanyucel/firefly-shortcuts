package com.mustafacanyucel.firefly_shortcuts.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mustafacanyucel.firefly_shortcuts.R

data class Option(val id: String, val name: String)
private const val SEARCH_THRESHOLD = 8

/** A field showing the picked option. Tapping it opens a list to pick from. */
@Composable
fun OptionPicker(
    label: String,
    options: List<Option>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allowNone: Boolean = false,
    isError: Boolean = false,
) {
    var open by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = options.find { it.id == selectedId }?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            supportingText = if (isError) { { Text("Required") } } else null,
            trailingIcon = { Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // A text field keeps taps for itself, so a clear layer on top catches them.
        Box(Modifier.matchParentSize().clickable { open = true })
    }

    if (open) {
        PickerDialog(
            title = label,
            options = options,
            selectedId = selectedId,
            allowNone = allowNone,
            onDismiss = { open = false },
            onPick = {
                onSelect(it)
                open = false
            },
        )
    }
}

@Composable
private fun PickerDialog(
    title: String,
    options: List<Option>,
    selectedId: String?,
    allowNone: Boolean,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(options, query) {
        if (query.isBlank()) options else options.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                if (options.size > SEARCH_THRESHOLD) {
                    val focus = remember { FocusRequester() }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .focusRequester(focus),
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    if (allowNone && query.isBlank()) {
                        item { OptionRow("None", selected = selectedId == null) { onPick(null) } }
                    }
                    items(shown, key = { it.id }) { option ->
                        OptionRow(option.name, selected = option.id == selectedId) { onPick(option.id) }
                    }
                }
                if (shown.isEmpty()) {
                    Text(
                        "No matches",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
