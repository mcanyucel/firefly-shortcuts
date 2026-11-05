package com.mustafacanyucel.firefly_shortcuts.widget

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutDetail
import com.mustafacanyucel.firefly_shortcuts.model.normalizeAmount
import com.mustafacanyucel.firefly_shortcuts.ui.AmountField
import com.mustafacanyucel.firefly_shortcuts.ui.currencyText
import com.mustafacanyucel.firefly_shortcuts.ui.label
import com.mustafacanyucel.firefly_shortcuts.ui.rememberAmountState
import com.mustafacanyucel.firefly_shortcuts.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Where taps on a widget row's ▶ button land. A shortcut with a fixed amount
 * starts at once, and this closes without showing anything. One that asks for
 * the amount shows a small prompt over the home screen.
 *
 * An activity rather than a broadcast receiver, because Android doesn't let a
 * background broadcast open a window. Not exported, so only the widget's own
 * PendingIntent can open it. Only the shortcut id comes from the tap: the rest
 * of the transaction is read from the database, and the amount is typed here.
 */
class WidgetTapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_SHORTCUT_ID, 0)
        if (id <= 0) {
            closeQuietly()
            return
        }
        val container = appContainer
        lifecycleScope.launch {
            val item = container.database.shortcutDao().getById(id)
            when {
                item == null -> closeQuietly()
                !item.shortcut.askAmount -> {
                    // The app scope, because this activity closes right away.
                    container.appScope.launch { ShortcutRunWorker.start(applicationContext, id) }
                    closeQuietly()
                }
                // Busy or showing a result: ignore the tap, as for fixed shortcuts.
                withContext(Dispatchers.IO) { container.runStates.get(id) } != RunState.IDLE -> closeQuietly()
                else -> setContent {
                    AppTheme {
                        AmountPrompt(item, onSave = { amount -> save(id, amount) }, onCancel = ::finish)
                    }
                }
            }
        }
    }

    private fun save(id: Long, amount: String) {
        appContainer.appScope.launch { ShortcutRunWorker.start(applicationContext, id, amount) }
        finish()
    }

    /** Closes without an animation, since nothing was shown. */
    private fun closeQuietly() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val EXTRA_SHORTCUT_ID = "shortcut_id"

        /** The tap action all rows share. Mutable, so each row can add its shortcut id. */
        fun template(context: Context): PendingIntent {
            val intent = Intent(context, WidgetTapActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getActivity(context, 0, intent, flags)
        }

        /** Merged into [template] when a row's button is tapped. */
        fun fillIn(id: Long): Intent = Intent().putExtra(EXTRA_SHORTCUT_ID, id)
    }
}

/** A card over a dimmed home screen. Tapping outside it cancels. */
@Composable
private fun AmountPrompt(item: ShortcutDetail, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val s = item.shortcut
    var amount by rememberAmountState(s.amount)
    var showError by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val save: () -> Unit = {
        val normalized = normalizeAmount(amount.text)
        if (normalized == null) {
            showError = true
        } else {
            onSave(normalized)
        }
    }

    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            )
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Surface takes the taps on the card, so they don't reach the dimmed area.
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        listOfNotNull(s.icon?.takeIf { it.isNotBlank() }, s.name).joinToString(" "),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${s.transactionType.label} · ${item.fromAccount.name} → ${item.toAccount.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AmountField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        showError = false
                    },
                    isError = showError,
                    currency = item.currencyText,
                    modifier = Modifier.focusRequester(focus),
                    onDone = save,
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    Button(onClick = save) { Text("Save") }
                }
            }
        }
    }
}
