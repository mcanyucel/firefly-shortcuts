package com.mustafacanyucel.firefly_shortcuts.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.appContainer
import com.mustafacanyucel.firefly_shortcuts.ui.home.HomeScreen
import com.mustafacanyucel.firefly_shortcuts.ui.settings.SettingsScreen

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Composable
fun AppNavHost() {
    val auth = LocalContext.current.appContainer.auth
    val authState by auth.state.collectAsStateWithLifecycle()
    LaunchedEffect(auth) { auth.load() }

    // Nothing to show until the stored tokens are read, which takes a moment.
    val signedIn = authState?.isAuthenticated ?: return

    val navController = rememberNavController()
    val startRoute: Any = remember { if (signedIn) HomeRoute else SettingsRoute }
    val currentEntry by navController.currentBackStackEntryAsState()

    // Signed out (logout, or a revoked refresh token): go to settings and
    // clear the back stack. Settings handles its own sign-in failures.
    LaunchedEffect(signedIn, currentEntry) {
        val onSettings = currentEntry?.destination?.hasRoute<SettingsRoute>() ?: true
        if (!signedIn && !onSettings) {
            navController.navigate(SettingsRoute) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {
        composable<HomeRoute> {
            HomeScreen(onOpenSettings = { navController.navigate(SettingsRoute) })
        }
        composable<SettingsRoute> {
            val canGoBack = remember { navController.previousBackStackEntry != null }
            SettingsScreen(
                canGoBack = canGoBack,
                onBack = { navController.popBackStack() },
                onConnected = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(HomeRoute) {
                            popUpTo<SettingsRoute> { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}
