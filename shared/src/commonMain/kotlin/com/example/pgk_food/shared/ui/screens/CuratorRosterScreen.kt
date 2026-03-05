package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.pgk_food.shared.model.NoMealReasonType
import com.example.pgk_food.shared.model.StudentCategory
import com.example.pgk_food.shared.runtime.AppModeState
import com.example.pgk_food.shared.ui.components.AppDatePickerDialog
import com.example.pgk_food.shared.ui.components.LocalAppSnackbarDispatcher
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.components.longPressHelp
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.isRosterDateEditable
import com.example.pgk_food.shared.ui.util.isRosterDateReadable
import com.example.pgk_food.shared.ui.util.nextEditableRosterDateFrom
import com.example.pgk_food.shared.ui.util.nextWeekStart
import com.example.pgk_food.shared.ui.util.nowSamara
import com.example.pgk_food.shared.ui.util.plusDays
import com.example.pgk_food.shared.util.HintScreenKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private fun normalizeManyChildrenEntry(entry: StudentRosterDto): StudentRosterDto {
    if (entry.studentCategory != StudentCategory.MANY_CHILDREN) return entry
    val normalizedDays = entry.days.map { day ->
        if (day.isBreakfast && day.isLunch) day.copy(isLunch = false) else day
    }
    return if (normalizedDays == entry.days) entry else entry.copy(days = normalizedDays)
}

private enum class AbsenceDateTarget {
    FROM,
    TO,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorRosterScreen(
    token: String,
    curatorId: String,
    curatorRepository: CuratorRepository,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
    onNavigateToCategories: () -> Unit,
) {
    val isTestMode by AppModeState.isTestMode.collectAsState()
    var businessNow by remember { mutableStateOf(nowSamara()) }
    val initialSelectedDate = remember { nextWeekStart(nowSamara().date) }
    val isDateReadable: (LocalDate) -> Boolean = remember(businessNow, isTestMode) {
        { date -> isRosterDateReadable(date = date, now = businessNow, testMode = isTestMode) }
    }
    val isDateEditable: (LocalDate) -> Boolean = remember(businessNow, isTestMode) {
        { date -> isRosterDateEditable(date = date, now = businessNow, testMode = isTestMode) }
    }

    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    var copyDate by remember {
        mutableStateOf(
            nextEditableRosterDateFrom(
                startDate = plusDays(initialSelectedDate, 1),
                now = businessNow,
                testMode = isTestMode,
            )
        )
    }
    val isSelectedDateEditable = remember(selectedDate, businessNow) { isDateEditable(selectedDate) }
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
    var showExpelConfirm by remember { mutableStateOf(false) }
    var hasAutoSelectedUnfilledDate by remember(selectedGroupId) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarDispatcher = LocalAppSnackbarDispatcher.current
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading
    val hintContent = remember { HintCatalog.content(HintScreenKey.CURATOR_ROSTER) }

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
                snackbarDispatcher.show(
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
            entries = result.getOrDefault(emptyList()).map(::normalizeManyChildrenEntry)
            if (result.isFailure) {
                snackbarDispatcher.show(
                    result.exceptionOrNull()?.userMessageOr("Не удалось загрузить табель") ?: "Не удалось загрузить табель"
                )
            }
            isLoading = false
        }
    }

    fun isStudentDayUnfilled(day: RosterDayDto): Boolean {
        return !day.isBreakfast &&
            !day.isLunch &&
            day.noMealReasonType == null &&
            day.reason.isNullOrBlank() &&
            day.noMealReasonText.isNullOrBlank() &&
            day.comment.isNullOrBlank()
    }

