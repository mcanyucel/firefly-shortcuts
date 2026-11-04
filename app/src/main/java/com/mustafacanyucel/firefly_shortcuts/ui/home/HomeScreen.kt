package com.mustafacanyucel.firefly_shortcuts.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSettings: () -> Unit, onOpenSync: () -> Unit, onAddShortcut: () -> Unit) {
    val dao = LocalContext.current.appContainer.database.shortcutDao()
    // observeAll() returns a new Flow on every call. Without remember, each
    // redraw would start a new database query.
    val shortcuts by remember(dao) { dao.observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())

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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddShortcut) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "New shortcut")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                if (shortcuts.isEmpty()) "No shortcuts yet"
                else shortcuts.joinToString("\n") { "${it.shortcut.name}: ${it.shortcut.amount}" }
            )
        }
    }
}
