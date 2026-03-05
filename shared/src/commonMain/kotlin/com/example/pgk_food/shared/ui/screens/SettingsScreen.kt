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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.pgk_food.shared.data.repository.NotificationRepository
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.platform.NotificationPermissionStatus
import com.example.pgk_food.shared.platform.rememberNotificationPermissionController
import com.example.pgk_food.shared.ui.components.LocalAppSnackbarDispatcher
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiApiAction
import com.example.pgk_food.shared.ui.theme.springEntrance
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
    var pushEnabled by remember { mutableStateOf(true) }
    var isPushSettingsLoading by remember { mutableStateOf(true) }
    var isPushSettingsUpdating by remember { mutableStateOf(false) }
    var pendingEnablePushAfterPermission by remember { mutableStateOf(false) }

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

    fun updatePushEnabled(enabled: Boolean) {
        if (isPushSettingsUpdating) return
        scope.launch {
            isPushSettingsUpdating = true
            val previous = pushEnabled
            pushEnabled = enabled
            val ok = runUiApiAction(
                actionState = actionState,
                successMessage = null,
                fallbackErrorMessage = "Не удалось обновить уведомления",
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

    LaunchedEffect(token) {
        isPushSettingsLoading = true
        var loadErrorText: String? = null
        notificationRepository.getPushSettings(token)
            .onSuccess { pushEnabled = it.pushEnabled }
            .onFailure { loadErrorText = it.userMessage }
        loadErrorText?.let { snackbarDispatcher.show(it) }
        isPushSettingsLoading = false
    }
    LaunchedEffect(uiScalePercent) {
        pendingUiScaleSlider = uiSettingsManager.percentToSliderPosition(uiScalePercent).toFloat()
    }
    val pendingUiScalePercent = remember(pendingUiScaleSlider) {
        uiSettingsManager.sliderPositionToPercent(pendingUiScaleSlider.toInt())
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
                                        "Короткие подсказки сценариев можно выключить в любой момент.",
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
                            Text("Уведомления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Включить уведомления",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
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
                                    enabled = !isPushSettingsLoading &&
                                        !isPushSettingsUpdating &&
                                        !isActionLoading &&
                                        !pendingEnablePushAfterPermission,
                                )
                            }
                            if (isPushSettingsLoading || isActionLoading || isPushSettingsUpdating) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}
