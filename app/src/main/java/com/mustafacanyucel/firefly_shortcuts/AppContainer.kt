package com.mustafacanyucel.firefly_shortcuts

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.secrets.KeystoreCipher
import com.mustafacanyucel.firefly_shortcuts.data.secrets.SecretStore
import com.mustafacanyucel.firefly_shortcuts.data.secrets.SettingsStore

private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

private val Context.secretsDataStore by preferencesDataStore(
    name = "secrets",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/**
 * The app's singletons. Activities, workers, and the widget reach it through [AppContainer].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val settings: SettingsStore by lazy { SettingsStore(appContext.settingsDataStore) }
    val secrets: SecretStore by lazy { SecretStore(appContext.secretsDataStore, KeystoreCipher("secrets")) }
}
