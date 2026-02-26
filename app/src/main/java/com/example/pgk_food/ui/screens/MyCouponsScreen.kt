package com.example.pgk_food.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.repository.MealsTodayResponse
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.ui.components.HowItWorksCard
import com.example.pgk_food.ui.theme.HeroCardShape
import com.example.pgk_food.ui.theme.PillShape
import com.example.pgk_food.ui.theme.springEntrance
import com.example.pgk_food.ui.viewmodels.DownloadKeysState
import com.example.pgk_food.ui.viewmodels.StudentViewModel

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
    val downloadState by viewModel.downloadKeysState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun loadMeals() {
        isLoading = true
        var pendingError: String? = null
        studentRepository.getMealsToday(token)
            .onSuccess {
                mealsToday = it
                isOfflineMode = it.reason?.let { reason ->
                    reason.contains("offline", ignoreCase = true) || reason.contains("оффлайн", ignoreCase = true)
                } == true
            }
            .onFailure {
                isOfflineMode = true
                mealsToday = MealsTodayResponse(
                    date = "Сегодня",
                    isBreakfastAllowed = true,
                    isLunchAllowed = true,
                    isDinnerAllowed = true,
                    isSnackAllowed = false,
                    isSpecialAllowed = false
                )
                pendingError = "Не удалось загрузить талоны [${it.code}]"
            }
        if (pendingError != null) {
            snackbarHostState.showSnackbar(pendingError!!)
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadMeals() }

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
                CircularProgressIndicator()
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
                        text = "Мои талоны",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.springEntrance()
                    )
                    Text(
                        text = "Выберите талон питания для открытия QR",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showHints) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HowItWorksCard(
                            steps = listOf(
                                "Статусы талона: Доступен, Использован, Недоступен.",
                                "После сканирования статус обновляется через интернет, может быть небольшая задержка.",
                                "В оффлайн-режиме отображаются последние сохраненные данные.",
                                "Если статус не обновился, потяните экран повторно или откройте его снова."
                            ),
                            note = "Оффлайн не гарантирует мгновенную синхронизацию статуса.",
                            onHideHints = onHideHints
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = PillShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Подсказка: ключи обычно обновляются автоматически раз в день. Эта кнопка нужна для ручного обновления.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = onHideHints,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Скрыть подсказки")
                                }
                            }
                        }

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
                                    Icons.Rounded.Warning,
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
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Скачать оффлайн-ключи")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                mealsToday?.let { meals ->
                    val availableMeals = mutableListOf<Pair<String, String>>()
                    if (meals.isBreakfastAllowed) availableMeals.add("Завтрак" to "BREAKFAST")
                    if (meals.isLunchAllowed) availableMeals.add("Обед" to "LUNCH")
                    if (meals.isDinnerAllowed) availableMeals.add("Ужин" to "DINNER")
                    if (meals.isSnackAllowed) availableMeals.add("Полдник" to "SNACK")
                    if (meals.isSpecialAllowed) availableMeals.add("Спецпитание" to "SPECIAL")

                    if (availableMeals.isEmpty()) {
                        item { EmptyCouponsState() }
                    } else {
                        items(availableMeals.size) { index ->
                            val (name, type) = availableMeals[index]
                            CouponItem(name, delay = index * 60) { onCouponClick(type) }
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
fun CouponItem(name: String, delay: Int = 0, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .springEntrance(delay = delay)
            .clickable { onClick() },
        shape = HeroCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
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
                        Icons.Rounded.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Сегодня",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Rounded.QrCode,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
            Icons.Rounded.ConfirmationNumber,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Нет доступных талонов",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
