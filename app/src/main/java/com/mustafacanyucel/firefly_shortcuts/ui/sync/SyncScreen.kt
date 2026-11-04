package com.mustafacanyucel.firefly_shortcuts.ui.sync

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.sync.SyncTable
import com.mustafacanyucel.firefly_shortcuts.sync.TableStatus
import com.mustafacanyucel.firefly_shortcuts.ui.errorText
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val state by container.sync.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            settings?.let { Text(lastSyncText(it.lastSync), style = MaterialTheme.typography.bodyMedium) }

            Button(
                onClick = { container.appScope.launch { container.sync.syncAll() } },
                enabled = !state.running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.running) {
                    CircularProgressIndicator(Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Syncing…")
                } else {
                    Icon(painterResource(R.drawable.ic_sync), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Sync now")
                }
            }

            HorizontalDivider()
            SyncTable.entries.forEach { table -> TableRow(table, state.tables[table]) }
        }
    }
}

/** One table's line: its name, then progress, the item count, or what went wrong. */
@Composable
private fun TableRow(table: SyncTable, status: TableStatus?) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(table.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when (status) {
                null -> {}
                TableStatus.Syncing ->
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                is TableStatus.Done -> {
                    Text("${status.count} items", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                is TableStatus.Failed ->
                    Icon(painterResource(R.drawable.ic_error), contentDescription = "Failed", tint = colors.error, modifier = Modifier.size(18.dp))
            }
        }
        if (status is TableStatus.Failed) {
            Text(errorText(status.error), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
    }
}

private val SyncTable.label: String
    get() = when (this) {
        SyncTable.ACCOUNTS -> "Accounts"
        SyncTable.BILLS -> "Bills"
        SyncTable.BUDGETS -> "Budgets"
        SyncTable.CATEGORIES -> "Categories"
        SyncTable.PIGGY_BANKS -> "Piggy banks"
        SyncTable.TAGS -> "Tags"
    }

private fun lastSyncText(time: Instant?): String {
    if (time == null) return "Never synced"
    val now = System.currentTimeMillis()
    if (now - time.toEpochMilli() < DateUtils.MINUTE_IN_MILLIS) return "Last synced just now"
    return "Last synced " + DateUtils.getRelativeTimeSpanString(time.toEpochMilli(), now, DateUtils.MINUTE_IN_MILLIS)
}
