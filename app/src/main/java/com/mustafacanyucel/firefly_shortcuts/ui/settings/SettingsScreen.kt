package com.mustafacanyucel.firefly_shortcuts.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mustafacanyucel.firefly_shortcuts.R
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.auth.AuthState
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    canGoBack: Boolean,
    onBack: () -> Unit,
    onConnected: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val authState by vm.authState.collectAsStateWithLifecycle()
    val signedIn = authState?.isAuthenticated == true
    val snackbar = remember { SnackbarHostState() }
    val currentOnConnected by rememberUpdatedState(onConnected)

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> vm.onSignInResult(result.data) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is SettingsEvent.LaunchSignIn -> signInLauncher.launch(event.intent)
                is SettingsEvent.Message -> launch { snackbar.showSnackbar(event.text) }
                is SettingsEvent.Connected -> {
                    if (event.dataCleared) {
                        Toast.makeText(
                            context,
                            "Shortcuts from the previous Firefly account were removed",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    currentOnConnected()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Server")
            OutlinedTextField(
                value = vm.serverUrl,
                onValueChange = { vm.serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://firefly.example.com") },
                singleLine = true,
                enabled = !signedIn && !vm.busy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!signedIn) {
                SectionTitle("Personal access token")
                OutlinedTextField(
                    value = vm.token,
                    onValueChange = { vm.token = it },
                    label = { Text("Personal access token") },
                    supportingText = {
                        Text(
                            "Firefly → Profile → OAuth → Personal access tokens. Used instead of OAuth when filled in.\n" +
                                "If Firefly shows the new token as blank, open the browser's developer tools " +
                                "before creating it and copy accessToken from the response."
                        )
                    },
                    singleLine = true,
                    enabled = !vm.busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionTitle("OAuth")
                OutlinedTextField(
                    value = vm.clientId,
                    onValueChange = { vm.clientId = it },
                    label = { Text("Client ID") },
                    supportingText = {
                        Text("Doesn't work if Firefly signs you in through a proxy like Authelia; use a personal access token instead.")
                    },
                    singleLine = true,
                    enabled = !vm.busy,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.redirectUrl,
                    onValueChange = { vm.redirectUrl = it },
                    label = { Text("Redirect URL") },
                    supportingText = { Text("Must match the redirect URL of the client in Firefly.") },
                    singleLine = true,
                    enabled = !vm.busy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Connection")
            authState?.let { ConnectionStatus(it) }
            if (signedIn) {
                OutlinedButton(onClick = vm::disconnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Disconnect")
                }
            } else {
                Button(onClick = vm::connect, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
                    if (vm.busy) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Connect")
                    }
                }
            }
        }
    }

    vm.serverSwitch?.let { switch ->
        AlertDialog(
            onDismissRequest = vm::cancelServerSwitch,
            title = { Text("Switch server?") },
            text = {
                val noun = if (switch.shortcutCount == 1) "shortcut" else "shortcuts"
                Text(
                    "Your ${switch.shortcutCount} $noun belong to ${switch.ownerServer}. " +
                            "Connecting to ${switch.newServer} deletes them, along with the " +
                            "synced accounts, categories and tags."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = vm::confirmServerSwitch,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete and connect") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelServerSwitch) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

private data class StatusLine(@DrawableRes val icon: Int, val color: Color, val text: String)

@Composable
private fun ConnectionStatus(state: AuthState) {
    val colors = MaterialTheme.colorScheme
    val ok = R.drawable.ic_check_circle
    val status = when {
        !state.isAuthenticated -> StatusLine(R.drawable.ic_error, colors.error, "Not connected")
        state.isOAuth -> StatusLine(ok, colors.primary, "Connected with OAuth")
        else -> {
            val expiry = state.tokenExpiry
            val now = Instant.now()
            when {
                expiry == null -> StatusLine(ok, colors.primary, "Connected with an access token")
                !now.isBefore(expiry) -> StatusLine(
                    R.drawable.ic_error, colors.error,
                    "The access token has expired. Disconnect and enter a new one.",
                )
                else -> {
                    val days = Duration.between(now, expiry).toDays()
                    when {
                        days < 1 -> StatusLine(R.drawable.ic_warning, colors.tertiary, "The access token expires today")
                        days <= 30 -> StatusLine(R.drawable.ic_warning, colors.tertiary, "The access token expires in $days days")
                        else -> StatusLine(ok, colors.primary, "Connected with an access token ($days days left)")
                    }
                }
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(status.icon), contentDescription = null, tint = status.color, modifier = Modifier.size(20.dp))
        Text(status.text)
    }
}
