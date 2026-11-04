package com.mustafacanyucel.firefly_shortcuts.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

data class Settings(
    val serverUrl: String? = null,
    val clientId: String? = null,
    val redirectUrl: String = SettingsStore.DEFAULT_REDIRECT_URL,
    val lastSync: Instant? = null,
)

/**
 * The Firefly server and user the local database was synced from
 */
data class DataOwner(val server: String, val user: String)

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            serverUrl = prefs[SERVER_URL],
            clientId = prefs[CLIENT_ID],
            redirectUrl = prefs[REDIRECT_URL] ?: DEFAULT_REDIRECT_URL,
            lastSync = prefs[LAST_SYNC]?.let(Instant::ofEpochMilli),
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun saveConnection(serverUrl: String, clientId: String?, redirectUrl: String?) {
        dataStore.edit {
            it[SERVER_URL] = normalizeServerUrl(serverUrl)
            it.setOrRemove(CLIENT_ID, clientId?.trim()?.ifEmpty { null })
            it.setOrRemove(REDIRECT_URL, redirectUrl?.trim()?.ifEmpty { null })
        }
    }

    suspend fun setLastSync(time: Instant?) {
        dataStore.edit { it.setOrRemove(LAST_SYNC, time?.toEpochMilli()) }
    }

    suspend fun dataOwner(): DataOwner? {
        val prefs = dataStore.data.first()
        val server = prefs[OWNER_SERVER] ?: return null
        val user = prefs[OWNER_USER] ?: return null
        return DataOwner(server, user)
    }

    suspend fun setDataOwner(owner: DataOwner) {
        dataStore.edit {
            it[OWNER_SERVER] = owner.server
            it[OWNER_USER] = owner.user
        }
    }

    companion object {
        const val DEFAULT_REDIRECT_URL =
            "https://fireflyiiishortcuts.mustafacanyucel.com/oauth2redirect.html"

        fun normalizeServerUrl(url: String): String = url.trim().trimEnd('/')

        private val SERVER_URL = stringPreferencesKey("server_url")
        private val CLIENT_ID = stringPreferencesKey("client_id")
        private val REDIRECT_URL = stringPreferencesKey("redirect_url")
        private val LAST_SYNC = longPreferencesKey("last_sync")
        private val OWNER_SERVER = stringPreferencesKey("owner_server")
        private val OWNER_USER = stringPreferencesKey("owner_user")
    }
}

private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value == null) remove(key) else set(key, value)
}
