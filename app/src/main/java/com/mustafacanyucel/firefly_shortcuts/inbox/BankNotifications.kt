package com.mustafacanyucel.firefly_shortcuts.inbox

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mustafacanyucel.firefly_shortcuts.MainActivity
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.data.db.BankMessageEntity
import com.mustafacanyucel.firefly_shortcuts.ui.sms.BankReviewActivity

/** "644.00 TL · V A N T A" */
val BankMessageEntity.summary: String
    get() = listOfNotNull(
        amount?.let { listOfNotNull(it, currency).joinToString(" ") } ?: "No amount",
        merchant,
    ).joinToString(" · ")

/** Notifications about bank SMS. Each message has its own, replaced as its state changes. */
object BankNotifications {
    private const val CHANNEL = "bank_sms"
    // Message ids start at 1; keep clear of other notification ids.
    private const val ID_OFFSET = 1000
    private const val UNREADABLE_ID = 999

    fun allowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** A message waiting for review. */
    fun review(context: Context, message: BankMessageEntity) = show(
        context, message.id.toInt() + ID_OFFSET, message.summary,
        "Tap to review it and send it to Firefly", reviewIntent(context, message.id),
    )

    fun failed(context: Context, message: BankMessageEntity, error: String) = show(
        context, message.id.toInt() + ID_OFFSET, message.summary,
        "Couldn't send it to Firefly: $error. Tap to try again.", reviewIntent(context, message.id),
    )

    /** Sent without review: a quiet note, so it doesn't happen unseen. */
    fun sent(context: Context, message: BankMessageEntity) = show(
        context, message.id.toInt() + ID_OFFSET, message.summary, "Sent to Firefly",
        appIntent(context), silent = true,
    )

    /** The wording matched a template, but a field couldn't be read. */
    fun unreadable(context: Context, sender: String, template: String, problem: String) = show(
        context, UNREADABLE_ID, "Couldn't read an SMS from $sender",
        "Template \"$template\": $problem", appIntent(context),
    )

    fun cancel(context: Context, messageId: Long) =
        NotificationManagerCompat.from(context).cancel(messageId.toInt() + ID_OFFSET)

    private fun show(
        context: Context,
        id: Int,
        title: String,
        text: String,
        intent: PendingIntent,
        silent: Boolean = false,
    ) {
        if (!allowed(context)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Bank SMS")
                .setDescription("Bank SMS that your templates read")
                .build()
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setSilent(silent)
            .build()
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // The permission was taken away just now.
        }
    }

    private fun reviewIntent(context: Context, messageId: Long): PendingIntent = PendingIntent.getActivity(
        context, messageId.toInt(), BankReviewActivity.intent(context, messageId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun appIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
}
