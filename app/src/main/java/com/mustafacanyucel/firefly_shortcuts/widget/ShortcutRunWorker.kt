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
import com.mustafacanyucel.firefly_shortcuts.data.api.transactionDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Runs one shortcut tapped on the widget. The input holds only its id, the
 * date and, for a shortcut that asks for it, the typed amount.
 */
class ShortcutRunWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_SHORTCUT_ID, 0)
        val date = inputData.getString(KEY_DATE)
        val amount = inputData.getString(KEY_AMOUNT)
        if (id <= 0 || date == null) return Result.failure()

        val container = applicationContext.appContainer
        // Everything else sent to Firefly comes from the database, never from the tap.
        val item = container.database.shortcutDao().getById(id)
        if (item == null) {
            Log.w(TAG, "Shortcut $id no longer exists")
            return finish(id, succeeded = false)
        }
        return try {
            container.runner.run(item, date, amount)
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
        private const val KEY_AMOUNT = "amount"
        private const val MAX_ATTEMPTS = 3

        /**
         * Starts a run of shortcut [id] from the widget, unless its row is busy
         * or still showing a result. Returns false if the tap was ignored.
         * [amount] is the typed amount for a shortcut that asks for one.
         */
        suspend fun start(context: Context, id: Long, amount: String? = null): Boolean {
            val app = context.applicationContext
            if (!app.appContainer.runStates.tryStart(id)) return false
            // Fixed now, so every retry sends the same transaction.
            enqueue(app, id, transactionDate(), amount)
            ShortcutWidget.updateAll(app)
            return true
        }

        /**
         * One run per shortcut at a time. [date] and [amount] are fixed at the tap,
         * so every retry sends the same transaction and Firefly can spot a duplicate.
         */
        private fun enqueue(context: Context, id: Long, date: String, amount: String?) {
            val request = OneTimeWorkRequestBuilder<ShortcutRunWorker>()
                .setInputData(workDataOf(KEY_SHORTCUT_ID to id, KEY_DATE to date, KEY_AMOUNT to amount))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("run_shortcut_$id", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
