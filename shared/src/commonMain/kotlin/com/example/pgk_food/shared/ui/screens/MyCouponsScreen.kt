package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.repository.MealsTodayResponse
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.ui.viewmodels.DownloadKeysState
import com.example.pgk_food.shared.ui.viewmodels.StudentViewModel
import kotlinx.coroutines.launch

private enum class CouponStatus {
    AVAILABLE,
    USED,
    UNAVAILABLE,
    UNKNOWN,
}

private data class CouponUi(
    val name: String,
    val type: String,
    val status: CouponStatus,
)

private fun resolveCouponStatus(allowed: Boolean, consumed: Boolean?): CouponStatus {
    return when {
        consumed == true -> CouponStatus.USED
        !allowed -> CouponStatus.UNAVAILABLE
        consumed == false -> CouponStatus.AVAILABLE
        else -> CouponStatus.UNKNOWN
    }
}

@Composable
fun MyCouponsScreen(
    token: String,
    studentRepository: StudentRepository,
    viewModel: StudentViewModel,
    showHints: Boolean = true,
    onHideHints: () -> Unit = {},
    onCouponClick: (String) -> Unit
) {
    var mealsToday by remember { mutableStateOf<MealsTodayResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var loadRequestVersion by remember { mutableStateOf(0) }
    val downloadState by viewModel.downloadKeysState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    suspend fun loadMeals(forceOffline: Boolean = false) {
        val requestVersion = loadRequestVersion + 1
        loadRequestVersion = requestVersion
        isLoading = true

        if (forceOffline) {
            val cachedMeals = studentRepository.getMealsTodayCached()
            if (requestVersion != loadRequestVersion) return
            isOfflineMode = true
            mealsToday = cachedMeals ?: MealsTodayResponse(
                date = "Сегодня",
                isBreakfastAllowed = true,
                isLunchAllowed = true,
            )
            isLoading = false
            if (cachedMeals == null) {
                snackbarHostState.showSnackbar("Кэш талонов не найден, показаны локальные значения")
            }
            return
        }

        var pendingError: String? = null
        studentRepository.getMealsToday(token)
            .onSuccess {
                if (requestVersion != loadRequestVersion) return@onSuccess
                mealsToday = it
                isOfflineMode = it.reason?.let { reason ->
                    reason.contains("offline", ignoreCase = true) ||
                        reason.contains("оффлайн", ignoreCase = true)
                } == true
            }
            .onFailure {
                if (requestVersion != loadRequestVersion) return@onFailure
                isOfflineMode = true
                mealsToday = MealsTodayResponse(
                    date = "Сегодня",
                    isBreakfastAllowed = true,
                    isLunchAllowed = true
                )
                pendingError = it.userMessageOr("Не удалось загрузить талоны")
            }
        if (requestVersion != loadRequestVersion) return
        pendingError?.let { snackbarHostState.showSnackbar(it) }
        if (requestVersion != loadRequestVersion) return
        isLoading = false
    }

    LaunchedEffect(token) {
        loadMeals()
    }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadKeysState.Success -> {
                snackbarHostState.showSnackbar("Оффлайн-ключи загружены")
                viewModel.resetDownloadKeysState()
            }

            is DownloadKeysState.Error -> {
                snackbarHostState.showSnackbar("Ошибка ключей [${state.code}]: ${state.message}")
                viewModel.resetDownloadKeysState()
            }

            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Загружаем талоны...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { scope.launch { loadMeals(forceOffline = true) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Перейти в оффлайн сейчас", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Твои талоны",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.springEntrance(),
                    )
                    Text(
                        text = "Выбери талон, чтобы показать QR-код",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showHints) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HowItWorksCard(
                            steps = listOf(
                                "Статусы талона: доступен, использован, недоступен.",
                                "После сканирования статус может обновиться с небольшой задержкой.",
                                "В оффлайн-режиме отображаются последние сохраненные данные.",
                                "Если статус не обновился, откройте экран повторно."
                            ),
                            note = "Оффлайн-режим не гарантирует мгновенную синхронизацию.",
                            onHideHints = onHideHints,
                        )
                    }

                    if (isOfflineMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = PillShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Включен оффлайн-режим", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val isLoadingKeys = downloadState is DownloadKeysState.Loading
                    OutlinedButton(
                        onClick = { viewModel.downloadKeys() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = PillShape,
                        enabled = !isLoadingKeys
                    ) {
                        if (isLoadingKeys) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Загрузка...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Скачать оффлайн-ключи")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { scope.launch { loadMeals() } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = PillShape,
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обновить талоны")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                mealsToday?.let { meals ->
                    val coupons = buildList {
                        val breakfastStatus = resolveCouponStatus(
                            allowed = meals.isBreakfastAllowed,
                            consumed = meals.isBreakfastConsumed,
                        )
                        if (meals.isBreakfastAllowed || breakfastStatus == CouponStatus.USED) {
                            add(CouponUi(name = "Завтрак", type = "BREAKFAST", status = breakfastStatus))
                        }

                        val lunchStatus = resolveCouponStatus(
                            allowed = meals.isLunchAllowed,
                            consumed = meals.isLunchConsumed,
                        )
                        if (meals.isLunchAllowed || lunchStatus == CouponStatus.USED) {
                            add(CouponUi(name = "Обед", type = "LUNCH", status = lunchStatus))
                        }
                    }

                    if (coupons.isEmpty()) {
                        item {
                            EmptyCouponsState()
                        }
                    } else {
                        items(coupons.size) { index ->
                            val coupon = coupons[index]
                            CouponItem(
                                name = coupon.name,
                                status = coupon.status,
                                delayMs = index * 60,
                                onClick = {
                                    when (coupon.status) {
                                        CouponStatus.AVAILABLE -> onCouponClick(coupon.type)
                                        CouponStatus.UNKNOWN -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Статус не проверен, открываем QR по сохраненным данным"
                                                )
                                            }
                                            onCouponClick(coupon.type)
                                        }

                                        CouponStatus.USED,
                                        CouponStatus.UNAVAILABLE,
                                        -> Unit
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun CouponItem(name: String, status: CouponStatus, delayMs: Int = 0, onClick: () -> Unit) {
    val clickable = status == CouponStatus.AVAILABLE || status == CouponStatus.UNKNOWN
    val containerColor = when (status) {
        CouponStatus.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        CouponStatus.UNKNOWN -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        CouponStatus.USED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        CouponStatus.UNAVAILABLE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    }
    val titleColor = when (status) {
        CouponStatus.AVAILABLE -> MaterialTheme.colorScheme.onPrimaryContainer
        CouponStatus.UNKNOWN -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = when (status) {
        CouponStatus.AVAILABLE -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        CouponStatus.UNKNOWN -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (status) {
        CouponStatus.AVAILABLE -> "Доступен"
        CouponStatus.USED -> "Использован"
        CouponStatus.UNAVAILABLE -> "Недоступен"
        CouponStatus.UNKNOWN -> "Статус не проверен"
    }
    val statusColor = when (status) {
        CouponStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        CouponStatus.USED -> MaterialTheme.colorScheme.tertiary
        CouponStatus.UNAVAILABLE -> MaterialTheme.colorScheme.error
        CouponStatus.UNKNOWN -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .springEntrance(delayMs)
            .alpha(if (clickable) 1f else 0.8f)
            .clickable(enabled = clickable) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Text(
                        text = "На сегодня",
                        style = MaterialTheme.typography.labelMedium,
                        color = subtitleColor
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.18f),
                    shape = PillShape,
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (clickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                        tint = if (clickable) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCouponsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ConfirmationNumber,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "У вас нет доступных талонов",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
