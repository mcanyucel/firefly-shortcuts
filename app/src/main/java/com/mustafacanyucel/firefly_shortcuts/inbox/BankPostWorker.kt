package com.mustafacanyucel.firefly_shortcuts.inbox

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.api.ApiException
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageStatus
import com.mustafacanyucel.firefly_shortcuts.ui.errorText
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Sends one bank message to Firefly with the choices from its review. On success the message is
 * deleted; on failure it's marked failed and can be reviewed again.
 */
class BankPostWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_MESSAGE_ID, 0)
        val review = inputData.toReview() ?: return Result.failure()
        val announce = inputData.getBoolean(KEY_ANNOUNCE, false)
        val container = applicationContext.appContainer
        val dao = container.database.bankMessageDao()
        val message = dao.getById(id) ?: return Result.failure()

        val account = container.database.accountDao().getAll().firstOrNull { it.id == review.accountId }
            ?: return fail(id, "the account no longer exists. Sync, then pick another one")
        return try {
            container.api.createTransaction(bankTransactionRequest(message, account, review))
            dao.delete(id)
            if (announce) {
                BankNotifications.sent(applicationContext, message)
            } else {
                BankNotifications.cancel(applicationContext, id)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e.isTransient() && runAttemptCount < MAX_ATTEMPTS - 1) {
                Log.i(TAG, "Sending bank message $id failed, retrying", e)
                Result.retry()
            } else {
                Log.w(TAG, "Sending bank message $id failed", e)
                fail(id, errorText(e))
            }
        }
    }

    private suspend fun fail(id: Long, error: String): Result {
        val dao = applicationContext.appContainer.database.bankMessageDao()
        dao.setStatus(id, BankMessageStatus.FAILED, error)
        dao.getById(id)?.let { BankNotifications.failed(applicationContext, it, error) }
        return Result.failure()
    }

    private fun Exception.isTransient() =
        this is IOException || (this is ApiException && isTransient)

    companion object {
        private const val TAG = "BankPostWorker"
        private const val KEY_MESSAGE_ID = "message_id"
        private const val KEY_ANNOUNCE = "announce"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_COUNTERPARTY = "counterparty"
        private const val KEY_FOREIGN_AMOUNT = "foreign_amount"
        private const val KEY_FOREIGN_CURRENCY = "foreign_currency"
        private const val KEY_CATEGORY = "category"
        private const val KEY_BUDGET = "budget"
        private const val KEY_TAGS = "tags"
        private const val MAX_ATTEMPTS = 3

        /**
         * Sends message [messageId] with [review]. [announce] shows a "Sent to Firefly" note
         * afterwards, for messages sent without review.
         */
        suspend fun send(context: Context, messageId: Long, review: BankReview, announce: Boolean = false) {
            val app = context.applicationContext
            app.appContainer.database.bankMessageDao().setStatus(messageId, BankMessageStatus.SENDING)
            val request = OneTimeWorkRequestBuilder<BankPostWorker>()
                .setInputData(
                    workDataOf(
                        KEY_MESSAGE_ID to messageId,
                        KEY_ANNOUNCE to announce,
                        KEY_ACCOUNT to review.accountId,
                        KEY_AMOUNT to review.amount,
                        KEY_COUNTERPARTY to review.counterparty,
                        KEY_FOREIGN_AMOUNT to review.foreignAmount,
                        KEY_FOREIGN_CURRENCY to review.foreignCurrencyCode,
                        KEY_CATEGORY to review.categoryId,
                        KEY_BUDGET to review.budgetId,
                        KEY_TAGS to review.tags.toTypedArray(),
                    )
                )
                // Waits for a connection instead of failing at once, e.g. right after a purchase abroad.
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(app)
                .enqueueUniqueWork("bank_message_$messageId", ExistingWorkPolicy.REPLACE, request)
        }

        private fun Data.toReview(): BankReview? = BankReview(
            accountId = getString(KEY_ACCOUNT) ?: return null,
            amount = getString(KEY_AMOUNT) ?: return null,
            counterparty = getString(KEY_COUNTERPARTY) ?: return null,
            foreignAmount = getString(KEY_FOREIGN_AMOUNT),
            foreignCurrencyCode = getString(KEY_FOREIGN_CURRENCY),
            categoryId = getString(KEY_CATEGORY),
            budgetId = getString(KEY_BUDGET),
            tags = getStringArray(KEY_TAGS)?.toList().orEmpty(),
        )
    }
}
