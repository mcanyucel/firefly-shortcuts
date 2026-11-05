package com.mustafacanyucel.firefly_shortcuts.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.api.transactionDate
import kotlinx.coroutines.launch


/**
 * Receives taps on a widget row's ▶ button. Not exported, so only the
 * widget's own PendingIntent can reach it.
 */
class ShortcutRunReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_SHORTCUT_ID, 0)
        if (id <= 0) return
        val app = context.applicationContext
        val container = app.appContainer
        val pending = goAsync()
        container.appScope.launch {
            try {
                if (container.runStates.tryStart(id)) {
                    ShortcutRunWorker.enqueue(app, id, transactionDate())
                    ShortcutWidget.updateAll(app)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_SHORTCUT_ID = "shortcut_id"

        /** The tap action all rows share. Mutable, so each row can add its shortcut id. */
        fun template(context: Context): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getBroadcast(
                context, 0, Intent(context, ShortcutRunReceiver::class.java), flags,
            )
        }

        /** Merged into [template] when a row's button is tapped. */
        fun fillIn(id: Long): Intent = Intent().putExtra(EXTRA_SHORTCUT_ID, id)
    }
}
