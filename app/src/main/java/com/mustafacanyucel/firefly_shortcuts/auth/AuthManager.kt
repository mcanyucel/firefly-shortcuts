package com.mustafacanyucel.firefly_shortcuts.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mustafacanyucel.firefly_shortcuts.data.secrets.Secret
import com.mustafacanyucel.firefly_shortcuts.data.secrets.SecretStore
import com.mustafacanyucel.firefly_shortcuts.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import okio.IOException
import java.time.Instant
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class LoginResult { CANCELLED, CONNECTED, CONNECTED_DATA_CLEARED }

class AuthManager(
    private val context: Context,
    private val settings: SettingsStore,
    private val secrets: SecretStore,
    private val guard: DataOwnerGuard
    ) {
    private val _state = MutableStateFlow<AuthState?>(null)

    /** Null until [load] or the first [accessToken] call has read storage. */
    val state: StateFlow<AuthState?> = _state.asStateFlow()

    /**
     * Guards the state and the stored tokens. Also makes concurrent callers share one refresh:
     * Firefly revokes a refresh token once it is used, so a seonc parallel refresh would fail.
     */
    private val mutex = Mutex()

    suspend fun load(): AuthState = mutex.withLock { loadedState() }

    /** A usable access token, refreshed first if needed. Null when signed out. */
    suspend fun accessToken(): String? = mutex.withLock {
        val current = loadedState()
        if (current.isOAuth && current.isExpired()) refresh(current).accessToken
        else current.accessToken
    }

    suspend fun loginWithPat(token: String): LoginResult {
        mutex.withLock { store(AuthState(accessToken = token.trim())) }
        return finishLogin()
    }

    /** The sign-in page. Start it for a result and pass that to [completeOAuth]. */
    suspend fun authorizationIntent(): Intent {
        val s = settings.current()
        val server = s.serverUrl ?: error("No server URL set")
        val clientId = s.clientId ?: error("No client ID set")
        val request = AuthorizationRequest.Builder(
            serviceConfig(server),
            clientId,
            ResponseTypeValues.CODE,
            s.redirectUrl.toUri(),
        ).build()
        return withAuthService { it.getAuthorizationRequestIntent(request) }
    }

    suspend fun completeOAuth(data: Intent?): LoginResult {
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        if (response == null) {
            val error = data?.let { AuthorizationException.fromIntent(it) }
            if (error == null || error == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW) {
                return LoginResult.CANCELLED
            }
            throw error
        }
        val tokens = withAuthService { it.token(response.createTokenExchangeRequest()) }
        mutex.withLock { store(tokens.toAuthState(fallbackRefreshToken = null)) }
        return finishLogin()
    }

    suspend fun logout() {
        mutex.withLock { store(AuthState()) }
    }

    // Claiming the local data is the first authenticated request, so it also
    // tests the connection. Without knowing the user we can't tell whether the
    // local shortcuts belong to them, so a failure signs out again.
    // Must run without the mutex: claim() calls the API, which calls accessToken().
    private suspend fun finishLogin(): LoginResult {
        val cleared = try {
            guard.claim()
        } catch (e: Exception) {
            withContext(NonCancellable) { logout() }
            throw e
        }
        return if (cleared) LoginResult.CONNECTED_DATA_CLEARED else LoginResult.CONNECTED
    }

    // !NOTE: The private functions below expect teh mutex to be held.

    private suspend fun loadedState(): AuthState {
        _state.value?.let { return it }
        val loaded = AuthState(
            accessToken = secrets.get(Secret.ACCESS_TOKEN),
            refreshToken = secrets.get(Secret.REFRESH_TOKEN),
            accessTokenExpiry = secrets.get(Secret.TOKEN_EXPIRY)
                ?.toLongOrNull()?.let(Instant::ofEpochMilli)
        )
        _state.value = loaded
        return loaded
    }

    private suspend fun store(state: AuthState) {
        secrets.put(
            Secret.ACCESS_TOKEN to state.accessToken,
            Secret.REFRESH_TOKEN to state.refreshToken,
            Secret.TOKEN_EXPIRY to state.accessTokenExpiry?.toEpochMilli()?.toString()
        )
        _state.value = state
    }

    /**
     * Signs out if the server rejects the refresh token: throws IOException if unreachable.
     */
    private suspend fun refresh(current: AuthState): AuthState {
        val s = settings.current()
        val server = s.serverUrl ?: return current
        val clientId = s.clientId ?: return current
        val request = TokenRequest.Builder(serviceConfig(server), clientId)
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setRefreshToken(current.refreshToken)
            .build()

        val tokens = try {
            withAuthService { it.token(request) }
        } catch (e: AuthorizationException) {
            if (e.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
                // Revoked or expired: only a new sign-in helps.
                store(AuthState())
                return AuthState()
            }
            throw IOException("Token refresh failed", e)
        }
        val refreshed = tokens.toAuthState(fallbackRefreshToken = current.refreshToken)
        store(refreshed)
        return refreshed
    }

    private fun serviceConfig(server: String) = AuthorizationServiceConfiguration(
        "$server/oauth/authorize".toUri(),
        "$server/oauth/token".toUri()
    )

    private suspend fun <T> withAuthService(block: suspend (AuthorizationService) -> T): T {
        val service = AuthorizationService(context)
        try {
            return block(service)
        } finally {
            service.dispose()
        }
    }
}

private fun TokenResponse.toAuthState(fallbackRefreshToken: String?) = AuthState(
    accessToken = checkNotNull(accessToken) { "No access token in response" },
    refreshToken = refreshToken ?: fallbackRefreshToken,
    accessTokenExpiry = accessTokenExpirationTime?.let(Instant::ofEpochMilli)
)

/**
 * AppAuth's callback API as a suspend function. Started on the main thread
 * because AppAuth runs the request as an AsyncTask.
 */
private suspend fun AuthorizationService.token(request: TokenRequest): TokenResponse =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            performTokenRequest(request) { response, error ->
                if (response != null) {
                    cont.resume(response)
                } else {
                    cont.resumeWithException(
                        error ?: AuthorizationException.GeneralErrors.SERVER_ERROR
                    )
                }
            }
        }
    }
