package com.mustafacanyucel.firefly_shortcuts.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import com.mustafacanyucel.firefly_shortcuts.MainActivity
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.ShortcutWidgetProvider
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.auth.AuthState
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.ui.amountText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Draws the home screen widgets from the database and the sign-in state. */
object ShortcutWidget {
    private const val TAG = "ShortcutWidget"

    /** One redraw at a time, so an older one can't overwrite a newer one. */
    private val mutex = Mutex()

    private data class Row(val id: Long, val title: String, val detail: String)
    private data class Content(val rows: List<Row>, val needsSignIn: Boolean)

    /**
     * Redraws the widgets whenever the shortcuts or the sign-in change.
     * Started once per process, by FireflyApp.
     */
    fun keepUpdated(context: Context) {
        val container = context.appContainer
        container.appScope.launch {
            container.auth.load()
            combine(
                container.database.shortcutDao().observeAll(),
                container.auth.state.filterNotNull(),
            ) { shortcuts, auth -> content(shortcuts, auth) }
                // Skips changes the widget doesn't show, like lastUsed.
                .distinctUntilChanged()
                .collect { updateAll(context) }
        }
    }

    /** Redraws every placed widget. */
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ShortcutWidgetProvider::class.java))
        if (ids.isNotEmpty()) update(context, manager, ids)
    }

    /** Logs failures instead of throwing: a widget problem mustn't crash the app. */
    suspend fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        try {
            mutex.withLock {
                val container = context.appContainer
                // Read inside the lock, so the last redraw always has the latest data.
                val content = content(container.database.shortcutDao().getAll(), container.auth.load())
                for (id in ids) manager.updateAppWidget(id, views(context, id, content))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Widget update failed", e)
        }
    }

    private fun content(shortcuts: List<ShortcutDetail>, auth: AuthState) = Content(
        rows = shortcuts.map { item ->
            val s = item.shortcut
            Row(
                id = s.id,
                title = listOfNotNull(s.icon?.takeIf { it.isNotBlank() }, s.name).joinToString(" "),
                detail = "${item.fromAccount.name} → ${item.toAccount.name} · ${item.amountText}",
            )
        },
        needsSignIn = auth.needsSignIn(),
    )

    private fun views(context: Context, widgetId: Int, content: Content): RemoteViews {
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val items = RemoteViewsCompat.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .apply { content.rows.forEach { addItem(it.id, row(context, it)) } }
            .build()
        return RemoteViews(context.packageName, R.layout.shortcut_widget).apply {
            setOnClickPendingIntent(R.id.widget_title, openApp)
            setOnClickPendingIntent(R.id.widget_sign_in, openApp)
            setViewVisibility(R.id.widget_sign_in, if (content.needsSignIn) View.VISIBLE else View.GONE)
            setEmptyView(R.id.widget_list, R.id.widget_empty)
            RemoteViewsCompat.setRemoteAdapter(context, this, widgetId, R.id.widget_list, items)
        }
    }

    private fun row(context: Context, row: Row) =
        RemoteViews(context.packageName, R.layout.shortcut_widget_item).apply {
            setTextViewText(R.id.widget_shortcut_name, row.title)
            setTextViewText(R.id.widget_shortcut_detail, row.detail)
            setImageViewResource(R.id.widget_execute_button, R.drawable.ic_widget_play)
        }
}
