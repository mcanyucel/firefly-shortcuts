package com.mustafacanyucel.firefly_shortcuts.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64


class AuthStateTest {
    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("""{"alg":"none"}""", payload, "sig")
            .joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }

    @Test
    fun patExpiryComesFromJwt() {
        val state = AuthState(accessToken = jwt("""{"exp":1893456000}"""))
        assertEquals(Instant.ofEpochSecond(1893456000), state.tokenExpiry)
    }

    @Test
    fun fractionalExpIsRead() {
        val state = AuthState(accessToken = jwt("""{"exp":1893456000.5}"""))
        assertEquals(Instant.ofEpochMilli(1893456000500), state.tokenExpiry)
    }

    @Test
    fun oauthUsesStoredExpiryWithMargin() {
        val expiry = Instant.parse("2026-12-01T00:00:00Z")
        val state = AuthState(
            accessToken = jwt("""{"exp":1}"""),
            refreshToken = "refresh",
            accessTokenExpiry = expiry,
        )
        assertEquals(expiry, state.tokenExpiry)
        assertTrue(state.isExpired(now = expiry.minusSeconds(60)))
        assertFalse(state.isExpired(now = expiry.minusSeconds(600)))
    }

    @Test
    fun opaqueTokenHasNoExpiry() {
        val state = AuthState(accessToken = "not-a-jwt")
        assertNull(state.tokenExpiry)
        assertFalse(state.isExpired())
    }

    @Test
    fun needsSignInOnlyWhenRefreshCantHelp() {
        val exp = Instant.ofEpochSecond(1893456000)
        val pat = AuthState(accessToken = jwt("""{"exp":1893456000}"""))
        val oauth = AuthState(accessToken = "a", refreshToken = "r", accessTokenExpiry = exp)

        assertTrue(AuthState().needsSignIn())
        assertTrue(pat.needsSignIn(now = exp))
        assertFalse(pat.needsSignIn(now = exp.minusSeconds(3600)))
        assertFalse(oauth.needsSignIn(now = exp))
    }
}
