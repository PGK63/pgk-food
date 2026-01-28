package com.example.pgk_food.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.remote.dto.RosterDayDto
import com.example.pgk_food.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.data.remote.dto.StudentRosterDto
import com.example.pgk_food.data.repository.CuratorRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CuratorRosterScreen(token: String, curatorRepository: CuratorRepository) {
    val today = remember { LocalDate.now() }
    var selectedDay by remember { mutableStateOf(today.dayOfMonth.toString()) }
    var selectedMonth by remember { mutableStateOf(today.monthValue.toString()) }
    var selectedYear by remember { mutableStateOf(today.year.toString()) }
    
    var roster by remember { mutableStateOf<List<StudentRosterDto>>(emptyList()) }
    var editedRoster by remember { mutableStateOf<Map<String, StudentRosterDto>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadRoster() {
        val day = selectedDay.toIntOrNull() ?: today.dayOfMonth
        val month = selectedMonth.toIntOrNull() ?: today.monthValue
        val year = selectedYear.toIntOrNull() ?: today.year
        
        val dateString = try {
            LocalDate.of(year, month, day).toString()
        } catch (e: Exception) {
            today.toString()
        }

        isLoading = true
        scope.launch {
            val result = curatorRepository.getRoster(token, dateString)
            val data = result.getOrDefault(emptyList())
            roster = data
            editedRoster = data.associateBy { it.studentId }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRoster()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(text = "Табель питания", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = selectedDay,
                    onValueChange = { if (it.length <= 2) selectedDay = it },
                    label = { Text("День") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = { if (it.length <= 2) selectedMonth = it },
                    label = { Text("Месяц") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = { if (it.length <= 4) selectedYear = it },
                    label = { Text("Год") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                Button(
                    onClick = { loadRoster() },
                    modifier = Modifier.align(Alignment.CenterVertically),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("OK")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (roster.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Данные отсутствуют")
                }
            } else {
                LazyColumn {
                    items(roster) { originalStudent ->
                        val student = editedRoster[originalStudent.studentId] ?: originalStudent
                        val day = student.days.firstOrNull() ?: RosterDayDto(
                            date = LocalDate.now().toString(),
                            isBreakfast = false,
                            isLunch = false,
                            isDinner = false,
                            isSnack = false,
                            isSpecial = false,
                            reason = ""
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = student.fullName, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MealStatusBadge("Завтрак", day.isBreakfast) {
                                        val newDay = day.copy(isBreakfast = !day.isBreakfast)
                                        editedRoster = editedRoster + (student.studentId to student.copy(days = listOf(newDay)))
                                    }
                                    MealStatusBadge("Обед", day.isLunch) {
                                        val newDay = day.copy(isLunch = !day.isLunch)
                                        editedRoster = editedRoster + (student.studentId to student.copy(days = listOf(newDay)))
                                    }
                                    MealStatusBadge("Ужин", day.isDinner) {
                                        val newDay = day.copy(isDinner = !day.isDinner)
                                        editedRoster = editedRoster + (student.studentId to student.copy(days = listOf(newDay)))
                                    }
                                    MealStatusBadge("Полдник", day.isSnack) {
                                        val newDay = day.copy(isSnack = !day.isSnack)
                                        editedRoster = editedRoster + (student.studentId to student.copy(days = listOf(newDay)))
                                    }
                                    MealStatusBadge("Спец", day.isSpecial) {
                                        val newDay = day.copy(isSpecial = !day.isSpecial)
                                        editedRoster = editedRoster + (student.studentId to student.copy(days = listOf(newDay)))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = day.reason ?: "",
                                    onValueChange = { 
                                        val newDay = day.copy(reason = it)
                                        editedRoster = editedRoster + (student.studentId to student.copy(days = listOf(newDay)))
                                    },
                                    label = { Text("Причина (если не питается)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val request = SaveRosterRequest(
                                                studentId = student.studentId,
                                                permissions = student.days
                                            )
                                            curatorRepository.updateRoster(token, request).onSuccess {
                                                snackbarHostState.showSnackbar("Успешно сохранено")
                                            }.onFailure {
                                                snackbarHostState.showSnackbar("Ошибка при сохранении")
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Сохранить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealStatusBadge(label: String, isAllowed: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isAllowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(width = 60.dp, height = 24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isAllowed) "Да" else "Нет",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAllowed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
