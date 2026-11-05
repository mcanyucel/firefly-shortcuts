package com.mustafacanyucel.firefly_shortcuts.ui

import com.mustafacanyucel.firefly_shortcuts.data.api.ApiException
import com.mustafacanyucel.firefly_shortcuts.data.api.OfflineException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.openid.appauth.AuthorizationException
import java.io.IOException
import java.net.UnknownHostException

/** A short explanation of [e] for the user. */
fun errorText(e: Exception): String = when (e) {
    is ApiException -> when {
        e.isUnauthorized -> "Firefly rejected the token"
        else -> fireflyMessage(e.body)?.let { "Firefly: $it" } ?: "Firefly answered with error ${e.code}"
    }
    is AuthorizationException -> e.errorDescription ?: e.error ?: "Sign-in failed"
    is OfflineException -> "No internet connection"
    is UnknownHostException -> "There's no server at this address"
    is IOException -> "Can't reach the server (${e.message})"
    else -> e.message ?: e.toString()
}

/**
 * The most specific message in a Firefly error response: the first field
 * error if there is one ("errors": {"field": ["..."]}), else "message".
 */
private fun fireflyMessage(body: String): String? = try {
    val json = Json.parseToJsonElement(body).jsonObject
    val firstFieldError = json["errors"]?.jsonObject?.values?.firstOrNull()
        ?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
    firstFieldError ?: json["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null  // not JSON, e.g. a proxy's HTML error page
}
