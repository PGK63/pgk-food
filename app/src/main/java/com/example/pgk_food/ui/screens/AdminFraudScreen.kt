package com.example.pgk_food.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.remote.dto.FraudReportDto
import com.example.pgk_food.data.repository.AdminRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AdminFraudScreen(token: String, adminRepository: AdminRepository) {
    val today = remember { LocalDate.now() }
    val lastWeek = remember { today.minusDays(7) }
    
    var startDay by remember { mutableStateOf(lastWeek.dayOfMonth.toString()) }
    var startMonth by remember { mutableStateOf(lastWeek.monthValue.toString()) }
    var startYear by remember { mutableStateOf(lastWeek.year.toString()) }
    
    var endDay by remember { mutableStateOf(today.dayOfMonth.toString()) }
    var endMonth by remember { mutableStateOf(today.monthValue.toString()) }
    var endYear by remember { mutableStateOf(today.year.toString()) }
    
    var reports by remember { mutableStateOf<List<FraudReportDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadReports() {
        val sDay = startDay.toIntOrNull() ?: lastWeek.dayOfMonth
        val sMonth = startMonth.toIntOrNull() ?: lastWeek.monthValue
        val sYear = startYear.toIntOrNull() ?: lastWeek.year
        
        val eDay = endDay.toIntOrNull() ?: today.dayOfMonth
        val eMonth = endMonth.toIntOrNull() ?: today.monthValue
        val eYear = endYear.toIntOrNull() ?: today.year

        val startDateStr = try { LocalDate.of(sYear, sMonth, sDay).toString() } catch (e: Exception) { lastWeek.toString() }
        val endDateStr = try { LocalDate.of(eYear, eMonth, eDay).toString() } catch (e: Exception) { today.toString() }

        scope.launch {
            isLoading = true
            val result = adminRepository.getFraudReports(token, startDateStr, endDateStr)
            reports = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadReports()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(text = "Подозрительные операции", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Начальная дата:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startDay,
                    onValueChange = { if (it.length <= 2) startDay = it },
                    label = { Text("День") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = startMonth,
                    onValueChange = { if (it.length <= 2) startMonth = it },
                    label = { Text("Месяц") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = startYear,
                    onValueChange = { if (it.length <= 4) startYear = it },
                    label = { Text("Год") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Конечная дата:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = endDay,
                    onValueChange = { if (it.length <= 2) endDay = it },
                    label = { Text("День") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endMonth,
                    onValueChange = { if (it.length <= 2) endMonth = it },
                    label = { Text("Месяц") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endYear,
                    onValueChange = { if (it.length <= 4) endYear = it },
                    label = { Text("Год") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                Button(
                    onClick = { loadReports() },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("OK")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет подозрительных операций")
                }
            } else {
                LazyColumn {
                    items(reports) { report ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(report.studentName, style = MaterialTheme.typography.titleMedium)
                                        Text("Группа: ${report.groupName}", style = MaterialTheme.typography.bodySmall)
                                        Text("Тип: ${report.mealType}", style = MaterialTheme.typography.bodySmall)
                                        Text("Дата: ${report.date}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Повар: ${report.chefName}", style = MaterialTheme.typography.bodySmall)
                                        Text("Причина: ${report.reason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                    }
                                    
                                    if (!report.resolved) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    adminRepository.resolveFraud(token, report.id).onSuccess {
                                                        snackbarHostState.showSnackbar("Помечено как проверенное")
                                                        loadReports()
                                                    }.onFailure {
                                                        snackbarHostState.showSnackbar("Ошибка при сохранении")
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Проверить")
                                        }
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                "Проверено",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
