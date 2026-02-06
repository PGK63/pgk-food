package com.example.pgk_food.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.remote.dto.DailyReportDto
import com.example.pgk_food.data.repository.AdminRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(token: String, adminRepository: AdminRepository) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    var startDate by remember { mutableStateOf(today.minusDays(7)) }
    var endDate by remember { mutableStateOf(today) }
    var startDateText by remember { mutableStateOf(startDate.format(formatter)) }
    var endDateText by remember { mutableStateOf(endDate.format(formatter)) }
    var isLoading by remember { mutableStateOf(false) }
    var reports by remember { mutableStateOf<List<DailyReportDto>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun loadReports(parsedStart: LocalDate, parsedEnd: LocalDate) {
        scope.launch {
            isLoading = true
            val result = mutableListOf<DailyReportDto>()
            var current = parsedStart
            var failed = false
            while (!current.isAfter(parsedEnd)) {
                val apiResult = adminRepository.getDailyReport(token, current.toString())
                apiResult.onSuccess { result.add(it) }.onFailure {
                    failed = true
                }
                if (failed) break
                current = current.plusDays(1)
            }
            if (failed) {
                snackbarHostState.showSnackbar("Ошибка загрузки отчетов")
            } else {
                reports = result
            }
            isLoading = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        loadReports(startDate, endDate)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "Отчеты", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Период", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDateText,
                            onValueChange = { if (it.length <= 10) startDateText = it },
                            label = { Text("С даты") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDateText,
                            onValueChange = { if (it.length <= 10) endDateText = it },
                            label = { Text("По дату") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val parsedStart = runCatching { LocalDate.parse(startDateText, formatter) }.getOrNull()
                            val parsedEnd = runCatching { LocalDate.parse(endDateText, formatter) }.getOrNull()
                            when {
                                parsedStart == null || parsedEnd == null -> scope.launch {
                                    snackbarHostState.showSnackbar("Неверный формат даты. Используйте ДД.ММ.ГГГГ")
                                }
                                parsedEnd.isBefore(parsedStart) -> scope.launch {
                                    snackbarHostState.showSnackbar("Дата окончания раньше даты начала")
                                }
                                else -> {
                                    startDate = parsedStart
                                    endDate = parsedEnd
                                    loadReports(parsedStart, parsedEnd)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Обновить")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Загрузка...")
                }
            } else if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Нет отчетов")
                }
            } else {
                val totalBreakfast = reports.sumOf { it.breakfastCount }
                val totalLunch = reports.sumOf { it.lunchCount }
                val totalDinner = reports.sumOf { it.dinnerCount }
                val totalSnack = reports.sumOf { it.snackCount }
                val totalSpecial = reports.sumOf { it.specialCount }
                val totalAll = reports.sumOf { it.totalCount }
                val totalOffline = reports.sumOf { it.offlineTransactions }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Сводка за период", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${startDate.format(formatter)} - ${endDate.format(formatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ReportStatRow("Завтраки", totalBreakfast)
                        ReportStatRow("Обеды", totalLunch)
                        ReportStatRow("Ужины", totalDinner)
                        ReportStatRow("Полдники", totalSnack)
                        ReportStatRow("Спец. питание", totalSpecial)
                        ReportStatRow("Всего", totalAll)
                        ReportStatRow("Оффлайн", totalOffline)
                    }
                }

                reports.forEach { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Отчет за ${report.date}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ReportStatRow("Завтраки", report.breakfastCount)
                            ReportStatRow("Обеды", report.lunchCount)
                            ReportStatRow("Ужины", report.dinnerCount)
                            ReportStatRow("Полдники", report.snackCount)
                            ReportStatRow("Спец. питание", report.specialCount)
                            ReportStatRow("Всего", report.totalCount)
                            ReportStatRow("Оффлайн", report.offlineTransactions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatRow(label: String, value: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}
