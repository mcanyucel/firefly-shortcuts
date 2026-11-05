package com.mustafacanyucel.firefly_shortcuts

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.mustafacanyucel.firefly_shortcuts.widget.ShortcutWidget
import kotlinx.coroutines.launch

/**
 * Receives the system's widget updates. Keep this name and package: placed
 * widgets are bound to it, so 1.0.3's widgets keep working after the update.
 */
class ShortcutWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Placed widgets still hold the previous version's tap action until redrawn.
            redraw(context) { ShortcutWidget.updateAll(context) }
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        redraw(context) { ShortcutWidget.update(context, appWidgetManager, appWidgetIds) }
    }

    /**
     * The redraw reads the database, so it can't run on the main thread.
     * goAsync keeps this receiver alive until it's done.
     */
    private fun redraw(context: Context, block: suspend () -> Unit) {
        val pending = goAsync()
        context.appContainer.appScope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }
}
