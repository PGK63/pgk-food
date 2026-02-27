package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.CreateMenuItemRequest
import com.example.pgk_food.shared.data.remote.dto.MenuItemDto
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.model.MenuMealTypeUi
import com.example.pgk_food.shared.platform.rememberCsvImportLauncher
import com.example.pgk_food.shared.ui.util.formatRuDate
import com.example.pgk_food.shared.ui.util.plusDays
import com.example.pgk_food.shared.ui.util.todayLocalDate
import com.example.pgk_food.shared.util.MenuCsvParser
import com.example.pgk_food.shared.util.MenuMealTypeCodec
import com.example.pgk_food.shared.util.sortMenuItemsForUi
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private data class CsvImportReport(
    val importedCount: Int,
    val failedCount: Int,
    val parseErrors: List<String>,
    val sendErrors: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefMenuManageScreenV2(
    token: String,
    chefRepository: ChefRepository,
) {
    val studentRepository = remember { StudentRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedDate by remember { mutableStateOf(todayLocalDate()) }
    var menuItems by remember { mutableStateOf<List<MenuItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var copyDate by remember { mutableStateOf(plusDays(todayLocalDate(), 1)) }
    var showCopyDatePicker by remember { mutableStateOf(false) }
    var importReport by remember { mutableStateOf<CsvImportReport?>(null) }

    fun loadMenu() {
        scope.launch {
            isLoading = true
            menuItems = studentRepository.getMenu(token, selectedDate.toString()).getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(selectedDate) { loadMenu() }

    suspend fun importCsv(fileBytes: ByteArray) {
        val parseResult = MenuCsvParser.parse(fileBytes, selectedDate)
        val requests = parseResult.rows.map { row ->
            CreateMenuItemRequest(
                date = row.date.toString(),
                name = row.name,
                description = MenuMealTypeCodec.encode(row.mealType, row.description),
            )
        }

        val sendErrors = mutableListOf<String>()
        var imported = 0
        if (requests.isNotEmpty()) {
            val batchResult = chefRepository.addMenuItemsBatch(token, requests)
            if (batchResult.isSuccess) {
                imported = requests.size
            } else {
                requests.forEachIndexed { index, request ->
                    chefRepository.addMenuItem(token, request)
                        .onSuccess { imported += 1 }
                        .onFailure { sendErrors += "Строка ${index + 1}: ${it.message ?: "unknown"}" }
                }
            }
        }

        importReport = CsvImportReport(
            importedCount = imported,
            failedCount = (requests.size - imported).coerceAtLeast(0) + parseResult.errors.size,
            parseErrors = parseResult.errors.map { "Строка ${it.lineNumber}: ${it.reason}" },
            sendErrors = sendErrors,
        )
        loadMenu()
    }

    val launchCsvImport = rememberCsvImportLauncher { bytes ->
        scope.launch {
            if (bytes == null) {
                snackbarHostState.showSnackbar("Импорт CSV недоступен на этой платформе")
            } else {
                importCsv(bytes)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        selectedDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } },
        ) { DatePicker(state = pickerState) }
    }

    if (showCopyDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = copyDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showCopyDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        copyDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    showCopyDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showCopyDatePicker = false }) { Text("Отмена") } },
        ) { DatePicker(state = pickerState) }
    }

    val sortedItems = remember(menuItems) { sortMenuItemsForUi(menuItems) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Управление меню", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Дата меню", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(formatRuDate(selectedDate))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { copyDate = plusDays(selectedDate, 1); showCopyDialog = true }) {
                                Text("Копировать меню")
                            }
                            Button(onClick = launchCsvImport) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Импорт CSV")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (sortedItems.isEmpty()) {
                            item { Text("На выбранную дату меню пустое", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(sortedItems, key = { it.id }) { item ->
                                val decoded = MenuMealTypeCodec.decode(item.description)
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.RestaurantMenu, contentDescription = null)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            AssistChip(onClick = {}, enabled = false, label = { Text(decoded.mealType.titleRu) })
                                            if (decoded.description.isNotBlank()) {
                                                Text(decoded.description, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        IconButton(onClick = { scope.launch { chefRepository.deleteMenuItem(token, item.id); loadMenu() } }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить блюдо")
            }
        }
    }

    if (showCreateDialog) {
        CreateMenuItemDialogV2(
            selectedDate = selectedDate,
            onDismiss = { showCreateDialog = false },
            onCreate = { date, name, description, mealType ->
                scope.launch {
                    chefRepository.addMenuItem(
                        token = token,
                        request = CreateMenuItemRequest(
                            date = date.toString(),
                            name = name,
                            description = MenuMealTypeCodec.encode(mealType, description),
                        )
                    )
                    showCreateDialog = false
                    loadMenu()
                }
            },
        )
    }

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("Копировать меню") },
            text = {
                Column {
                    Text("Выберите дату назначения")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { showCopyDatePicker = true }) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatRuDate(copyDate))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (menuItems.isEmpty()) {
                            snackbarHostState.showSnackbar("Меню пустое, копировать нечего")
                            showCopyDialog = false
                            return@launch
                        }
                        menuItems.forEach { item ->
                            chefRepository.addMenuItem(token, CreateMenuItemRequest(copyDate.toString(), item.name, item.description.orEmpty()))
                        }
                        showCopyDialog = false
                        snackbarHostState.showSnackbar("Меню скопировано")
                    }
                }) { Text("Копировать") }
            },
            dismissButton = { TextButton(onClick = { showCopyDialog = false }) { Text("Отмена") } },
        )
    }

    importReport?.let { report ->
        AlertDialog(
            onDismissRequest = { importReport = null },
            title = { Text("Результат импорта CSV") },
            text = {
                Column {
                    Text("Импортировано: ${report.importedCount}")
                    Text("Ошибок: ${report.failedCount}")
                    if (report.parseErrors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ошибки парсинга:", fontWeight = FontWeight.Bold)
                        report.parseErrors.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (report.sendErrors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ошибки отправки:", fontWeight = FontWeight.Bold)
                        report.sendErrors.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = { Button(onClick = { importReport = null }) { Text("ОК") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMenuItemDialogV2(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onCreate: (LocalDate, String, String, MenuMealTypeUi) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(selectedDate) }
    var mealType by remember { mutableStateOf<MenuMealTypeUi?>(null) }
    var datePickerVisible by remember { mutableStateOf(false) }
    var mealTypeExpanded by remember { mutableStateOf(false) }

    if (datePickerVisible) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    datePickerVisible = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { datePickerVisible = false }) { Text("Отмена") } },
        ) { DatePicker(state = pickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое блюдо") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Описание") })
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { datePickerVisible = true }) { Text("Дата: ${formatRuDate(date)}") }
                ExposedDropdownMenuBox(expanded = mealTypeExpanded, onExpandedChange = { mealTypeExpanded = it }) {
                    OutlinedTextField(
                        value = mealType?.titleRu ?: "Тип питания",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                    )
                    DropdownMenu(expanded = mealTypeExpanded, onDismissRequest = { mealTypeExpanded = false }) {
                        MenuMealTypeUi.entries.filterNot { it == MenuMealTypeUi.UNKNOWN }.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.titleRu) },
                                onClick = {
                                    mealType = type
                                    mealTypeExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedMealType = mealType ?: return@Button
                    onCreate(date, name, description, selectedMealType)
                },
                enabled = name.isNotBlank() && mealType != null,
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
