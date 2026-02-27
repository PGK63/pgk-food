package com.example.pgk_food.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.example.pgk_food.shared.ui.screens.LoginScreen
import com.example.pgk_food.shared.ui.screens.MainScreenShared
import com.example.pgk_food.shared.ui.screens.SplashScreen
import com.example.pgk_food.shared.util.NotificationAutoRefreshBus

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
    val snackbarHostState = remember { SnackbarHostState() }

    val session by SessionStore.session.collectAsState()
    val isRestored by SessionStore.isRestored.collectAsState()

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
        refreshNotificationsSilently(activeSession, notificationRepository)
        dailySyncOrchestrator.runForSession(activeSession)
    }

    LaunchedEffect(Unit) {
        appForegroundEvents().collect {
            val activeSession = SessionStore.session.value ?: return@collect
            refreshNotificationsSilently(activeSession, notificationRepository)
            dailySyncOrchestrator.runForSession(activeSession)
        }
    }

    MaterialTheme {
        Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    !isRestored -> SplashScreen()
                    session == null -> LoginScreen(authRepository = authRepository)
                    else -> MainScreenShared(
                        session = session!!,
                        onLogout = {
                            session?.userId?.let { userId -> mainLoopStateStore.clear(userId) }
                            authRepository.logout()
                        }
                    )
                }
            }
        }
    }
}
