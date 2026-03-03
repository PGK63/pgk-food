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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.remote.dto.ConsumptionReportRowDto
import com.example.pgk_food.shared.data.remote.dto.FraudReportDto
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.repository.AdminRepository
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.minusDays
import com.example.pgk_food.shared.ui.util.todayLocalDate
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    token: String,
    adminRepository: AdminRepository,
    showFraudTab: Boolean = true,
    loadGroups: suspend () -> Result<List<GroupDto>>,
) {
    val today = remember { todayLocalDate() }

    var startDate by remember { mutableStateOf(minusDays(today, 7)) }
    var endDate by remember { mutableStateOf(today) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var groupSearchQuery by remember { mutableStateOf("") }
    var isGroupsLoading by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<ConsumptionReportRowDto>>(emptyList()) }
    var fraudRows by remember { mutableStateOf<List<FraudReportDto>>(emptyList()) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    val selectedGroupLabel = remember(groups, selectedGroupId) {
        if (selectedGroupId == null) {
            "Все группы"
        } else {
            groups.firstOrNull { it.id == selectedGroupId }?.let { "${it.name} (#${it.id})" }
                ?: "Все группы"
        }
    }

    fun loadGroupOptions() {
        scope.launch {
            isGroupsLoading = true
            loadGroups()
                .onSuccess { loaded ->
                    groups = loaded.sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
                    if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
                        selectedGroupId = null
                    }
                }
                .onFailure {
                    selectedGroupId = null
                    snackbarHostState.showSnackbar(it.userMessageOr("Не удалось загрузить список групп"))
                }
            isGroupsLoading = false
        }
    }

    fun load() {
        scope.launch {
            if (endDate < startDate) {
                snackbarHostState.showSnackbar("Дата окончания раньше даты начала")
                return@launch
            }
            isLoading = true
            if (showFraudTab && selectedTab == 1) {
                adminRepository.getFraudReports(token, startDate.toString(), endDate.toString())
                    .onSuccess { fraudRows = it }
                    .onFailure { snackbarHostState.showSnackbar(it.userMessageOr("Ошибка загрузки подозрений")) }
            } else {
                adminRepository.getConsumptionReport(
                    token = token,
                    startDate = startDate.toString(),
                    endDate = endDate.toString(),
                    groupId = selectedGroupId,
                    assignedByRole = "ALL",
                ).onSuccess { rows = it }
                    .onFailure { snackbarHostState.showSnackbar(it.userMessageOr("Ошибка загрузки отчета")) }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGroupOptions()
    }
    LaunchedEffect(selectedTab) {
        load()
    }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        startDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
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
            initialSelectedDateMillis = endDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        endDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    showEndPicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Отмена") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showGroupPicker) {
        GroupPickerDialog(
            groups = groups,
            searchQuery = groupSearchQuery,
            onSearchQueryChange = { groupSearchQuery = it },
            onDismiss = { showGroupPicker = false },
            onSelectAll = {
                selectedGroupId = null
                showGroupPicker = false
                groupSearchQuery = ""
            },
            onSelectGroup = {
                selectedGroupId = it.id
                showGroupPicker = false
                groupSearchQuery = ""
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Отчеты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }

            if (showFraudTab) {
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Питание") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Подозрения") })
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.height(0.dp).weight(1f))
                            TextButton(onClick = { showStartPicker = true }) { Text("С: ${formatRuDate(startDate)}") }
                            TextButton(onClick = { showEndPicker = true }) { Text("ПО: ${formatRuDate(endDate)}") }
                        }

                        if (!showFraudTab || selectedTab == 0) {
                            OutlinedTextField(
                                value = selectedGroupLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Группа (опционально)") },
                                trailingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isGroupsLoading) { showGroupPicker = true },
                                singleLine = true,
                                enabled = !isGroupsLoading
                            )
                            if (isGroupsLoading) {
                                Text(
                                    "Загрузка списка групп...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(onClick = { load() }, enabled = !isLoading && !isActionLoading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isLoading) "Загрузка..." else "Сформировать")
                        }
                    }
                }
            }

            if (isActionLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (showFraudTab && selectedTab == 1) {
                if (fraudRows.isEmpty()) {
                    item { Text("Подозрительных операций нет", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(fraudRows) { report ->
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
                                    if (ok) load()
                                }
                            }
                        )
                    }
                }
            } else {
                if (rows.isEmpty()) {
                    item { Text("Нет данных за выбранный период", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    adminRepository.exportConsumptionCsv(
                                        token = token,
                                        startDate = startDate.toString(),
                                        endDate = endDate.toString(),
                                        groupId = selectedGroupId,
                                        assignedByRole = "ALL",
                                    ).onSuccess { bytes ->
                                        clipboard.setText(AnnotatedString(bytes.decodeToString()))
                                        snackbarHostState.showSnackbar("CSV скопирован в буфер")
                                    }.onFailure { snackbarHostState.showSnackbar(it.userMessageOr("Ошибка экспорта CSV")) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.height(0.dp).padding(4.dp))
                            Text("Экспорт CSV")
                        }
                    }

                    items(rows, key = { "${it.date}_${it.studentId}" }) { row ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(row.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${row.groupName} • ${row.category}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Дата: ${row.date} • Назначил: ${row.assignedByRole}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MealStatusBadge("Завтрак", row.breakfastUsed)
                                    MealStatusBadge("Обед", row.lunchUsed)
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
private fun GroupPickerDialog(
    groups: List<GroupDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectGroup: (GroupDto) -> Unit,
) {
    val queryTokens = remember(searchQuery) {
        searchQuery.trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }
    val filteredGroups = remember(groups, queryTokens) {
        groups
            .filter { group ->
                if (queryTokens.isEmpty()) return@filter true
                val searchable = "${group.name} ${group.id}".lowercase()
                queryTokens.all { token -> searchable.contains(token) }
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Выбор группы") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск группы") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Найдено: ${filteredGroups.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.heightIn(max = 420.dp)) {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text("Все группы") },
                                supportingContent = { Text("Без фильтра по группе") },
                                modifier = Modifier.clickable { onSelectAll() }
                            )
                            HorizontalDivider()
                        }
                        if (filteredGroups.isEmpty()) {
                            item {
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        } else {
                            items(filteredGroups, key = { it.id }) { group ->
                                ListItem(
                                    headlineContent = { Text(group.name) },
                                    supportingContent = { Text("ID: ${group.id}") },
                                    modifier = Modifier.clickable { onSelectGroup(group) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
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
            containerColor = if (report.resolved) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(report.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Группа: ${report.groupName}", style = MaterialTheme.typography.bodySmall)
            Text("Причина: ${report.reason}", style = MaterialTheme.typography.bodyMedium)
            if (!report.resolved) {
                Button(onClick = onResolve, enabled = !isResolving) {
                    Text(if (isResolving) "..." else "Решить")
                }
            }
        }
    }
}

@Composable
private fun MealStatusBadge(label: String, used: Boolean) {
    val color = if (used) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val textColor = if (used) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Text(
            text = "$label: ${if (used) "да" else "нет"}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
