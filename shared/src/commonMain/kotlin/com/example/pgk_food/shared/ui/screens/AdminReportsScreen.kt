package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.remote.dto.DailyReportDto
import com.example.pgk_food.shared.data.remote.dto.FraudReportDto
import com.example.pgk_food.shared.data.repository.AdminRepository
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.minusDays
import com.example.pgk_food.shared.ui.util.plusDays
import com.example.pgk_food.shared.ui.util.todayLocalDate
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(token: String, adminRepository: AdminRepository) {
    val today = remember { todayLocalDate() }

    var startDate by remember { mutableStateOf(minusDays(today, 7)) }
    var endDate by remember { mutableStateOf(today) }
    var isLoading by remember { mutableStateOf(false) }
    var reports by remember { mutableStateOf<List<DailyReportDto>>(emptyList()) }
    var fraudReports by remember { mutableStateOf<List<FraudReportDto>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    fun loadReports(parsedStart: LocalDate, parsedEnd: LocalDate) {
        scope.launch {
            isLoading = true
            if (selectedTab == 0) {
                val result = mutableListOf<DailyReportDto>()
                var current = parsedStart
                var loadError: String? = null
                while (current <= parsedEnd) {
                    adminRepository.getDailyReport(token, current.toString())
                        .onSuccess { result.add(it) }
                        .onFailure { loadError = it.userMessageOr("Ошибка загрузки отчётов") }
                    if (loadError != null) break
                    current = plusDays(current, 1)
                }
                if (loadError != null) {
                    snackbarHostState.showSnackbar(loadError!!)
                } else {
                    reports = result
                }
            } else {
                var loadError: String? = null
                adminRepository.getFraudReports(token, parsedStart.toString(), parsedEnd.toString())
                    .onSuccess { fraudReports = it }
                    .onFailure { loadError = it.userMessageOr("Ошибка загрузки подозрительных отчётов") }
                if (loadError != null) {
                    snackbarHostState.showSnackbar(loadError!!)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTab) {
        loadReports(startDate, endDate)
    }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate
                .atStartOfDayIn(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        startDate = Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    }
                    showStartPicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Отмена") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate
                .atStartOfDayIn(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        endDate = Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    }
                    showEndPicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Отмена") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "УПРАВЛЕНИЕ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {},
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Отчеты") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Подозрения") },
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (selectedTab == 0) "Отчёты по питанию за период" else "Поиск подозрительных транзакций",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("С", modifier = Modifier.width(32.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.clickable { showStartPicker = true },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(formatRuDate(startDate), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ПО", modifier = Modifier.width(32.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.clickable { showEndPicker = true },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(formatRuDate(endDate), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (endDate < startDate) {
                                    scope.launch { snackbarHostState.showSnackbar("Дата окончания раньше даты начала") }
                                } else {
                                    loadReports(startDate, endDate)
                                }
                            },
                            enabled = !isLoading && !isActionLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isLoading) "Загрузка..." else "Сформировать",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isActionLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (selectedTab == 0) {
                if (reports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Нет отчетов")
                        }
                    }
                } else {
                    val totalBreakfast = reports.sumOf { it.breakfastCount }
                    val totalLunch = reports.sumOf { it.lunchCount }
                    val totalDinner = reports.sumOf { it.dinnerCount }
                    val totalSnack = reports.sumOf { it.snackCount }
                    val totalSpecial = reports.sumOf { it.specialCount }
                    val totalAll = reports.sumOf { it.totalCount }
                    val totalOffline = reports.sumOf { it.offlineTransactions }

                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Сводка за период", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    }

                    items(reports) { report ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Отчет за ${report.date}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                ReportStatRow("Всего", report.totalCount)
                                ReportStatRow("Оффлайн", report.offlineTransactions)
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(buildCsvReport(reports)))
                                scope.launch {
                                    snackbarHostState.showSnackbar("CSV скопирован в буфер обмена")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text("Экспорт в CSV", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (fraudReports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Подозрительных транзакций нет")
                        }
                    }
                } else {
                    items(fraudReports) { report ->
                        FraudReportItem(
                            report = report,
                            isResolving = isActionLoading,
                            onResolve = {
                                scope.launch {
                                    val ok = runUiAction(
                                        actionState = actionState,
                                        successMessage = "Помечено как решено",
                                        fallbackErrorMessage = "Ошибка при решении кейса",
                                    ) {
                                        adminRepository.resolveFraud(token, report.id)
                                    }
                                    if (ok) {
                                        loadReports(startDate, endDate)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun buildCsvReport(reports: List<DailyReportDto>): String {
    return buildString {
        append("Дата,Завтраки,Обеды,Ужины,Полдники,Спец. питание,Всего,Оффлайн\n")
        reports.forEach { report ->
            append(
                "${report.date},${report.breakfastCount},${report.lunchCount},${report.dinnerCount}," +
                    "${report.snackCount},${report.specialCount},${report.totalCount},${report.offlineTransactions}\n"
            )
        }
    }
}

@Composable
private fun FraudReportItem(
    report: FraudReportDto,
    isResolving: Boolean,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.resolved) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(report.attemptTimestamp, style = MaterialTheme.typography.bodySmall)
                if (report.resolved) {
                    Text(
                        "РЕШЕНО",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(report.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Группа: ${report.groupName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Причина: ${report.reason}", style = MaterialTheme.typography.bodyMedium)

            if (!report.resolved) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onResolve,
                    enabled = !isResolving,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isResolving) "..." else "Решить")
                }
            }
        }
    }
}

@Composable
private fun ReportStatRow(label: String, value: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}
