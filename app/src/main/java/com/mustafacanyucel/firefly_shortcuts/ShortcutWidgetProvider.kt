package com.mustafacanyucel.firefly_shortcuts

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.mustafacanyucel.firefly_shortcuts.widget.ShortcutWidget
import kotlinx.coroutines.launch

/**
 * Receives the system's widget updates. Keep this name and package: placed
 * widgets are bound to it, so 1.0.3's widgets keep working after the update.
 */
class ShortcutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // The redraw reads the database, so it can't run on the main thread.
        // goAsync keeps this receiver alive until it's done.
        val pending = goAsync()
        context.appContainer.appScope.launch {
            try {
                ShortcutWidget.update(context, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }
}
