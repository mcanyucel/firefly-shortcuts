package com.mustafacanyucel.firefly_shortcuts

import android.content.Context
  import androidx.work.CoroutineWorker
  import androidx.work.WorkerParameters

  class ShortcutStateResetWorker(
      private val context: Context,
      params: WorkerParameters
  ) : CoroutineWorker(context, params) {

      companion object {
          const val KEY_SHORTCUT_ID = "shortcut_id"
      }

      override suspend fun doWork(): Result {
          val shortcutId = inputData.getInt(KEY_SHORTCUT_ID, -1)
          if (shortcutId == -1) return Result.failure()

          context.getSharedPreferences(ShortcutWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
              .edit().remove("${ShortcutWidgetProvider.KEY_STATE_PREFIX}$shortcutId").apply()
          ShortcutWidgetProvider.updateAllWidgets(context)

          return Result.success()
      }
  }