package com.mustafacanyucel.firefly_shortcuts.widget

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.api.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Runs one shortcut tapped on the widget. The input holds only its id and the date. */
class ShortcutRunWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_SHORTCUT_ID, 0)
        val date = inputData.getString(KEY_DATE)
        if (id <= 0 || date == null) return Result.failure()

        val container = applicationContext.appContainer
        // Everything sent to Firefly comes from the database, never from the tap.
        val item = container.database.shortcutDao().getById(id)
        if (item == null) {
            Log.w(TAG, "Shortcut $id no longer exists")
            return finish(id, succeeded = false)
        }
        return try {
            container.runner.run(item, date)
            finish(id, succeeded = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e.isTransient() && runAttemptCount < MAX_ATTEMPTS - 1) {
                Log.i(TAG, "Run of shortcut $id failed, retrying", e)
                Result.retry()
            } else {
                Log.w(TAG, "Run of shortcut $id failed", e)
                finish(id, succeeded = false)
            }
        }
    }

    /** Shows the result, then redraws once it has expired so the row shows ▶ again. */
    private suspend fun finish(id: Long, succeeded: Boolean): Result {
        applicationContext.appContainer.runStates.finish(id, succeeded)
        ShortcutWidget.updateAll(applicationContext)
        delay(RunStateStore.RESULT_SHOWN_MS)
        ShortcutWidget.updateAll(applicationContext)
        return if (succeeded) Result.success() else Result.failure()
    }

    private fun Exception.isTransient() =
        this is IOException || (this is ApiException && isTransient)

    companion object {
        private const val TAG = "ShortcutRunWorker"
        private const val KEY_SHORTCUT_ID = "shortcut_id"
        private const val KEY_DATE = "date"
        private const val MAX_ATTEMPTS = 3

        /**
         * One run per shortcut at a time. [date] is fixed at the tap, so every
         * retry sends the same transaction and Firefly can spot a duplicate.
         */
        fun enqueue(context: Context, id: Long, date: String) {
            val request = OneTimeWorkRequestBuilder<ShortcutRunWorker>()
                .setInputData(workDataOf(KEY_SHORTCUT_ID to id, KEY_DATE to date))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("run_shortcut_$id", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
