package com.mustafacanyucel.firefly_shortcuts.data.api

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * A non-2xx response. Network failures surface as IOException instead.
 */
class ApiException(val code: Int, val body: String): Exception("HTTP $code") {
    val isUnauthorized: Boolean get() = code == 401

    /** The same request may succeed later. */
    val isTransient: Boolean get() = code == 408 || code == 429 || code >= 500
}

/** The server answered, but not like a Firefly III API. */
class NotFireflyException(code: Int) :
    Exception("There's no Firefly III API at this address (HTTP $code)")

/** The phone has no network at all, so nothing was sent. */
class OfflineException : IOException("No internet connection")

class FireflyApi(
    private val client: OkHttpClient,
    private val serverUrl: suspend () -> String?,
    private val accessToken: suspend() -> String?
) {

    suspend fun getAccounts() = getAllPages<AccountAttributes>("accounts")
    suspend fun getBills() = getAllPages<NamedAttributes>("bills")
    suspend fun getBudgets() = getAllPages<NamedAttributes>("budgets")
    suspend fun getCategories() = getAllPages<NamedAttributes>("categories")
    suspend fun getPiggyBanks() = getAllPages<NamedAttributes>("piggy-banks")
    suspend fun getTags() = getAllPages<TagAttributes>("tags")

    /**
     * Throws unless the server URL points at a Firefly III API. Used before
     * opening the sign-in page; without a token, the API answers 401.
     */
    suspend fun checkServer() {
        try {
            send(newRequest("about"))
        } catch (e: ApiException) {
            if (!e.isUnauthorized) throw NotFireflyException(e.code)
        }
    }

    /**
     * ID of the user the access token belongs to.
     */
    suspend fun getCurrentUserId(): String =
        json.decodeFromString<SingleResponse>(send(newRequest("about/user"))).data.id

    /**
     * A duplicate counts as success: with error_if_duplicate_hash, Firefly
     * only reports one when an earlier attempt of this request got through
     */
    suspend fun createTransaction(request: TransactionRequest) {
        val body = json.encodeToString(request).toRequestBody(JSON_MEDIA_TYPE)
        try {
            send(newRequest("transactions").post(body))
        } catch (e: ApiException) {
            if (e.code != 422 || DUPLICATE_MESSAGE !in e.body) throw e
        }
    }

    private suspend inline fun <reified A> getAllPages(path: String): List<Resource<A>> {
        val result = mutableListOf<Resource<A>>()
        var page = 1
        do {
            val parsed = json.decodeFromString<Page<A>>(send(newRequest("$path?page=$page")))
            result += parsed.data
            page++
        } while (parsed.meta.pagination.currentPage < parsed.meta.pagination.totalPages)
        return result
    }

    private suspend fun newRequest(path: String): Request.Builder {
        val base = serverUrl() ?: throw IllegalStateException("No server URL set")
        val builder = Request.Builder()
            .url("$base/api/v1/$path")
            .header("Accept", "application/vnd.api+json")
        accessToken()?.let { builder.header("Authorization", "Bearer $it")}
        return builder
    }

    /**
     * Returns the response body, or throws [ApiException] for a non-2xx status.
     */
    private suspend fun send(builder: Request.Builder): String = withContext(Dispatchers.IO) {
        client.newCall(builder.build()).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) throw ApiException(response.code, body)
            body
        }
    }

    companion object {
        // Firefly's 422 message when error_if_duplicate_hash matches
        private const val DUPLICATE_MESSAGE = "Duplicate of transaction"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        fun httpClient(context: Context): OkHttpClient {
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            return OkHttpClient.Builder()
                .addInterceptor { chain ->
                    // Without this, airplane mode fails at the DNS lookup, which
                    // looks the same as a mistyped server address.
                    if (connectivity.activeNetwork == null) throw OfflineException()
                    chain.proceed(chain.request())
                }
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

}
