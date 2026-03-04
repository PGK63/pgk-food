package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.NotificationDto
import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.repository.NotificationRepository
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.platform.NotificationPermissionStatus
import com.example.pgk_food.shared.platform.rememberNotificationPermissionController
import com.example.pgk_food.shared.ui.components.LocalAppSnackbarDispatcher
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiApiAction
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.util.NotificationAutoRefreshBus
import com.example.pgk_food.shared.util.UiSettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    token: String,
    roles: List<UserRole>,
    uiScalePercent: Int,
    onUiScaleCommit: (Int) -> Unit,
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
    var pushEnabled by remember { mutableStateOf(true) }
    var isPushSettingsUpdating by remember { mutableStateOf(false) }
    var pendingEnablePushAfterPermission by remember { mutableStateOf(false) }
    var rosterDeadline by remember { mutableStateOf<RosterDeadlineNotificationDto?>(null) }

    val isCurator = remember(roles) { UserRole.CURATOR in roles }
    val permissionController = rememberNotificationPermissionController()
    val permissionStatus = permissionController.status
    val isSystemPermissionGranted = permissionStatus == NotificationPermissionStatus.GRANTED ||
        permissionStatus == NotificationPermissionStatus.UNSUPPORTED
    val snackbarDispatcher = LocalAppSnackbarDispatcher.current
    val scope = rememberCoroutineScope()
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading
    var pendingUiScaleSlider by remember(uiScalePercent) {
        mutableStateOf(uiSettingsManager.percentToSliderPosition(uiScalePercent).toFloat())
    }

    fun updateHints(enabled: Boolean) {
        uiSettingsManager.setHintsOverride(userId, enabled)
        showHints = uiSettingsManager.shouldShowHints(userId)
    }

    suspend fun loadNotifications() {
        isLoading = true
        var errorText: String? = null

        notificationRepository.getPushSettings(token)
            .onSuccess { pushEnabled = it.pushEnabled }
            .onFailure { errorText = it.userMessage }

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

        errorText?.let { snackbarDispatcher.show(it) }
        isLoading = false
    }

    fun updatePushEnabled(enabled: Boolean) {
        if (isPushSettingsUpdating) return
        scope.launch {
            isPushSettingsUpdating = true
            val previous = pushEnabled
            pushEnabled = enabled
            val ok = runUiApiAction(
                actionState = actionState,
                successMessage = null,
                fallbackErrorMessage = "Не удалось обновить настройки push",
                emitSuccessFeedback = false,
            ) {
                notificationRepository.updatePushSettings(token, enabled)
            }
            if (!ok) {
                pushEnabled = previous
            }
            isPushSettingsUpdating = false
        }
    }

    fun requestSystemPermissionForPushEnable() {
        if (pendingEnablePushAfterPermission) return
        if (!permissionController.canRequestPermission) {
            scope.launch {
                snackbarDispatcher.show("Включите системные уведомления в настройках устройства.")
            }
            return
        }
        pendingEnablePushAfterPermission = true
        permissionController.requestPermission { result ->
            pendingEnablePushAfterPermission = false
            val granted = result == NotificationPermissionStatus.GRANTED ||
                result == NotificationPermissionStatus.UNSUPPORTED
            if (granted) {
                updatePushEnabled(true)
            } else {
                scope.launch {
                    snackbarDispatcher.show("Системные уведомления выключены. Разрешите их в настройках устройства.")
                }
            }
        }
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
            errorText?.let { snackbarDispatcher.show(it) }
            isLoadingMore = false
        }
    }

    fun markAsRead(id: Long) {
        scope.launch {
            val wasUnread = notifications.firstOrNull { it.id == id }?.isRead == false
            val ok = runUiApiAction(
                actionState = actionState,
                successMessage = null,
                fallbackErrorMessage = "Не удалось отметить уведомление",
                emitSuccessFeedback = false
            ) {
                notificationRepository.markAsRead(token, id)
            }
            if (ok) {
                notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }
                if (wasUnread && unreadCount > 0) unreadCount -= 1
            }
        }
    }

    fun markAllRead() {
        val unreadIds = notifications.filterNot { it.isRead }.map { it.id }
        if (unreadIds.isEmpty() || isMarkingAllRead || isActionLoading) return
        scope.launch {
            isMarkingAllRead = true
            val ok = runUiApiAction(
                actionState = actionState,
                successMessage = "Все уведомления прочитаны",
                fallbackErrorMessage = "Не удалось отметить уведомления",
            ) {
                notificationRepository.markAsReadBatch(token, unreadIds)
            }
            if (ok) {
                notifications = notifications.map { it.copy(isRead = true) }
                unreadCount = (unreadCount - unreadIds.size).coerceAtLeast(0)
            }
            isMarkingAllRead = false
        }
    }

    LaunchedEffect(token, isCurator) { loadNotifications() }
    LaunchedEffect(token, isCurator) {
        NotificationAutoRefreshBus.events.collect { loadNotifications() }
    }
    LaunchedEffect(uiScalePercent) {
        pendingUiScaleSlider = uiSettingsManager.percentToSliderPosition(uiScalePercent).toFloat()
    }
    val pendingUiScalePercent = remember(pendingUiScaleSlider) {
        uiSettingsManager.sliderPositionToPercent(pendingUiScaleSlider.toInt())
    }
    val systemPermissionStatusText = remember(permissionStatus) {
        when (permissionStatus) {
            NotificationPermissionStatus.GRANTED -> "включены"
            NotificationPermissionStatus.DENIED -> "выключены"
            NotificationPermissionStatus.NOT_REQUESTED -> "не запрошены"
            NotificationPermissionStatus.UNSUPPORTED -> "не требуется"
        }
    }
    val serverPushStatusText = remember(pushEnabled) {
        if (pushEnabled) "включены" else "выключены"
    }
    val shouldShowPermissionCta = remember(pushEnabled, permissionStatus) {
        pushEnabled && permissionStatus != NotificationPermissionStatus.GRANTED &&
            permissionStatus != NotificationPermissionStatus.UNSUPPORTED
    }
    val baseDensity = LocalDensity.current
    val previewMultiplier = (pendingUiScalePercent / 100f).coerceIn(0.85f, 1.15f)
    val previewDensity = remember(baseDensity, pendingUiScalePercent) {
        Density(
            density = baseDensity.density * previewMultiplier,
            fontScale = baseDensity.fontScale * previewMultiplier,
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().springEntrance(),
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
                                        "Подсказки можно включать и выключать в любой момент.",
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
                                    text = "Данные обновляются автоматически при наличии интернета.",
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
                        modifier = Modifier.fillMaxWidth().springEntrance(35),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Масштаб интерфейса", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Выбрано: $pendingUiScalePercent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = pendingUiScaleSlider,
                                onValueChange = {
                                    pendingUiScaleSlider = it.toInt().coerceIn(
                                        UiSettingsManager.UI_SCALE_SLIDER_MIN,
                                        UiSettingsManager.UI_SCALE_SLIDER_MAX,
                                    ).toFloat()
                                },
                                valueRange = UiSettingsManager.UI_SCALE_SLIDER_MIN.toFloat()..
                                    UiSettingsManager.UI_SCALE_SLIDER_MAX.toFloat(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CompositionLocalProvider(LocalDensity provides previewDensity) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Пример интерфейса", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Так будет выглядеть интерфейс после применения.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {}, enabled = false) { Text("Пример кнопки") }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onUiScaleCommit(pendingUiScalePercent) },
                                    enabled = pendingUiScalePercent != uiScalePercent,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Применить") }
                                OutlinedButton(
                                    onClick = {
                                        pendingUiScaleSlider = uiSettingsManager.percentToSliderPosition(
                                            UiSettingsManager.UI_SCALE_DEFAULT_PERCENT
                                        ).toFloat()
                                        onUiScaleCommit(UiSettingsManager.UI_SCALE_DEFAULT_PERCENT)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Сбросить по умолчанию") }
                            }
                            Text(
                                text = "100% соответствует позиции 102. Изменение применяется только после подтверждения.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().springEntrance(70),
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

                            Text(
                                "Системные уведомления: $systemPermissionStatusText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Push на сервере: $serverPushStatusText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Text(
                                        "Push-уведомления",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        "Лента уведомлений в приложении остается доступной.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = pushEnabled,
                                    onCheckedChange = { enabled ->
                                        if (!enabled) {
                                            updatePushEnabled(false)
                                        } else if (isSystemPermissionGranted) {
                                            updatePushEnabled(true)
                                        } else {
                                            requestSystemPermissionForPushEnable()
                                        }
                                    },
                                    enabled = !isPushSettingsUpdating &&
                                        !isActionLoading &&
                                        !pendingEnablePushAfterPermission,
                                )
                            }

                            if (shouldShowPermissionCta) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Для доставки push включите системное разрешение уведомлений.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { requestSystemPermissionForPushEnable() },
                                    enabled = !isActionLoading &&
                                        !isPushSettingsUpdating &&
                                        !pendingEnablePushAfterPermission,
                                ) {
                                    Text(
                                        if (pendingEnablePushAfterPermission) {
                                            "Запрашиваем..."
                                        } else {
                                            "Разрешить уведомления"
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scope.launch { loadNotifications() } }, enabled = !isLoading && !isActionLoading) {
                                    Text("Обновить")
                                }
                                Button(
                                    onClick = { markAllRead() },
                                    enabled = notifications.any { !it.isRead } && !isMarkingAllRead && !isActionLoading,
                                ) {
                                    Text(if (isMarkingAllRead) "..." else "Прочитать все")
                                }
                            }

                            if (isActionLoading) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            if (isCurator && rosterDeadline != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                CuratorDeadlineCard(
                                    rosterDeadline = rosterDeadline!!,
                                    modifier = Modifier.springEntrance(120)
                                )
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
                    itemsIndexed(notifications, key = { _, item -> item.id }) { index, item ->
                        NotificationRow(
                            modifier = Modifier.springEntrance((index.coerceAtMost(8) * 40) + 140),
                            item = item,
                            isProcessing = isActionLoading,
                            onMarkRead = { markAsRead(item.id) }
                        )
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
private fun NotificationRow(
    modifier: Modifier = Modifier,
    item: NotificationDto,
    isProcessing: Boolean,
    onMarkRead: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                TextButton(onClick = onMarkRead, enabled = !isProcessing) {
                    Text(if (isProcessing) "..." else "Прочитать")
                }
            }
        }
    }
}

@Composable
private fun CuratorDeadlineCard(
    rosterDeadline: RosterDeadlineNotificationDto,
    modifier: Modifier = Modifier,
) {
    val deadlineHuman = rosterDeadline.deadlineHuman ?: rosterDeadline.deadlineDate
    val actionHint = rosterDeadline.actionHint
    Card(
        modifier = modifier.fillMaxWidth(),
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
                deadlineHuman?.let {
                    Text("Дедлайн: $it", style = MaterialTheme.typography.bodySmall)
                }
                rosterDeadline.weekStart?.let {
                    Text("Неделя: $it", style = MaterialTheme.typography.bodySmall)
                }
                if (!actionHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(actionHint, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(rosterDeadline.reason ?: "Напоминание не требуется.", style = MaterialTheme.typography.bodySmall)
                if (!actionHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(actionHint, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
