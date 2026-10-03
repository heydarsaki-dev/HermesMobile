package dev.helmlab.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.helmlab.hermes.ui.HermesViewModel
import dev.helmlab.hermes.ui.screens.ChatScreen
import dev.helmlab.hermes.ui.screens.SessionsScreen
import dev.helmlab.hermes.ui.screens.SettingsScreen
import dev.helmlab.hermes.ui.theme.HermesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: HermesViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            val toast by vm.toast.collectAsState()
            val snackbar = remember { SnackbarHostState() }
            val nav = rememberNavController()

            LaunchedEffect(toast) {
                toast?.let {
                    snackbar.showSnackbar(it)
                    vm.clearToast()
                }
            }

            HermesTheme(settings) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbar) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { _ ->
                    NavHost(nav, startDestination = "chat") {
                        composable(
                            "chat",
                            enterTransition = { fadeIn(tween(220)) },
                            exitTransition = { fadeOut(tween(160)) }
                        ) {
                            ChatScreen(
                                vm = vm,
                                onOpenSettings = { nav.navigate("settings") },
                                onOpenSessions = { nav.navigate("sessions") }
                            )
                        }
                        composable(
                            "settings",
                            enterTransition = { slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(220)) },
                            exitTransition = { slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200)) }
                        ) {
                            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
                        }
                        composable(
                            "sessions",
                            enterTransition = { slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(220)) },
                            exitTransition = { slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200)) }
                        ) {
                            SessionsScreen(vm = vm, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}