package com.example.pgk_food.ui.screens

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
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.example.pgk_food.data.remote.dto.DailyReportDto
import com.example.pgk_food.data.repository.AdminRepository
import com.example.pgk_food.ui.theme.PillShape
import com.example.pgk_food.ui.theme.springEntrance
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import com.example.pgk_food.data.remote.dto.FraudReportDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(token: String, adminRepository: AdminRepository) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    var startDate by remember { mutableStateOf(today.minusDays(7)) }
    var endDate by remember { mutableStateOf(today) }
    var isLoading by remember { mutableStateOf(false) }
    var reports by remember { mutableStateOf<List<DailyReportDto>>(emptyList()) }
    var fraudReports by remember { mutableStateOf<List<FraudReportDto>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadReports(parsedStart: LocalDate, parsedEnd: LocalDate) {
        scope.launch {
            isLoading = true
            if (selectedTab == 0) {
                val result = mutableListOf<DailyReportDto>()
                var current = parsedStart
                var failed = false
                while (!current.isAfter(parsedEnd)) {
                    val apiResult = adminRepository.getDailyReport(token, current.toString())
                    apiResult.onSuccess { result.add(it) }.onFailure { failed = true }
                    if (failed) break
                    current = current.plusDays(1)
                }
                if (failed) snackbarHostState.showSnackbar("Ошибка загрузки отчетов") else reports = result
            } else {
                var fraudFailed = false
                adminRepository.getFraudReports(token, parsedStart.toString(), parsedEnd.toString())
                    .onSuccess { fraudReports = it }
                    .onFailure { fraudFailed = true }
                if (fraudFailed) snackbarHostState.showSnackbar("Ошибка загрузки подозрительных отчётов")
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTab) {
        loadReports(startDate, endDate)
    }

    // Date picker dialogs
    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showStartPicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showEndPicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = "УПРАВЛЕНИЕ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.springEntrance())
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Отчеты") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Подозрения") }
                    )
                }
            }

            // Date range card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            if (selectedTab == 0) "Отчёты по питанию за период" else "Поиск подозрительных транзакций",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // ... (Rest of Date range card is SAME)

                        // Start date chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("С", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(32.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = PillShape,
                                modifier = Modifier.clickable { showStartPicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        startDate.format(formatter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // End date chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ПО", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(32.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = PillShape,
                                modifier = Modifier.clickable { showEndPicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        endDate.format(formatter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { loadReports(startDate, endDate) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = PillShape
                        ) {
                            Text("Сформировать", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (selectedTab == 0) {
                // DAILY REPORTS
                if (reports.isEmpty()) {
                    item { Text("Нет отчетов", modifier = Modifier.padding(32.dp)) }
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
                                val csvText = buildString {
                                    append("Дата,Завтраки,Обеды,Ужины,Полдники,Спец. питание,Всего,Оффлайн\n")
                                    reports.forEach { r ->
                                        append("${r.date},${r.breakfastCount},${r.lunchCount},${r.dinnerCount},${r.snackCount},${r.specialCount},${r.totalCount},${r.offlineTransactions}\n")
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, csvText)
                                    type = "text/csv"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Экспорт отчета"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = PillShape
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Экспорт в CSV", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // FRAUD REPORTS
                if (fraudReports.isEmpty()) {
                    item { Text("Подозрительных транзакций нет", modifier = Modifier.padding(32.dp)) }
                } else {
                    items(fraudReports) { report ->
                        FraudReportItem(
                            report = report,
                            onResolve = {
                                scope.launch {
                                    var resolved = false
                                    adminRepository.resolveFraud(token, report.id)
                                        .onSuccess { resolved = true }
                                    if (resolved) {
                                        snackbarHostState.showSnackbar("Помечено как решено")
                                        loadReports(startDate, endDate)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FraudReportItem(report: FraudReportDto, onResolve: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().springEntrance(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.resolved) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(report.attemptTimestamp, style = MaterialTheme.typography.bodySmall)
                if (report.resolved) {
                    Text("РЕШЕНО", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(report.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Группа: ${report.groupName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Причина: ${report.reason}", style = MaterialTheme.typography.bodyMedium)
            
            if (!report.resolved) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onResolve, modifier = Modifier.align(Alignment.End), shape = PillShape) {
                    Text("Решить")
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
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}
