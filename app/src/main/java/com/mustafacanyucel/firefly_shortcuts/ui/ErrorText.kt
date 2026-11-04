package com.mustafacanyucel.firefly_shortcuts.ui

import com.mustafacanyucel.firefly_shortcuts.data.api.ApiException
import net.openid.appauth.AuthorizationException
import java.io.IOException
import java.net.UnknownHostException

/** A short explanation of [e] for the user. */
fun errorText(e: Exception): String = when (e) {
    is ApiException ->
        if (e.isUnauthorized) "Firefly rejected the token" else "Firefly answered with error ${e.code}"
    is AuthorizationException -> e.errorDescription ?: e.error ?: "Sign-in failed"
    is UnknownHostException -> "There's no server at this address"
    is IOException -> "Can't reach the server (${e.message})"
    else -> e.message ?: e.toString()
}
