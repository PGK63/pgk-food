package com.example.pgk_food.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.ui.screens.LoginScreen
import com.example.pgk_food.shared.ui.screens.MainScreenShared

@Composable
fun PgkSharedApp() {
    val authRepository = remember { AuthRepository() }
    LaunchedEffect(Unit) { SessionStore.ensureRestored() }
    val session by SessionStore.session.collectAsState()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (session == null) {
                LoginScreen(authRepository)
            } else {
                MainScreenShared(
                    session = session!!,
                    onLogout = { authRepository.logout() }
                )
            }
        }
    }
}

@Composable
private fun MainSharedScreen(
    session: UserSession,
    onLogout: () -> Unit
) {
    var selectedRole by remember(session.userId) { mutableStateOf(session.roles.firstOrNull()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${session.surname} ${session.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text("ID: ${session.userId}")
                Text("Platform: ${platformName()}")
                session.groupId?.let { Text("Группа: $it") }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (session.roles.isNotEmpty()) {
            Text("Роли", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                session.roles.forEach { role ->
                    FilterChip(
                        selected = role == selectedRole,
                        onClick = { selectedRole = role },
                        label = { Text(role.name) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        FeatureBlock(selectedRole)

        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Выйти")
        }
    }
}

@Composable
private fun FeatureBlock(selectedRole: UserRole?) {
    val items = when (selectedRole) {
        UserRole.STUDENT -> listOf(
            "Купоны/доступные приемы пищи",
            "Меню на день",
            "QR-код студента (iOS экран еще переносится)"
        )
        UserRole.CHEF -> listOf(
            "Валидация QR (логика переносится)",
            "Управление меню",
            "Статистика кухни",
            "Сканер камеры (iOS требует отдельную реализацию)"
        )
        UserRole.REGISTRATOR -> listOf(
            "Пользователи",
            "Группы",
            "Импорт студентов"
        )
        UserRole.CURATOR -> listOf(
            "Ведомость",
            "Статистика группы",
            "Уведомления дедлайна"
        )
        UserRole.ADMIN -> listOf(
            "Отчеты",
            "Фрод-репорты",
            "Экспорт CSV/PDF"
        )
        null -> listOf("Нет доступных ролей")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Модуль ${selectedRole?.name ?: "-"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Text("• $item")
                Spacer(Modifier.height(6.dp))
            }
            Text(
                "Сейчас это KMP-флоу с реальной авторизацией. Следующий этап: перенос экранов и локального хранилища.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
