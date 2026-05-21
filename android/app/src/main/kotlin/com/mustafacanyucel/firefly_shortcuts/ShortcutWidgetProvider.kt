package com.mustafacanyucel.firefly_shortcuts

import android.appwidget.AppWidgetManager
  import android.appwidget.AppWidgetProvider
  import android.content.ComponentName
  import android.content.Context
  import android.content.Intent
  import android.net.Uri
  import android.os.Build
  import android.app.PendingIntent
  import android.widget.RemoteViews
  import androidx.work.OneTimeWorkRequestBuilder
  import androidx.work.WorkManager
  import androidx.work.workDataOf

  class ShortcutWidgetProvider : AppWidgetProvider() {

      companion object {
          const val ACTION_EXECUTE = "com.mustafacanyucel.firefly_shortcuts.ACTION_EXECUTE_SHORTCUT"
          const val EXTRA_SHORTCUT_ID = "shortcut_id"
          const val EXTRA_SHORTCUT_DATA = "shortcut_data"
          const val PREFS_NAME = "HomeWidgetPreferences"
          const val KEY_STATE_PREFIX = "exec_state_"

          fun updateAllWidgets(context: Context) {
              val manager = AppWidgetManager.getInstance(context)
              val ids = manager.getAppWidgetIds(
                  ComponentName(context, ShortcutWidgetProvider::class.java)
              )
              if (ids.isEmpty()) return
              ShortcutWidgetProvider().onUpdate(context, manager, ids)
          }
      }

      override fun onUpdate(
          context: Context,
          appWidgetManager: AppWidgetManager,
          appWidgetIds: IntArray
      ) {
          for (id in appWidgetIds) setupWidget(context, appWidgetManager, id)
      }

      override fun onReceive(context: Context, intent: Intent) {
          super.onReceive(context, intent)
          if (intent.action != ACTION_EXECUTE) return

          val shortcutId = intent.getIntExtra(EXTRA_SHORTCUT_ID, -1)
          val shortcutData = intent.getStringExtra(EXTRA_SHORTCUT_DATA) ?: return
          if (shortcutId == -1) return

          val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          val currentState = prefs.getString("$KEY_STATE_PREFIX$shortcutId", "idle")
          if (currentState != "idle") return

          prefs.edit().putString("$KEY_STATE_PREFIX$shortcutId", "executing").apply()
          updateAllWidgets(context)

          WorkManager.getInstance(context).enqueue(
              OneTimeWorkRequestBuilder<ShortcutExecuteWorker>()
                  .setInputData(workDataOf(
                      ShortcutExecuteWorker.KEY_SHORTCUT_ID to shortcutId,
                      ShortcutExecuteWorker.KEY_SHORTCUT_DATA to shortcutData,
                  ))
                  .build()
          )
      }

      private fun setupWidget(
          context: Context,
          appWidgetManager: AppWidgetManager,
          appWidgetId: Int
      ) {
          val serviceIntent = Intent(context, ShortcutListWidgetService::class.java).apply {
              putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
              data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
          }

          val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
          else PendingIntent.FLAG_UPDATE_CURRENT

          val templateIntent = PendingIntent.getBroadcast(
              context, 0,
              Intent(context, ShortcutWidgetProvider::class.java).apply {
                  action = ACTION_EXECUTE
              },
              flags
          )

          RemoteViews(context.packageName, R.layout.shortcut_widget).apply {
              setRemoteAdapter(R.id.widget_list_view, serviceIntent)
              setEmptyView(R.id.widget_list_view, android.R.id.empty)
              setPendingIntentTemplate(R.id.widget_list_view, templateIntent)
          }.also { appWidgetManager.updateAppWidget(appWidgetId, it) }

          appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
      }
  }