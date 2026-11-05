package com.mustafacanyucel.firefly_shortcuts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mustafacanyucel.firefly_shortcuts.R

enum class MessageKind { SUCCESS, WARNING, ERROR }

/** Something to tell the user once, shown as a colored snackbar. */
data class UiMessage(val text: String, val kind: MessageKind)

private class StatusVisuals(val kind: MessageKind, override val message: String) : SnackbarVisuals {
    override val actionLabel: String? get() = null
    override val withDismissAction: Boolean get() = false
    // Problems stay up longer, so there's time to read them.
    override val duration: SnackbarDuration
        get() = if (kind == MessageKind.SUCCESS) SnackbarDuration.Short else SnackbarDuration.Long
}

suspend fun SnackbarHostState.show(message: UiMessage) {
    showSnackbar(StatusVisuals(message.kind, message.text))
}

/** A SnackbarHost that colors messages sent through [show]. */
@Composable
fun StatusSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        val kind = (data.visuals as? StatusVisuals)?.kind
        if (kind == null) {
            Snackbar(data)
            return@SnackbarHost
        }
        // Material's own Snackbar(data) adds this margin too.
        Snackbar(
            modifier = Modifier.padding(12.dp),
            containerColor = kind.background,
            contentColor = Color.White,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(kind.icon), contentDescription = null)
                Text(data.visuals.message)
            }
        }
    }
}

// Material's darker shades: white text is hard to read on the standard 500 green and orange.
private val MessageKind.background: Color
    get() = when (this) {
        MessageKind.SUCCESS -> Color(0xFF2E7D32)  // Green 800
        MessageKind.WARNING -> Color(0xFFE65100)  // Orange 900
        MessageKind.ERROR -> Color(0xFFC62828)    // Red 800
    }

private val MessageKind.icon: Int
    get() = when (this) {
        MessageKind.SUCCESS -> R.drawable.ic_check_circle
        MessageKind.WARNING -> R.drawable.ic_warning
        MessageKind.ERROR -> R.drawable.ic_error
    }
