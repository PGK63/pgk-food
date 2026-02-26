package com.example.pgk_food.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.remote.dto.RosterDayDto
import com.example.pgk_food.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.data.remote.dto.StudentRosterDto
import com.example.pgk_food.data.repository.CuratorRepository
import com.example.pgk_food.ui.theme.PillShape
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorRosterScreen(token: String, curatorRepository: CuratorRepository) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var copyDate by remember { mutableStateOf(today.plusDays(1)) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showCopyDatePicker by remember { mutableStateOf(false) }
    var isCopying by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<StudentRosterDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadRoster() {
        scope.launch {
            isLoading = true
            val result = curatorRepository.getRoster(token, selectedDate.toString())
            entries = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(selectedDate) { loadRoster() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCopyDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = copyDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showCopyDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        copyDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showCopyDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Копировать отметки") },
            text = {
                Column {
                    Text("Выберите дату, на которую скопировать отметки этого дня:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { showCopyDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(copyDate.format(formatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCopying = true
                            var success = true
                            for (entry in entries) {
                                val currentDayDto = entry.days.firstOrNull { it.date == selectedDate.toString() }
                                if (currentDayDto != null) {
                                    val request = SaveRosterRequest(
                                        studentId = entry.studentId,
                                        permissions = listOf(currentDayDto.copy(date = copyDate.toString()))
                                    )
                                    val result = curatorRepository.updateRoster(token, request)
                                    if (result.isFailure) success = false
                                }
                            }
                            isCopying = false
                            showCopyDialog = false
                            if (success) snackbarHostState.showSnackbar("Успешно скопировано на ${copyDate.format(formatter)}")
                            else snackbarHostState.showSnackbar("Копирование завершилось с ошибками")
                        }
                    },
                    enabled = !isCopying
                ) {
                    Text("Копировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) { Text("Отмена") }
            }
        )
    }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) entries
        else entries.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Начните искать") },
                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = PillShape,
                    modifier = Modifier.clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            selectedDate.format(formatter),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                IconButton(onClick = { 
                    copyDate = selectedDate.plusDays(1)
                    showCopyDialog = true 
                }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Скопировать день", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEntries, key = { it.studentId }) { entry ->
                        RosterCard(
                            entry = entry,
                            selectedDateStr = selectedDate.toString(),
                            onUpdate = { updatedEntry ->
                                entries = entries.map { if (it.studentId == updatedEntry.studentId) updatedEntry else it }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save button
                Button(
                    onClick = {
                        scope.launch {
                            var success = true
                            for (entry in entries) {
                                // Find the entry for the selected date
                                val dayDto = entry.days.firstOrNull { it.date == selectedDate.toString() }
                                if (dayDto != null) {
                                    val request = SaveRosterRequest(
                                        studentId = entry.studentId,
                                        permissions = listOf(dayDto)
                                    )
                                    val result = curatorRepository.updateRoster(token, request)
                                    if (result.isFailure) success = false
                                }
                            }
                            if (success) snackbarHostState.showSnackbar("Сохранено")
                            else snackbarHostState.showSnackbar("Ошибка сохранения некоторых записей")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RosterCard(entry: StudentRosterDto, selectedDateStr: String, onUpdate: (StudentRosterDto) -> Unit) {
    val dayEntry = entry.days.firstOrNull { it.date == selectedDateStr } ?: RosterDayDto(
        date = selectedDateStr,
        isBreakfast = false,
        isLunch = false,
        isDinner = false,
        isSnack = false,
        isSpecial = false,
        reason = null
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                entry.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Meal type chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealToggleChip("Завтрак", dayEntry.isBreakfast) {
                    val updatedDay = dayEntry.copy(isBreakfast = it)
                    val updatedDays = entry.days.filter { d -> d.date != selectedDateStr } + updatedDay
                    onUpdate(entry.copy(days = updatedDays))
                }
                MealToggleChip("Обед", dayEntry.isLunch) {
                    val updatedDay = dayEntry.copy(isLunch = it)
                    val updatedDays = entry.days.filter { d -> d.date != selectedDateStr } + updatedDay
                    onUpdate(entry.copy(days = updatedDays))
                }
                MealToggleChip("Ужин", dayEntry.isDinner) {
                    val updatedDay = dayEntry.copy(isDinner = it)
                    val updatedDays = entry.days.filter { d -> d.date != selectedDateStr } + updatedDay
                    onUpdate(entry.copy(days = updatedDays))
                }
                MealToggleChip("Полдник", dayEntry.isSnack) {
                    val updatedDay = dayEntry.copy(isSnack = it)
                    val updatedDays = entry.days.filter { d -> d.date != selectedDateStr } + updatedDay
                    onUpdate(entry.copy(days = updatedDays))
                }
                MealToggleChip("Спец. питание", dayEntry.isSpecial) {
                    val updatedDay = dayEntry.copy(isSpecial = it)
                    val updatedDays = entry.days.filter { d -> d.date != selectedDateStr } + updatedDay
                    onUpdate(entry.copy(days = updatedDays))
                }
            }
        }
    }
}

@Composable
private fun MealToggleChip(label: String, isActive: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = isActive,
        onClick = { onToggle(!isActive) },
        label = { Text(label) },
        shape = PillShape
    )
}
