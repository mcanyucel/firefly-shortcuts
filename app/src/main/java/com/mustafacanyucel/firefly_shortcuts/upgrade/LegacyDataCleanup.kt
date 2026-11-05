package com.mustafacanyucel.firefly_shortcuts.upgrade

import android.content.Context
import android.util.Log
import com.mustafacanyucel.firefly_shortcuts.SECRETS_KEY_ALIAS
import java.io.File
import java.security.KeyStore

/**
 * Deletes what the Flutter app (1.0.3 and older) left behind. 2.0.0 imports
 * none of it: users sign in and set up their shortcuts again.
 *
 * A fresh install never has these files, so finding them means an update
 * from 1.0.3 (or a restored 1.0.3 backup). Once they're deleted, this does
 * nothing. The native app's own data is never touched.
 */
object LegacyDataCleanup {
    private const val TAG = "LegacyDataCleanup"

    /** Flutter's getApplicationDocumentsDirectory(): the drift database and data_owner.json. */
    private const val FLUTTER_DIR = "app_flutter"

    private val FLUTTER_PREFS = listOf(
        "FlutterSecureStorage",
        "FlutterSecureKeyStorage",
        "FlutterSharedPreferences",
        "HomeWidgetPreferences",
    )

    fun run(context: Context) {
        val dataDir = context.dataDir
        val flutterDir = File(dataDir, FLUTTER_DIR)
        val prefs = FLUTTER_PREFS.filter { File(dataDir, "shared_prefs/$it.xml").exists() }
        if (!flutterDir.exists() && prefs.isEmpty()) return

        Log.i(TAG, "Deleting the Flutter app's data")
        deleteOldKeys()
        prefs.forEach { context.deleteSharedPreferences(it) }
        // Last, so an interrupted cleanup runs again on the next start.
        if (!flutterDir.deleteRecursively()) Log.w(TAG, "Couldn't delete $FLUTTER_DIR")
    }

    /** 1.0.3's widget key and flutter_secure_storage's keys: everything except ours. */
    private fun deleteOldKeys() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.aliases().toList()
                .filter { it != SECRETS_KEY_ALIAS }
                .forEach { keyStore.deleteEntry(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't delete the old Keystore keys", e)
        }
    }
}
