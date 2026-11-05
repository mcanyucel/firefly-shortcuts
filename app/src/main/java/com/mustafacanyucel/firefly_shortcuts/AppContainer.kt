package com.mustafacanyucel.firefly_shortcuts

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.mustafacanyucel.firefly_shortcuts.auth.AuthManager
import com.mustafacanyucel.firefly_shortcuts.auth.DataOwnerGuard
import com.mustafacanyucel.firefly_shortcuts.data.api.FireflyApi
import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.secrets.KeystoreCipher
import com.mustafacanyucel.firefly_shortcuts.data.secrets.SecretStore
import com.mustafacanyucel.firefly_shortcuts.data.settings.SettingsStore
import com.mustafacanyucel.firefly_shortcuts.run.ShortcutRunner
import com.mustafacanyucel.firefly_shortcuts.sync.SyncManager
import com.mustafacanyucel.firefly_shortcuts.widget.RunStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    /**
     * api and auth may look like they have circular dependency, but actually api only needs the
     * definition of the [auth.accessToken()] as lambda, not the actual instance.
     */
    val api: FireflyApi by lazy { FireflyApi (
        FireflyApi.httpClient(appContext),
        serverUrl = { settings.current().serverUrl },
        accessToken = { auth.accessToken() }
    )}
    val auth: AuthManager by lazy { AuthManager (appContext, settings, secrets, DataOwnerGuard(api, settings, database))}
    val sync: SyncManager by lazy { SyncManager(api, database, settings) }
    val runner: ShortcutRunner by lazy { ShortcutRunner(api, database.shortcutDao()) }
    val runStates: RunStateStore by lazy { RunStateStore(appContext) }
    /** For work that must outlive the screen that started it, like a sync. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