    fun findFirstUnfilledDateInWeek(weekStartDate: LocalDate): LocalDate? {
        for (offset in 0..4) {
            val date = plusDays(weekStartDate, offset)
            val isoDate = date.toString()
            val hasUnfilled = entries.any { entry ->
                val day = entry.days.firstOrNull { it.date == isoDate } ?: return@any true
                isStudentDayUnfilled(day)
            }
            if (hasUnfilled) {
                return date
            }
        }
        return null
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            businessNow = nowSamara()
        }
    }
    LaunchedEffect(Unit) { loadGroups() }
    LaunchedEffect(selectedDate, businessNow, isTestMode) {
        if (!isDateEditable(copyDate)) {
            copyDate = nextEditableRosterDateFrom(
                startDate = plusDays(selectedDate, 1),
                now = businessNow,
                testMode = isTestMode,
            )
        }
    }
    LaunchedEffect(selectedDate, selectedGroupId, groupsLoaded) {
        if (groupsLoaded) {
            loadRoster()
        }
    }
    LaunchedEffect(entries, isLoading, groupsLoaded, selectedDate, businessNow, hasAutoSelectedUnfilledDate) {
        if (isLoading || !groupsLoaded || hasAutoSelectedUnfilledDate) return@LaunchedEffect
        val nextWeekDate = nextWeekStart(businessNow.date)
        val weekEndDate = plusDays(nextWeekDate, 4)
        if (selectedDate < nextWeekDate || selectedDate > weekEndDate) return@LaunchedEffect

        val autoDate = findFirstUnfilledDateInWeek(nextWeekDate) ?: nextWeekDate
        hasAutoSelectedUnfilledDate = true
        if (autoDate != selectedDate) {
            selectedDate = autoDate
        }
    }

    AppDatePickerDialog(
        visible = showDatePicker,
        initialDate = selectedDate,
        onDismiss = { showDatePicker = false },
        onDateSelected = { selectedDate = it },
        isDateSelectable = isDateReadable,
    )

    AppDatePickerDialog(
        visible = showCopyDatePicker,
        initialDate = copyDate,
        onDismiss = { showCopyDatePicker = false },
        onDateSelected = { copyDate = it },
        isDateSelectable = isDateEditable,
    )

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
                            .longPressHelp(
                                actionId = "roster.copy.date.pick",
                                fallbackDescription = "Выбрать дату копирования",
                            )
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
                                var missingCategoryError: Throwable? = null
                                for (entry in entries) {
                                    val currentDayDto = entry.days.firstOrNull { it.date == selectedDate.toString() }
                                    if (currentDayDto != null) {
                                        val request = SaveRosterRequest(
                                            studentId = entry.studentId,
                                            permissions = listOf(currentDayDto.copy(date = copyDate.toString()))
                                        )
                                        val result = curatorRepository.updateRoster(token, request)
                                        if (result.isFailure) {
                                            val ex = result.exceptionOrNull()
                                            val code = (ex as? ApiCallException)?.apiError?.code
                                            if (code == "STUDENT_CATEGORY_REQUIRED") {
                                                missingCategoryError = ex
                                                break
                                            }
                                            hasError = true
                                        }
                                    }
                                }
                                when {
                                    missingCategoryError != null -> {
                                        Result.failure<Unit>(checkNotNull(missingCategoryError))
                                    }
                                    hasError -> Result.failure<Unit>(IllegalStateException("Копирование завершилось с ошибками"))
                                    else -> Result.success(Unit)
                                }
                            }
                            isCopying = false
                            showCopyDialog = false
                            if (success) {
                                snackbarDispatcher.show("Успешно скопировано на ${formatRuDate(copyDate)}")
                            } else {
                                val error = actionState.value as? UiActionState.Error
                                if (error?.code == "STUDENT_CATEGORY_REQUIRED") {
                                    snackbarDispatcher.show(error.userMessage)
                                    onNavigateToCategories()
                                } else {
                                    snackbarDispatcher.show("Копирование завершилось с ошибками")
                                }
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

    fun hasExpelledForSelectedDate(): Boolean {
        val targetDate = selectedDate.toString()
        return entries.any { entry ->
            val day = entry.days.firstOrNull { it.date == targetDate } ?: return@any false
            !day.isBreakfast && !day.isLunch && day.noMealReasonType == NoMealReasonType.EXPELLED
        }
    }

    fun invalidAbsenceRangeStudentForSelectedDate(): String? {
        val targetDate = selectedDate.toString()
        return entries.firstNotNullOfOrNull { entry ->
            val day = entry.days.firstOrNull { it.date == targetDate } ?: return@firstNotNullOfOrNull null
            if (day.noMealReasonType != NoMealReasonType.SICK_LEAVE && day.noMealReasonType != NoMealReasonType.OTHER) {
                return@firstNotNullOfOrNull null
            }
            val from = parseIsoDateOrNull(day.absenceFrom) ?: return@firstNotNullOfOrNull null
            val to = parseIsoDateOrNull(day.absenceTo) ?: return@firstNotNullOfOrNull null
            if (to < from) entry.fullName else null
        }
    }

    fun saveSelectedDate() {
        scope.launch {
            if (!isDateEditable(selectedDate)) {
                snackbarDispatcher.show("После пятницы 12:00 следующая неделя доступна только для просмотра.")
                return@launch
            }
            val invalidRangeStudent = invalidAbsenceRangeStudentForSelectedDate()
            if (invalidRangeStudent != null) {
                snackbarDispatcher.show(
                    "Период отсутствия заполнен неверно: у $invalidRangeStudent дата \"По\" раньше даты \"С\"."
                )
                return@launch
            }

            val success = runUiAction(
                actionState = actionState,
                successMessage = "Сохранено",
                fallbackErrorMessage = "Ошибка сохранения некоторых записей",
                emitSuccessFeedback = false
            ) {
                var hasError = false
                var missingCategoryError: Throwable? = null
                for (entry in entries) {
                    val dayDto = entry.days.firstOrNull { it.date == selectedDate.toString() }
                    if (dayDto != null) {
                        val request = SaveRosterRequest(
                            studentId = entry.studentId,
                            permissions = listOf(dayDto),
                        )
                        val result = curatorRepository.updateRoster(token, request)
                        if (result.isFailure) {
                            val ex = result.exceptionOrNull()
                            val code = (ex as? ApiCallException)?.apiError?.code
                            if (code == "STUDENT_CATEGORY_REQUIRED") {
                                missingCategoryError = ex
                                break
                            }
                            hasError = true
                        }
                    }
                }
                when {
                    missingCategoryError != null -> {
                        Result.failure<Unit>(checkNotNull(missingCategoryError))
                    }
                    hasError -> Result.failure<Unit>(IllegalStateException("Ошибка сохранения некоторых записей"))
                    else -> Result.success(Unit)
                }
            }
            if (success) {
                snackbarDispatcher.show("Сохранено")
            } else {
                val error = actionState.value as? UiActionState.Error
                if (error?.code == "STUDENT_CATEGORY_REQUIRED") {
                    snackbarDispatcher.show(error.userMessage)
                    onNavigateToCategories()
                } else {
                    snackbarDispatcher.show("Ошибка сохранения некоторых записей")
                }
            }
        }
    }

    if (showExpelConfirm) {
        AlertDialog(
            onDismissRequest = { showExpelConfirm = false },
            title = { Text("Подтвердите отчисление") },
            text = {
                Text(
                    "На выбранную дату есть отметки \"Отчислен\". " +
                        "Сохранение заморозит таких студентов и запретит назначение питания."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showExpelConfirm = false
                    saveSelectedDate()
                }) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = { showExpelConfirm = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (showHints) {
                    HowItWorksCard(
                        title = hintContent.title,
                        steps = hintContent.steps,
                        note = hintContent.note,
                        onDismiss = onDismissHints,
                        modifier = Modifier.springEntrance(10),
                    )
                    hintContent.inlineHints.firstOrNull()?.let { inline ->
                        Spacer(modifier = Modifier.height(8.dp))
                        InlineHint(text = inline, modifier = Modifier.springEntrance(20))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Начните искать") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().springEntrance(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (groups.size > 1) {
                    ExposedDropdownMenuBox(
                        modifier = Modifier.springEntrance(60),
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
                    modifier = Modifier.fillMaxWidth().springEntrance(90),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = PillShape,
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .longPressHelp(
                                actionId = "roster.date.pick",
                                fallbackDescription = "Выбрать дату",
                            )
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

                    IconButton(
                        onClick = {
                            copyDate = nextEditableRosterDateFrom(
                                startDate = plusDays(selectedDate, 1),
                                now = businessNow,
                                testMode = isTestMode,
                            )
                            showCopyDialog = true
                        },
                        enabled = !isActionLoading && isSelectedDateEditable,
                        modifier = Modifier.longPressHelp(
                            actionId = "roster.copy-day",
                            fallbackDescription = "Скопировать день",
                        ),
                    ) {
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
                        itemsIndexed(filteredEntries, key = { _, entry -> entry.studentId }) { index, entry ->
                            RosterCard(
                                entry = entry,
                                selectedDateStr = selectedDate.toString(),
                                isDateSelectable = isDateEditable,
                                editable = isSelectedDateEditable,
                                animationDelayMs = (index.coerceAtMost(9) * 35) + 130,
                                onUpdate = { updatedEntry ->
                                    entries = entries.map { if (it.studentId == updatedEntry.studentId) updatedEntry else it }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isSelectedDateEditable) {
                        Text(
                            "Только чтение: после пятницы 12:00 следующая неделя доступна только для просмотра.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (hasExpelledForSelectedDate()) {
                                showExpelConfirm = true
                            } else {
                                saveSelectedDate()
                            }
                        },
                        enabled = !isActionLoading && isSelectedDateEditable,
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
}

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
private fun RosterCard(
    entry: StudentRosterDto,
    selectedDateStr: String,
    isDateSelectable: (LocalDate) -> Boolean,
    editable: Boolean,
    animationDelayMs: Int = 0,
    onUpdate: (StudentRosterDto) -> Unit
) {
    val isManyChildren = entry.studentCategory == StudentCategory.MANY_CHILDREN
    val rawDayEntry = entry.days.firstOrNull { it.date == selectedDateStr } ?: RosterDayDto(
        date = selectedDateStr,
        isBreakfast = false,
        isLunch = false,
        reason = null,
    )
    val dayEntry = if (isManyChildren && rawDayEntry.isBreakfast && rawDayEntry.isLunch) {
        rawDayEntry.copy(isLunch = false)
    } else {
        rawDayEntry
    }
    var datePickerTarget by remember { mutableStateOf<AbsenceDateTarget?>(null) }

    fun updateDayEntry(updated: RosterDayDto) {
        val updatedDays = entry.days.filter { d -> d.date != selectedDateStr } + updated
        onUpdate(entry.copy(days = updatedDays))
    }

    val openDatePickerTarget = datePickerTarget
    val initialDate = when (openDatePickerTarget) {
        AbsenceDateTarget.FROM -> parseIsoDateOrNull(dayEntry.absenceFrom) ?: parseIsoDateOrNull(selectedDateStr)
        AbsenceDateTarget.TO -> parseIsoDateOrNull(dayEntry.absenceTo) ?: parseIsoDateOrNull(selectedDateStr)
        null -> parseIsoDateOrNull(selectedDateStr)
    } ?: runCatching { LocalDate.parse(selectedDateStr) }.getOrElse { plusDays(nowSamara().date, 7) }

    AppDatePickerDialog(
        visible = openDatePickerTarget != null && editable,
        initialDate = initialDate,
        onDismiss = { datePickerTarget = null },
        onDateSelected = { pickedDate ->
            val pickedIso = pickedDate.toString()
            val updated = when (openDatePickerTarget) {
                AbsenceDateTarget.FROM -> dayEntry.copy(absenceFrom = pickedIso)
                AbsenceDateTarget.TO -> dayEntry.copy(absenceTo = pickedIso)
                null -> dayEntry
            }
            if (openDatePickerTarget != null) {
                updateDayEntry(updated)
            }
        },
        isDateSelectable = isDateSelectable,
    )

    Card(
        modifier = Modifier.fillMaxWidth().springEntrance(animationDelayMs),
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
            if (isManyChildren) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Многодетные: только один прием пищи в день",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealToggleChip("Завтрак", dayEntry.isBreakfast, enabled = editable) {
                    val updatedDay = if (isManyChildren && it) {
                        dayEntry.copy(isBreakfast = true, isLunch = false)
                    } else {
                        dayEntry.copy(isBreakfast = it)
                    }
                    updateDayEntry(updatedDay)
                }
                MealToggleChip("Обед", dayEntry.isLunch, enabled = editable) {
                    val updatedDay = if (isManyChildren && it) {
                        dayEntry.copy(isBreakfast = false, isLunch = true)
                    } else {
                        dayEntry.copy(isLunch = it)
                    }
                    updateDayEntry(updatedDay)
                }
            }

            val isNoMeal = !dayEntry.isBreakfast && !dayEntry.isLunch
            if (isNoMeal) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Причина непитания",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        NoMealReasonType.EXPELLED to "Отчислен",
                        NoMealReasonType.SICK_LEAVE to "Больничный",
                        NoMealReasonType.OTHER to "Иное"
                    ).forEach { (reasonType, title) ->
                        FilterChip(
                            selected = dayEntry.noMealReasonType == reasonType,
                            enabled = editable,
                            onClick = {
                                val withDefaults = when (reasonType) {
                                    NoMealReasonType.SICK_LEAVE,
                                    NoMealReasonType.OTHER -> dayEntry.copy(
                                        noMealReasonType = reasonType,
                                        absenceFrom = dayEntry.absenceFrom ?: selectedDateStr,
                                        absenceTo = dayEntry.absenceTo ?: selectedDateStr,
                                    )
                                    NoMealReasonType.EXPELLED -> dayEntry.copy(
                                        noMealReasonType = reasonType,
                                        absenceFrom = null,
                                        absenceTo = null,
                                    )
                                    NoMealReasonType.MISSING_ROSTER -> dayEntry
                                }
                                updateDayEntry(withDefaults)
                            },
                            label = { Text(title) },
                            shape = PillShape
                        )
                    }
                }

                if (dayEntry.noMealReasonType == NoMealReasonType.SICK_LEAVE || dayEntry.noMealReasonType == NoMealReasonType.OTHER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Период отсутствия",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = editable) { datePickerTarget = AbsenceDateTarget.FROM }
                                .longPressHelp(
                                    actionId = "roster.absence.from.pick",
                                    fallbackDescription = "Выбрать дату начала отсутствия",
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(0.dp).padding(4.dp))
                                Text("С: ${formatIsoDateRu(dayEntry.absenceFrom, selectedDateStr)}")
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = editable) { datePickerTarget = AbsenceDateTarget.TO }
                                .longPressHelp(
                                    actionId = "roster.absence.to.pick",
                                    fallbackDescription = "Выбрать дату окончания отсутствия",
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(0.dp).padding(4.dp))
                                Text("По: ${formatIsoDateRu(dayEntry.absenceTo, selectedDateStr)}")
                            }
                        }
                    }
                }

                if (dayEntry.noMealReasonType == NoMealReasonType.OTHER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dayEntry.noMealReasonText.orEmpty(),
                        onValueChange = { value ->
                            val updated = dayEntry.copy(noMealReasonText = value)
                            updateDayEntry(updated)
                        },
                        enabled = editable,
                        label = { Text("Текст причины") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dayEntry.comment.orEmpty(),
                    onValueChange = { value ->
                        val updated = dayEntry.copy(comment = value)
                        updateDayEntry(updated)
                    },
                    enabled = editable,
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                )
            }
        }
    }
}

@Composable
private fun MealToggleChip(label: String, isActive: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = isActive,
        enabled = enabled,
        onClick = { onToggle(!isActive) },
        label = { Text(label) },
        shape = PillShape
    )
}

private fun parseIsoDateOrNull(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun formatIsoDateRu(value: String?, fallbackIso: String): String {
    val fallback = parseIsoDateOrNull(fallbackIso)
    val date = parseIsoDateOrNull(value) ?: fallback
    return date?.let(::formatRuDate) ?: (value ?: fallbackIso)
}
