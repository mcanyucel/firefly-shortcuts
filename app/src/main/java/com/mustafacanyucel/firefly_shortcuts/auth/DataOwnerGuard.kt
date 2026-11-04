package com.mustafacanyucel.firefly_shortcuts.auth

import com.mustafacanyucel.firefly_shortcuts.data.api.FireflyApi
import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.settings.DataOwner
import com.mustafacanyucel.firefly_shortcuts.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ties the local database to the Firefly server and user it was synced from.
 * Shortcuts store raw Firefly IDs, which mean something else on another server
 * or for another user, so the data is wiped when the owner changes.
 */
class DataOwnerGuard(
    private val api: FireflyApi,
    private val settings: SettingsStore,
    private val database: AppDatabase
) {
    /**
     * Call after signing in. Records the server and user as the owner, wiping local data first
     * it belonged to another. Returns whether it wiped. Throws if the user can't be identified.
     */
    suspend fun claim(): Boolean {
        val server = settings.current().serverUrl ?: error("No server URL set")
        val owner = DataOwner(server, api.getCurrentUserId())
        val previous = settings.dataOwner()

        val changed  = previous != null && previous != owner
        if (changed) {
            withContext(Dispatchers.IO) { database.clearAllTables() }
            settings.setLastSync(null)
        }
        settings.setDataOwner(owner)
        return changed
    }
}
