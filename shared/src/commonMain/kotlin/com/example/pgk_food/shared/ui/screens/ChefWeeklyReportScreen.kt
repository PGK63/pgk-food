package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.pgk_food.shared.data.remote.dto.ChefWeeklyReportDto
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.formatRuDateTime
import com.example.pgk_food.shared.ui.util.mondayOfWeek
import com.example.pgk_food.shared.ui.util.nextWeekStart
import com.example.pgk_food.shared.ui.util.nowSamara
import com.example.pgk_food.shared.ui.util.parseIsoDateTimeOrNull
import com.example.pgk_food.shared.ui.util.plusDays
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun ChefWeeklyReportScreen(
    token: String,
    chefRepository: ChefRepository,
) {
    val scope = rememberCoroutineScope()
    val today = remember { nowSamara().date }
    val currentMonday = remember(today) { mondayOfWeek(today) }

    var weekStart by remember { mutableStateOf(nextWeekStart(today)) }
    var report by remember { mutableStateOf<ChefWeeklyReportDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            chefRepository.getWeeklyReport(token, weekStart.toString())
                .onSuccess {
                    report = it
                    statusText = null
                }
                .onFailure {
                    statusText = it.message ?: "Ошибка загрузки недельного отчета"
                }
            isLoading = false
        }
    }

    LaunchedEffect(weekStart) { load() }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { weekStart = plusDays(weekStart, -7) }) { Text("← Пред. неделя") }
                Text(
                    "Неделя ${formatRuDate(weekStart)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { weekStart = plusDays(weekStart, 7) }) {
                    Text("След. неделя →")
                }
            }
        }

        if (isLoading) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        statusText?.let { message ->
            item {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        report?.let { data ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Итоги недели", fontWeight = FontWeight.Bold)
                        Text("Завтрак: ${data.totalBreakfastCount}")
                        Text("Обед: ${data.totalLunchCount}")
                        Text("Завтрак+Обед: ${data.totalBothCount}")
                        Text(
                            if (data.confirmed) {
                                "Подтверждено: ${formatIsoDateTimeForUi(data.confirmedAt) ?: "да"}"
                            } else {
                                "Не подтверждено"
                            }
                        )
                        if (!data.confirmed) {
                            val windowStart = formatIsoDateTimeForUi(data.confirmWindowStart)
                            val windowEnd = formatIsoDateTimeForUi(data.confirmWindowEnd)
                            val windowText = if (windowStart != null && windowEnd != null) {
                                "Окно подтверждения: $windowStart - $windowEnd"
                            } else {
                                null
                            }
                            val hintText = data.confirmWindowHint
                                ?: "Подтверждение доступно с пятницы 12:00 до понедельника 00:00."

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                hintText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!windowText.isNullOrBlank()) {
                                Text(
                                    windowText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = {
                                scope.launch {
                                    chefRepository.confirmWeeklyReport(token, data.weekStart)
                                        .onSuccess { load() }
                                        .onFailure {
                                            statusText = it.message ?: "Ошибка подтверждения отчета"
                                        }
                                }
                            }, enabled = data.canConfirmNow) {
                                Text("Подтвердить просмотр")
                            }
                            if (!data.canConfirmNow) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Подтверждение сейчас недоступно.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            items(data.days, key = { it.date }) { day ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val dateLabel = runCatching { formatRuDate(LocalDate.parse(day.date)) }.getOrElse { day.date }
                        Text(dateLabel, fontWeight = FontWeight.Bold)
                        Text("Завтрак: ${day.breakfastCount}")
                        Text("Обед: ${day.lunchCount}")
                        Text("Завтрак+Обед: ${day.bothCount}")
                    }
                }
            }
        }
    }
}

private fun formatIsoDateTimeForUi(raw: String?): String? {
    return parseIsoDateTimeOrNull(raw)?.let(::formatRuDateTime)
}
