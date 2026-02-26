package com.example.pgk_food.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pgk_food.core.feedback.FeedbackController
import com.example.pgk_food.core.feedback.FeedbackLevel
import com.example.pgk_food.core.session.SessionEvent
import com.example.pgk_food.core.session.SessionManager
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.data.repository.NotificationRepository
import com.example.pgk_food.model.UserRole
import com.example.pgk_food.ui.screens.LoginScreen
import com.example.pgk_food.ui.screens.MainScreen
import com.example.pgk_food.ui.screens.SettingsScreen
import com.example.pgk_food.ui.screens.SplashScreen
import com.example.pgk_food.ui.viewmodels.AuthViewModel
import com.example.pgk_food.util.NetworkMonitor
import com.example.pgk_food.util.NotificationAutoRefreshBus
import com.example.pgk_food.util.UiSettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class RootScreen(val route: String) {
    data object Splash : RootScreen("splash")
    data object Login : RootScreen("login")
    data object MainShell : RootScreen("main_shell")
    data object Settings : RootScreen("settings")
}

@Suppress("UNCHECKED_CAST")
@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    database: AppDatabase
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val uiSettingsManager = remember { UiSettingsManager(context) }
    val notificationRepository = remember { NotificationRepository() }
    val scope = rememberCoroutineScope()
    val session by authRepository.getUserSession().collectAsState(initial = null)

    val authViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

    LaunchedEffect(Unit) {
        FeedbackController.messages.collect { message ->
            if (message.level == FeedbackLevel.Error) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (message.level == FeedbackLevel.Success) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            snackbarHostState.showSnackbar(message.text, message.actionLabel)
        }
    }

    LaunchedEffect(Unit) {
        SessionManager.events.collect { event ->
            if (event is SessionEvent.LogoutRequired) {
                authViewModel.logout()
                snackbarHostState.showSnackbar("Сессия истекла. Войдите снова.")
                navController.navigate(RootScreen.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    suspend fun refreshNotificationsSilently() {
        val currentSession = session ?: return
        notificationRepository.getUnreadCount(currentSession.token)
        notificationRepository.getNotifications(currentSession.token, cursor = null)
        if (UserRole.CURATOR in currentSession.roles) {
            notificationRepository.getRosterDeadline(currentSession.token)
        }
        NotificationAutoRefreshBus.notifyUpdated()
    }

    LaunchedEffect(session?.token) {
        if (session?.token != null) {
            refreshNotificationsSilently()
        }
    }

    DisposableEffect(lifecycleOwner, session?.token, session?.roles) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && session?.token != null) {
                scope.launch { refreshNotificationsSilently() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = RootScreen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(RootScreen.Splash.route) {
                LaunchedEffect(Unit) {
                    val initialSession = authRepository.getUserSession().first()
                    navController.navigate(if (initialSession != null) RootScreen.MainShell.route else RootScreen.Login.route) {
                        popUpTo(RootScreen.Splash.route) { inclusive = true }
                    }
                }
                SplashScreen()
            }

            composable(RootScreen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(RootScreen.MainShell.route) {
                            popUpTo(RootScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(RootScreen.MainShell.route) {
                MainScreen(
                    authRepository = authRepository,
                    database = database,
                    networkMonitor = networkMonitor,
                    uiSettingsManager = uiSettingsManager,
                    onOpenSettings = { navController.navigate(RootScreen.Settings.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(RootScreen.Login.route) {
                            popUpTo(RootScreen.MainShell.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(RootScreen.Settings.route) {
                session?.let {
                    SettingsScreen(
                        userId = it.userId,
                        token = it.token,
                        roles = it.roles,
                        uiSettingsManager = uiSettingsManager,
                        notificationRepository = notificationRepository,
                        onBack = { navController.popBackStack() }
                    )
                } ?: SplashScreen()
            }
        }
    }
}
