package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.core.network.toDetailedUserMessage
import com.example.pgk_food.shared.data.remote.dto.ConsumptionReportRowDto
import com.example.pgk_food.shared.data.remote.dto.ConsumptionSummaryResponseDto
import com.example.pgk_food.shared.data.remote.dto.FraudReportDto
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.repository.AdminRepository
import com.example.pgk_food.shared.model.NoMealReasonType
import com.example.pgk_food.shared.model.titleRu
import com.example.pgk_food.shared.platform.FileSaveRequest
import com.example.pgk_food.shared.platform.rememberFileSaveLauncher
import com.example.pgk_food.shared.ui.components.GroupPickerDialog
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.minusDays
import com.example.pgk_food.shared.ui.util.todayLocalDate
import com.example.pgk_food.shared.util.HintScreenKey
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
    showZeroFillBlock: Boolean = true,
    loadGroups: suspend () -> Result<List<GroupDto>>,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
    hintScreen: HintScreenKey = HintScreenKey.ADMIN_REPORTS,
    allGroupsLabel: String = "Все группы",
    groupFieldLabel: String = "Группа (опционально)",
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
    var summary by remember { mutableStateOf<ConsumptionSummaryResponseDto?>(null) }
    var fraudRows by remember { mutableStateOf<List<FraudReportDto>>(emptyList()) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading
    val scope = rememberCoroutineScope()
    val hintContent = remember(hintScreen) { HintCatalog.content(hintScreen) }
    val saveFile = rememberFileSaveLauncher { success, message ->
        scope.launch {
            val snack = when {
                success -> "Файл сохранен"
                message.isNullOrBlank() -> "Не удалось сохранить файл"
                else -> message
            }
            snackbarHostState.showSnackbar(snack)
        }
    }

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    fun Throwable.detailedUserMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.toDetailedUserMessage(default) ?: userMessageOr(default)
    }

    val selectedGroupLabel = remember(groups, selectedGroupId) {
        if (selectedGroupId == null) {
            allGroupsLabel
        } else {
            groups.firstOrNull { it.id == selectedGroupId }?.let { "${it.name} (#${it.id})" }
                ?: allGroupsLabel
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
                    .onFailure { snackbarHostState.showSnackbar(it.detailedUserMessageOr("Ошибка загрузки отчета")) }
                adminRepository.getConsumptionSummary(
                    token = token,
                    startDate = startDate.toString(),
                    endDate = endDate.toString(),
                    groupId = selectedGroupId,
                    assignedByRole = "ALL",
                ).onSuccess { summary = it }
                    .onFailure { snackbarHostState.showSnackbar(it.detailedUserMessageOr("Ошибка загрузки сводки")) }
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
            onDismiss = {
                showGroupPicker = false
                groupSearchQuery = ""
            },
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showHints) {
                item {
                    HowItWorksCard(
                        title = hintContent.title,
                        steps = hintContent.steps,
                        note = hintContent.note,
                        onDismiss = onDismissHints,
                        modifier = Modifier.springEntrance(40),
                    )
                }
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
                                value = groupSearchQuery,
                                onValueChange = {
                                    groupSearchQuery = it
                                    showGroupPicker = true
                                },
                                label = { Text(groupFieldLabel) },
                                placeholder = { Text(selectedGroupLabel) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showGroupPicker = true },
                                        enabled = !isGroupsLoading
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                    summary?.let { summaryData ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Сводка 3 единиц", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Итого: Завтрак ${summaryData.totalBreakfastCount} • Обед ${summaryData.totalLunchCount} • Завтрак+Обед ${summaryData.totalBothCount}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Строк с MISSING_ROSTER: ${summaryData.missingRosterRowsCount}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        if (showZeroFillBlock && summaryData.zeroFillCurators.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("КУРАТОРЫ С НУЛЕВЫМ ЗАПОЛНЕНИЕМ", fontWeight = FontWeight.Bold)
                                        summaryData.zeroFillCurators.forEach { curator ->
                                            Text(
                                                "${curator.curatorName} • неделя ${curator.weekStart} • ${curator.filledCells}/${curator.expectedCells}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showHints) {
                        hintContent.inlineHints.firstOrNull()?.let { inline ->
                            item {
                                InlineHint(
                                    text = inline,
                                    modifier = Modifier.springEntrance(80)
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                            saveFile(
                                                FileSaveRequest(
                                                    fileName = "consumption_${startDate}_${endDate}.csv",
                                                    mimeType = "text/csv",
                                                    bytes = bytes
                                                )
                                            )
                                        }.onFailure {
                                            snackbarHostState.showSnackbar(it.detailedUserMessageOr("Ошибка экспорта CSV"))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null)
                                Spacer(modifier = Modifier.height(0.dp).padding(4.dp))
                                Text("Экспорт CSV")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        adminRepository.exportConsumptionPdf(
                                            token = token,
                                            startDate = startDate.toString(),
                                            endDate = endDate.toString(),
                                            groupId = selectedGroupId,
                                            assignedByRole = "ALL",
                                        ).onSuccess { bytes ->
                                            saveFile(
                                                FileSaveRequest(
                                                    fileName = "consumption_${startDate}_${endDate}.pdf",
                                                    mimeType = "application/pdf",
                                                    bytes = bytes
                                                )
                                            )
                                        }.onFailure {
                                            snackbarHostState.showSnackbar(it.detailedUserMessageOr("Ошибка экспорта PDF"))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null)
                                Spacer(modifier = Modifier.height(0.dp).padding(4.dp))
                                Text("Экспорт PDF")
                            }
                        }
                    }

                    items(rows, key = { "${it.date}_${it.studentId}" }) { row ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(row.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${row.groupName} • ${row.category?.titleRu() ?: "Категория не указана"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Дата: ${row.date} • Назначил: ${assignedByDisplay(row.assignedByRole, row.assignedByName)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "План: Завтрак ${yesNoRu(row.plannedBreakfast)} • Обед ${yesNoRu(row.plannedLunch)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val reasonLabel = row.noMealReasonType?.titleRu() ?: "-"
                                if (reasonLabel != "-" || !row.noMealReasonText.isNullOrBlank() || !row.comment.isNullOrBlank()) {
                                    Text(
                                        "Причина: $reasonLabel${row.noMealReasonText?.let { " • $it" } ?: ""}${row.comment?.let { " • $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (row.noMealReasonType == NoMealReasonType.MISSING_ROSTER) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MealStatusBadge(
                                        label = "Завтрак",
                                        used = row.breakfastUsed,
                                        transactionId = row.breakfastTransactionId,
                                        scannedByName = row.breakfastScannedByName
                                    )
                                    MealStatusBadge(
                                        label = "Обед",
                                        used = row.lunchUsed,
                                        transactionId = row.lunchTransactionId,
                                        scannedByName = row.lunchScannedByName
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

private fun yesNoRu(value: Boolean): String = if (value) "Да" else "Нет"

@Composable
private fun FraudReportItem(
    report: FraudReportDto,
    isResolving: Boolean,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().springEntrance(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.resolved) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(report.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Группа: ${report.groupName}", style = MaterialTheme.typography.bodySmall)
            Text("Причина: ${fraudReasonTitleRu(report.reason)}", style = MaterialTheme.typography.bodyMedium)
            if (!report.resolved) {
                Button(onClick = onResolve, enabled = !isResolving) {
                    Text(if (isResolving) "..." else "Решить")
                }
            }
        }
    }
}

@Composable
private fun MealStatusBadge(
    label: String,
    used: Boolean,
    transactionId: Int?,
    scannedByName: String?
) {
    val color = if (used) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val textColor = if (used) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = "$label: ${if (used) "да" else "нет"}",
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tx: ${transactionId ?: "-"}",
                color = textColor,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Сканировал: ${scannedByName ?: "-"}",
                color = textColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun assignedByRoleTitleRu(role: String?): String = when (role?.uppercase()) {
    "ADMIN" -> "Администратор"
    "CURATOR" -> "Куратор"
    "REGISTRATOR" -> "Регистратор"
    null -> "-"
    else -> role
}

private fun assignedByDisplay(role: String?, assignedByName: String?): String {
    val roleLabel = assignedByRoleTitleRu(role)
    return when {
        role == null && assignedByName.isNullOrBlank() -> "Не назначено"
        assignedByName.isNullOrBlank() -> roleLabel
        role == null -> assignedByName
        else -> "$roleLabel • $assignedByName"
    }
}

private fun fraudReasonTitleRu(reason: String): String = when (reason.uppercase()) {
    "ALREADY_ATE" -> "Повторная попытка питания"
    "INVALID_SIGNATURE" -> "Неверная подпись QR"
    "EXPIRED_QR" -> "Просроченный QR-код"
    "NOT_ALLOWED" -> "Питание не разрешено"
    else -> reason
}
