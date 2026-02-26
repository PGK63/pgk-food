package com.example.pgk_food.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
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
import com.example.pgk_food.data.remote.dto.GroupDto
import com.example.pgk_food.data.remote.dto.StudentMealStatus
import com.example.pgk_food.data.repository.CuratorRepository
import com.example.pgk_food.ui.theme.PillShape
import com.example.pgk_food.ui.theme.TagShape
import com.example.pgk_food.ui.theme.springEntrance
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorStatsScreen(
    token: String,
    curatorId: String,
    curatorRepository: CuratorRepository
) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var stats by remember { mutableStateOf<List<StudentMealStatus>>(emptyList()) }

    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var groupsLoaded by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var isGroupMenuExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadGroups() {
        scope.launch {
            val result = curatorRepository.getCuratorGroups(token, curatorId)
            groups = result.getOrDefault(emptyList())
            if (selectedGroupId == null || groups.none { it.id == selectedGroupId }) {
                selectedGroupId = groups.firstOrNull()?.id
            }
            groupsLoaded = true
        }
    }

    fun loadStats() {
        scope.launch {
            if (groupsLoaded && groups.isNotEmpty() && selectedGroupId == null) {
                stats = emptyList()
                isLoading = false
                return@launch
            }
            isLoading = true
            val result = curatorRepository.getMyGroupStatistics(
                token = token,
                date = selectedDate.toString(),
                groupId = selectedGroupId
            )
            stats = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadGroups() }

    LaunchedEffect(selectedDate, selectedGroupId, groupsLoaded) {
        if (groupsLoaded) {
            loadStats()
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Статистика",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.springEntrance()
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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
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

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = PillShape,
            modifier = Modifier.clickable { showDatePicker = true }
        ) {
            androidx.compose.foundation.layout.Row(
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

        Spacer(modifier = Modifier.height(16.dp))

        if (groupsLoaded && groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Вы не привязаны ни к одной группе", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (stats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Нет данных за эту дату", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats, key = { it.studentId }) { student ->
                    MealStatusCard(student)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealStatusCard(student: StudentMealStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                student.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealStatusBadge("Завтрак", student.hadBreakfast)
                MealStatusBadge("Обед", student.hadLunch)
                MealStatusBadge("Ужин", student.hadDinner)
                MealStatusBadge("Полдник", student.hadSnack)
            }
        }
    }
}

@Composable
private fun MealStatusBadge(label: String, hadMeal: Boolean) {
    val containerColor = if (hadMeal) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = if (hadMeal) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = containerColor,
        shape = TagShape
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}
