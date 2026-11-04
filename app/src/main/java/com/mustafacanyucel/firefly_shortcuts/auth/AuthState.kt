package com.mustafacanyucel.firefly_shortcuts.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.Instant
import java.util.Base64

data class AuthState(
    val accessToken: String? = null,
    /** Only OAuth sign-ins have one. A personal access token can't be refreshed. */
    val refreshToken: String? = null,
    /** Expiry of an OAuth access token. */
    val accessTokenExpiry: Instant? = null
) {
    val isAuthenticated: Boolean get() = accessToken != null
    val isOAuth: Boolean get() = refreshToken != null

    /** When the current token stops working, if known. */
    val tokenExpiry: Instant?
        get() = if (isOAuth) accessTokenExpiry else accessToken?.let(::jwtExpiry)

    /** Also true shortly before expiry, so no request goes out with a token about to lapse. */
    fun isExpired(now: Instant = Instant.now()): Boolean =
        tokenExpiry?.let { !now.isBefore(it.minus(EXPIRY_MARGIN))} ?: false

    private companion object {
        val EXPIRY_MARGIN: Duration = Duration.ofMinutes(2)
    }
}

/**
 * The `exp` claim of a JWT, which Firefly's personal access tokens are.
 * Passport writes it with a fraction when the issue time had microseconds.
 */
internal fun jwtExpiry(token: String): Instant? {
    val parts = token.split('.')
    if (parts.size != 3) return null

    return try {
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        Json.parseToJsonElement(payload).jsonObject["exp"]?.jsonPrimitive?.doubleOrNull
            ?.let { Instant.ofEpochMilli((it * 1000).toLong())}
    } catch (_: Exception) {
        null
    }
}
