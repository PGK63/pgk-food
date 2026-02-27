package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.NotificationDto
import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.repository.NotificationRepository
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.util.NotificationAutoRefreshBus
import com.example.pgk_food.shared.util.UiSettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    token: String,
    roles: List<UserRole>,
    uiSettingsManager: UiSettingsManager,
    notificationRepository: NotificationRepository,
    onBack: () -> Unit,
) {
    var showHints by remember(userId) { mutableStateOf(uiSettingsManager.shouldShowHints(userId)) }
    var notifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0L) }
    var nextCursor by remember { mutableStateOf<Long?>(null) }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isMarkingAllRead by remember { mutableStateOf(false) }
    var rosterDeadline by remember { mutableStateOf<RosterDeadlineNotificationDto?>(null) }

    val isCurator = remember(roles) { UserRole.CURATOR in roles }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun updateHints(enabled: Boolean) {
        uiSettingsManager.setHintsOverride(userId, enabled)
        showHints = uiSettingsManager.shouldShowHints(userId)
    }

    suspend fun loadNotifications() {
        isLoading = true
        var errorText: String? = null

        notificationRepository.getUnreadCount(token)
            .onSuccess { unreadCount = it.count }
            .onFailure { errorText = it.userMessage }

        notificationRepository.getNotifications(token, cursor = null)
            .onSuccess {
                notifications = it.items
                nextCursor = it.nextCursor
                hasMore = it.hasMore
            }
            .onFailure { errorText = it.userMessage }

        if (isCurator) {
            notificationRepository.getRosterDeadline(token)
                .onSuccess { rosterDeadline = it }
                .onFailure { if (errorText == null) errorText = it.userMessage }
        }

        errorText?.let { snackbarHostState.showSnackbar(it) }
        isLoading = false
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (!hasMore || isLoadingMore) return
        scope.launch {
            isLoadingMore = true
            var errorText: String? = null
            notificationRepository.getNotifications(token, cursor = cursor)
                .onSuccess {
                    notifications = notifications + it.items
                    nextCursor = it.nextCursor
                    hasMore = it.hasMore
                }
                .onFailure { errorText = it.userMessage }
            errorText?.let { snackbarHostState.showSnackbar(it) }
            isLoadingMore = false
        }
    }

    fun markAsRead(id: Long) {
        scope.launch {
            val wasUnread = notifications.firstOrNull { it.id == id }?.isRead == false
            var errorText: String? = null
            notificationRepository.markAsRead(token, id)
                .onSuccess {
                    notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }
                    if (wasUnread && unreadCount > 0) unreadCount -= 1
                }
                .onFailure { errorText = it.userMessage }
            errorText?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    fun markAllRead() {
        val unreadIds = notifications.filterNot { it.isRead }.map { it.id }
        if (unreadIds.isEmpty() || isMarkingAllRead) return
        scope.launch {
            isMarkingAllRead = true
            var errorText: String? = null
            notificationRepository.markAsReadBatch(token, unreadIds)
                .onSuccess {
                    notifications = notifications.map { it.copy(isRead = true) }
                    unreadCount = (unreadCount - unreadIds.size).coerceAtLeast(0)
                }
                .onFailure { errorText = it.userMessage }
            errorText?.let { snackbarHostState.showSnackbar(it) }
            isMarkingAllRead = false
        }
    }

    LaunchedEffect(token, isCurator) { loadNotifications() }
    LaunchedEffect(token, isCurator) {
        NotificationAutoRefreshBus.events.collect { loadNotifications() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Подсказки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Text("Показывать подсказки", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        "По умолчанию подсказки активны первые 3 дня, затем скрываются автоматически.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(checked = showHints, onCheckedChange = { updateHints(it) })
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Ключи студента и оффлайн-данные повара обновляются автоматически 1 раз в день.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Уведомления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Непрочитано: $unreadCount", style = MaterialTheme.typography.labelLarge)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scope.launch { loadNotifications() } }, enabled = !isLoading) { Text("Обновить") }
                                Button(
                                    onClick = { markAllRead() },
                                    enabled = notifications.any { !it.isRead } && !isMarkingAllRead,
                                ) {
                                    Text(if (isMarkingAllRead) "..." else "Прочитать все")
                                }
                            }

                            if (isCurator && rosterDeadline != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                CuratorDeadlineCard(rosterDeadline!!)
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) { CircularProgressIndicator() }
                    }
                } else if (notifications.isEmpty()) {
                    item {
                        Text(
                            "Пока нет уведомлений",
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { item ->
                        NotificationRow(item = item, onMarkRead = { markAsRead(item.id) })
                    }
                }

                if (!isLoading && hasMore) {
                    item {
                        TextButton(onClick = { loadMore() }, enabled = !isLoadingMore, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isLoadingMore) "Загрузка..." else "Загрузить еще")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationDto, onMarkRead: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.message, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(item.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!item.isRead) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onMarkRead) { Text("Прочитать") }
            }
        }
    }
}

@Composable
private fun CuratorDeadlineCard(rosterDeadline: RosterDeadlineNotificationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rosterDeadline.needsReminder) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Статус табеля куратора", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            if (rosterDeadline.needsReminder) {
                Text(
                    "Нужно заполнить табель на следующую неделю. До дедлайна: ${rosterDeadline.daysUntilDeadline ?: "?"} дн.",
                    style = MaterialTheme.typography.bodySmall,
                )
                rosterDeadline.deadlineDate?.let { Text("Дедлайн: $it", style = MaterialTheme.typography.bodySmall) }
            } else {
                Text(rosterDeadline.reason ?: "Напоминание не требуется.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
