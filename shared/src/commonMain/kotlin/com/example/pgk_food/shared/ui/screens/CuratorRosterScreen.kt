package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.remote.dto.RosterDayDto
import com.example.pgk_food.shared.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.shared.data.remote.dto.StudentRosterDto
import com.example.pgk_food.shared.data.repository.CuratorRepository
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.plusDays
import com.example.pgk_food.shared.ui.util.todayLocalDate
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorRosterScreen(
    token: String,
    curatorId: String,
    curatorRepository: CuratorRepository
) {
    val today = remember { todayLocalDate() }

    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }

    var copyDate by remember { mutableStateOf(plusDays(today, 1)) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showCopyDatePicker by remember { mutableStateOf(false) }
    var isCopying by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<StudentRosterDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var groupsLoaded by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var isGroupMenuExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    fun loadGroups() {
        scope.launch {
            val result = curatorRepository.getCuratorGroups(token, curatorId)
            groups = result.getOrDefault(emptyList())
            if (selectedGroupId == null || groups.none { it.id == selectedGroupId }) {
                selectedGroupId = groups.firstOrNull()?.id
            }
            if (result.isFailure) {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.userMessageOr("Не удалось загрузить группы") ?: "Не удалось загрузить группы"
                )
            }
            groupsLoaded = true
        }
    }

    fun loadRoster() {
        scope.launch {
            if (groupsLoaded && groups.isNotEmpty() && selectedGroupId == null) {
                entries = emptyList()
                isLoading = false
                return@launch
            }
            isLoading = true
            val result = curatorRepository.getRoster(
                token = token,
                date = selectedDate.toString(),
                groupId = selectedGroupId,
            )
            entries = result.getOrDefault(emptyList())
            if (result.isFailure) {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.userMessageOr("Не удалось загрузить табель") ?: "Не удалось загрузить табель"
                )
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadGroups() }
    LaunchedEffect(selectedDate, selectedGroupId, groupsLoaded) {
        if (groupsLoaded) {
            loadRoster()
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        selectedDate = Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCopyDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = copyDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showCopyDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        copyDate = Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    }
                    showCopyDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCopyDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(formatRuDate(copyDate), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCopying = true
                            val success = runUiAction(
                                actionState = actionState,
                                successMessage = "Успешно скопировано на ${formatRuDate(copyDate)}",
                                fallbackErrorMessage = "Копирование завершилось с ошибками",
                                emitSuccessFeedback = false
                            ) {
                                var hasError = false
                                for (entry in entries) {
                                    val currentDayDto = entry.days.firstOrNull { it.date == selectedDate.toString() }
                                    if (currentDayDto != null) {
                                        val request = SaveRosterRequest(
                                            studentId = entry.studentId,
                                            permissions = listOf(currentDayDto.copy(date = copyDate.toString()))
                                        )
                                        val result = curatorRepository.updateRoster(token, request)
                                        if (result.isFailure) hasError = true
                                    }
                                }
                                if (hasError) Result.failure<Unit>(IllegalStateException("Копирование завершилось с ошибками"))
                                else Result.success(Unit)
                            }
                            isCopying = false
                            showCopyDialog = false
                            if (success) {
                                snackbarHostState.showSnackbar("Успешно скопировано на ${formatRuDate(copyDate)}")
                            } else {
                                snackbarHostState.showSnackbar("Копирование завершилось с ошибками")
                            }
                        }
                    },
                    enabled = !isCopying && !isActionLoading
                ) {
                    Text(if (isCopying || isActionLoading) "Копирование..." else "Копировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) { Text("Отмена") }
            }
        )
    }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) {
            entries
        } else {
            entries.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
        }
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Начните искать") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (groups.size > 1) {
                ExposedDropdownMenuBox(
                    expanded = isGroupMenuExpanded,
                    onExpandedChange = { isGroupMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = groups.find { it.id == selectedGroupId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Группа") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = isGroupMenuExpanded,
                        onDismissRequest = { isGroupMenuExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    isGroupMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

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
                            formatRuDate(selectedDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = {
                    copyDate = plusDays(selectedDate, 1)
                    showCopyDialog = true
                }) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Скопировать день",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (isActionLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (groupsLoaded && groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Вы не привязаны ни к одной группе", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (isLoading) {
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

                Button(
                    onClick = {
                        scope.launch {
                            val success = runUiAction(
                                actionState = actionState,
                                successMessage = "Сохранено",
                                fallbackErrorMessage = "Ошибка сохранения некоторых записей",
                                emitSuccessFeedback = false
                            ) {
                                var hasError = false
                                for (entry in entries) {
                                    val dayDto = entry.days.firstOrNull { it.date == selectedDate.toString() }
                                    if (dayDto != null) {
                                        val request = SaveRosterRequest(
                                            studentId = entry.studentId,
                                            permissions = listOf(dayDto),
                                        )
                                        val result = curatorRepository.updateRoster(token, request)
                                        if (result.isFailure) hasError = true
                                    }
                                }
                                if (hasError) Result.failure<Unit>(IllegalStateException("Ошибка сохранения некоторых записей"))
                                else Result.success(Unit)
                            }
                            if (success) {
                                snackbarHostState.showSnackbar("Сохранено")
                            } else {
                                snackbarHostState.showSnackbar("Ошибка сохранения некоторых записей")
                            }
                        }
                    },
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (isActionLoading) "Сохранение..." else "Сохранить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RosterCard(
    entry: StudentRosterDto,
    selectedDateStr: String,
    onUpdate: (StudentRosterDto) -> Unit
) {
    val dayEntry = entry.days.firstOrNull { it.date == selectedDateStr } ?: RosterDayDto(
        date = selectedDateStr,
        isBreakfast = false,
        isLunch = false,
        isDinner = false,
        isSnack = false,
        isSpecial = false,
        reason = null,
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

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
