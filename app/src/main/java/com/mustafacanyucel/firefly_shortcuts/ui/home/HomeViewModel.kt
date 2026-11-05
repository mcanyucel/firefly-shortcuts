package com.mustafacanyucel.firefly_shortcuts.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.data.api.OfflineException
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.ui.MessageKind
import com.mustafacanyucel.firefly_shortcuts.ui.UiMessage
import com.mustafacanyucel.firefly_shortcuts.ui.errorText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    /** Null until the first read from the database. */
    val shortcuts: StateFlow<List<ShortcutDetail>?> = container.database.shortcutDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val neverSynced: StateFlow<Boolean> = container.settings.settings
        .map { it.lastSync == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** IDs of the shortcuts whose transaction is being sent. */
    var running by mutableStateOf(emptySet<Long>())
        private set
    var confirmRun by mutableStateOf<ShortcutDetail?>(null)
        private set
    var confirmDelete by mutableStateOf<ShortcutDetail?>(null)
        private set

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun askRun(item: ShortcutDetail) {
        confirmRun = item
    }

    fun askDelete(item: ShortcutDetail) {
        confirmDelete = item
    }

    fun dismissDialog() {
        confirmRun = null
        confirmDelete = null
    }

    /** [amount] is the typed amount, for a shortcut that asks for one. */
    fun run(amount: String? = null) {
        val item = confirmRun ?: return
        confirmRun = null
        val id = item.shortcut.id
        if (id in running) return
        running = running + id
        // The app scope, so leaving the screen doesn't cut the request off.
        // Main, because it changes Compose state; the network part switches
        // threads by itself.
        container.appScope.launch(Dispatchers.Main) {
            val message = try {
                container.runner.run(item, amount = amount)
                UiMessage("Created \"${item.shortcut.name}\"", MessageKind.SUCCESS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                runErrorMessage(e)
            }
            running = running - id
            _messages.send(message)
        }
    }

    fun delete() {
        val item = confirmDelete ?: return
        confirmDelete = null
        viewModelScope.launch { container.database.shortcutDao().delete(item.shortcut.id) }
    }
}

/**
 * Without a reply we can't know whether Firefly stored the transaction.
 * Trying again would send a new date, which Firefly wouldn't catch as a
 * duplicate, so warn. Errors that happen before anything is sent are safe.
 */
private fun runErrorMessage(e: Exception): UiMessage = when (e) {
    is OfflineException, is UnknownHostException, is ConnectException, is NoRouteToHostException ->
        UiMessage(errorText(e), MessageKind.ERROR)
    is IOException -> UiMessage(
        "No reply from Firefly. The transaction may have been created; check Firefly before trying again.",
        MessageKind.WARNING,
    )
    else -> UiMessage(errorText(e), MessageKind.ERROR)
}
