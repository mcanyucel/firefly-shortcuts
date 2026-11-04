package com.mustafacanyucel.firefly_shortcuts.data.secrets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class Secret {
    ACCESS_TOKEN,
    REFRESH_TOKEN,
    TOKEN_EXPIRY;

    internal val prefKey = stringPreferencesKey(name.lowercase())
}

class SecretStore (
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher
) {
    suspend fun get(secret: Secret): String? {
        val stored = dataStore.data.first()[secret.prefKey] ?: return null
        return withContext(Dispatchers.IO) { cipher.decrypt(stored)}
    }

    /**
     * Writes all [values] at once.
     * A null or empty value removes that secret.
     */
    suspend fun put(vararg values: Pair<Secret, String?>) {
        val encrypted = withContext(Dispatchers.IO) {
           values.map { (secret, value ) ->
               secret to value?.takeIf { it.isNotEmpty() }?.let(cipher::encrypt)
           }
        }
        dataStore.edit { prefs ->
            for ((secret, value) in encrypted) {
                if (value == null) prefs.remove(secret.prefKey) else prefs[secret.prefKey] = value
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
