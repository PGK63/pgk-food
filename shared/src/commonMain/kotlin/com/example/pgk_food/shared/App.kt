package com.example.pgk_food.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.pgk_food.shared.core.feedback.FeedbackController
import com.example.pgk_food.shared.core.session.SessionEvent
import com.example.pgk_food.shared.core.session.SessionManager
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.NotificationRepository
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.runtime.DailySyncOrchestrator
import com.example.pgk_food.shared.runtime.MainLoopStateStore
import com.example.pgk_food.shared.runtime.appForegroundEvents
import com.example.pgk_food.shared.ui.components.AppSnackbarHostOverlay
import com.example.pgk_food.shared.ui.screens.LoginScreen
import com.example.pgk_food.shared.ui.screens.MainScreenShared
import com.example.pgk_food.shared.ui.screens.SplashScreen
import com.example.pgk_food.shared.util.NotificationAutoRefreshBus
import com.example.pgk_food.shared.util.UiSettingsManager

private suspend fun refreshNotificationsSilently(
    session: UserSession,
    notificationRepository: NotificationRepository,
) {
    notificationRepository.getUnreadCount(session.token)
    notificationRepository.getNotifications(session.token, cursor = null)
    if (UserRole.CURATOR in session.roles) {
        notificationRepository.getRosterDeadline(session.token)
    }
    NotificationAutoRefreshBus.notifyUpdated()
}

@Composable
fun PgkSharedApp() {
    val authRepository = remember { AuthRepository() }
    val notificationRepository = remember { NotificationRepository() }
    val dailySyncOrchestrator = remember { DailySyncOrchestrator(authRepository = authRepository) }
    val mainLoopStateStore = remember { MainLoopStateStore() }
    val uiSettingsManager = remember { UiSettingsManager() }
    val snackbarHostState = remember { SnackbarHostState() }
    var uiScalePercent by remember { mutableIntStateOf(uiSettingsManager.getUiScalePercent()) }

    val session by SessionStore.session.collectAsState()
    val isRestored by SessionStore.isRestored.collectAsState()
    val baseDensity = LocalDensity.current
    val scaleMultiplier = (uiScalePercent / 100f).coerceIn(0.85f, 1.3f)
    val scaledDensity = remember(baseDensity, uiScalePercent) {
        Density(
            density = baseDensity.density * scaleMultiplier,
            fontScale = baseDensity.fontScale * scaleMultiplier,
        )
    }

    LaunchedEffect(Unit) {
        SessionStore.ensureRestored()
    }

    LaunchedEffect(Unit) {
        FeedbackController.messages.collect { message ->
            snackbarHostState.showSnackbar(message.text, message.actionLabel)
        }
    }

    LaunchedEffect(Unit) {
        SessionManager.events.collect { event ->
            if (event is SessionEvent.LogoutRequired) {
                val activeSession = SessionStore.session.value ?: return@collect
                mainLoopStateStore.clear(activeSession.userId)
                authRepository.logout()
                snackbarHostState.showSnackbar("Сессия истекла. Войдите снова.")
            }
        }
    }

    LaunchedEffect(session?.token) {
        val activeSession = session ?: return@LaunchedEffect
        authRepository.refreshCurrentSession(activeSession.token)
        val currentSession = SessionStore.session.value ?: activeSession
        refreshNotificationsSilently(currentSession, notificationRepository)
        dailySyncOrchestrator.runForSession(currentSession)
    }

    LaunchedEffect(Unit) {
        appForegroundEvents().collect {
            val activeSession = SessionStore.session.value ?: return@collect
            authRepository.refreshCurrentSession(activeSession.token)
            val currentSession = SessionStore.session.value ?: activeSession
            refreshNotificationsSilently(currentSession, notificationRepository)
            dailySyncOrchestrator.runForSession(currentSession)
        }
    }

    MaterialTheme {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            !isRestored -> SplashScreen()
                            session == null -> LoginScreen(authRepository = authRepository)
                            else -> MainScreenShared(
                                session = session!!,
                                uiSettingsManager = uiSettingsManager,
                                uiScalePercent = uiScalePercent,
                                onUiScalePreview = { candidate ->
                                    uiScalePercent = uiSettingsManager.clampUiScalePercent(candidate)
                                },
                                onUiScaleCommit = { candidate ->
                                    val clamped = uiSettingsManager.clampUiScalePercent(candidate)
                                    uiScalePercent = clamped
                                    uiSettingsManager.setUiScalePercent(clamped)
                                },
                                onLogout = {
                                    session?.userId?.let { userId -> mainLoopStateStore.clear(userId) }
                                    authRepository.logout()
                                }
                            )
                        }
                    }
                    AppSnackbarHostOverlay(hostState = snackbarHostState)
                }
            }
        }
    }
}
