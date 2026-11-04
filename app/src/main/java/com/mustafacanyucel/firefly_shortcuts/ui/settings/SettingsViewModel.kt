package com.mustafacanyucel.firefly_shortcuts.ui.settings

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.auth.AuthState
import com.mustafacanyucel.firefly_shortcuts.auth.LoginResult
import com.mustafacanyucel.firefly_shortcuts.data.api.ApiException
import com.mustafacanyucel.firefly_shortcuts.data.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.UnknownHostException

data class ServerSwitch(val shortcutCount: Int, val ownerServer: String, val newServer: String)

/**
 * Things that happen once, as opposed to state the screen shows.
 */
sealed interface SettingsEvent {
    data class LaunchSignIn(val intent: Intent): SettingsEvent
    data class Connected(val dataCleared: Boolean): SettingsEvent
    data class Message(val text: String): SettingsEvent
}

class SettingsViewModel(private val container: AppContainer): ViewModel() {
    val authState: StateFlow<AuthState?> = container.auth.state

    /**
     * Compose state rather than StateFlow: a text field must get its new value back in the
     * same frame, or typing fast loses characters.
     */
    var serverUrl by mutableStateOf("")
    var clientId by mutableStateOf("")
    var redirectUrl by mutableStateOf("")
    var token by mutableStateOf("")

    var loaded by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var serverSwitch by mutableStateOf<ServerSwitch?>(null)
        private set

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val s = container.settings.current()
            serverUrl = s.serverUrl.orEmpty()
            clientId = s.clientId.orEmpty()
            redirectUrl = s.redirectUrl
            loaded = true
        }
    }

    fun connect() {
        if (busy) return
        val server = validServerUrl() ?: return
        if (token.isBlank() && clientId.isBlank()) {
            message("Enter a personal access token or an OAuth client ID")
            return
        }
        busy = true
        viewModelScope.launch {
            /**
             * Connecting to another server wipes the shortcuts, so ask the user first.
             * A different user on the same server is only known after sign-in.
             */
            val owner = container.settings.dataOwner()
            val count = container.database.shortcutDao().count()
            if (owner != null && owner.server != server && count > 0) {
                serverSwitch = ServerSwitch(count, owner.server, server)
                busy = false
            }
            else {
                signIn(server)
            }
        }
    }

    fun confirmServerSwitch() {
        val switch = serverSwitch ?: return
        serverSwitch = null
        busy = true
        viewModelScope.launch { signIn(switch.newServer) }
    }

    fun cancelServerSwitch() {
        serverSwitch = null
    }

    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val result = container.auth.completeOAuth(data)
                if (result == LoginResult.CANCELLED) message("Sign-in cancelled") else connected(result)
            } catch(e: CancellationException) {
                throw e
            } catch(e: Exception) {
                message(errorText(e))
            }
            busy = false
        }
    }

    fun disconnect() {
        viewModelScope.launch { container.auth.logout() }
    }

    private suspend fun signIn(server:String) {
        try {
            container.settings.saveConnection(
                serverUrl = server,
                clientId = clientId.trim().ifEmpty { null },
                redirectUrl = redirectUrl.trim().ifEmpty { null }
            )
            val pat = token.filterNot { it.isWhitespace() }
            if (pat.isEmpty()) {
                // Catch a wrong address here rather than on an error page in the browser.
                container.api.checkServer()
                // Stays busy until the browser comes back to onSignInResult
                _events.send(SettingsEvent.LaunchSignIn(container.auth.authorizationIntent()))
                return
            }
            connected(container.auth.loginWithPat(pat))
        } catch(e: CancellationException) {
            throw e
        } catch(e:Exception) {
            message(errorText(e))
        }
        busy = false
    }

    private suspend fun connected(result: LoginResult) {
        token = ""
        // Nothing synced yet (first sign-in, or the old data was wiped): there
        // would be no accounts to build a shortcut from.
        if (container.settings.current().lastSync == null) {
            container.appScope.launch { container.sync.syncAll() }
        }
        _events.send(SettingsEvent.Connected(dataCleared = result == LoginResult.CONNECTED_DATA_CLEARED))
    }

    private fun message(text: String) {
        _events.trySend(SettingsEvent.Message(text))
    }

    /**
     * The cleaned-up server URL, or null after telling the user what's wrong.
     */
    private fun validServerUrl(): String? {
        var url = SettingsStore.normalizeServerUrl(serverUrl)
        if (url.isEmpty()) {
            message("Enter the server URL")
            return null
        }
        if ("://" !in url) url = "https://$url"
        val parsed = url.toHttpUrlOrNull()
        when {
            parsed == null -> message("That isn't a valid URL")
            !parsed.isHttps -> message("Firefly must be reached over https://")
            else -> {
                serverUrl = url
                return url
            }
        }
        return null
    }
}

private fun errorText(e: Exception): String = when (e) {
    is ApiException ->
        if (e.isUnauthorized) "Firefly rejected the token" else "Firefly answered with error ${e.code}"
    is AuthorizationException -> e.errorDescription ?: e.error ?: "Sign-in failed"
    is UnknownHostException -> "There's no server at this address"
    is IOException -> "Can't reach the server (${e.message})"
    else -> e.message ?: e.toString()
}
