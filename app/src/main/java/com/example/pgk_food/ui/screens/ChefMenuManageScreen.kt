package com.example.pgk_food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.remote.dto.*
import com.example.pgk_food.data.repository.ChefRepository
import com.example.pgk_food.data.repository.StudentRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefMenuManageScreen(token: String, chefRepository: ChefRepository) {
    val studentRepository = remember { StudentRepository() }
    var menuItems by remember { mutableStateOf<List<MenuItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCopying by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    
    var newDate by remember { mutableStateOf(today) }
    var showNewDatePicker by remember { mutableStateOf(false) }

    var copyDate by remember { mutableStateOf(today.plusDays(1)) }
    var showCopyDatePicker by remember { mutableStateOf(false) }

    fun loadMenu() {
        scope.launch {
            isLoading = true
            val dateStr = selectedDate.toString()
            val result = studentRepository.getMenu(token, dateStr)
            menuItems = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(selectedDate) { loadMenu() }

    // Handlers for date pickers
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    if (showNewDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showNewDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showNewDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showNewDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    if (showCopyDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = copyDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showCopyDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        copyDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showCopyDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCopyDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "УПРАВЛЕНИЕ МЕНЮ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Выбрать дату", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(selectedDate.format(formatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                copyDate = selectedDate.plusDays(1)
                                showCopyDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCopying,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Скопировать меню на дату")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (menuItems.isEmpty()) {
                            item { EmptyMenuState() }
                        } else {
                            items(items = menuItems, key = { it.id }) { item ->
                                MenuItemCard(item) {
                                    scope.launch {
                                        chefRepository.deleteMenuItem(token, item.id)
                                        loadMenu()
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }

            ExtendedFloatingActionButton(
                onClick = { 
                    newDate = selectedDate
                    showAddDialog = true 
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ДОБАВИТЬ БЛЮДО")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Новое блюдо") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Дата подачи", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { showNewDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(newDate.format(formatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val dateStr = newDate.toString()
                            chefRepository.addMenuItem(token, CreateMenuItemRequest(dateStr, newName, if (newDescription.isBlank()) "" else newDescription))
                            showAddDialog = false
                            newName = ""; newDescription = ""
                            if (dateStr == selectedDate.toString()) {
                                loadMenu()
                            }
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Скопировать меню") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Выберите целевую дату")
                    Spacer(modifier = Modifier.height(12.dp))
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
                            if (menuItems.isEmpty()) {
                                snackbarHostState.showSnackbar("Меню пустое — нечего копировать")
                                showCopyDialog = false
                                return@launch
                            }
                            val targetDate = copyDate.toString()
                            isCopying = true
                            var successCount = 0
                            var failed = false
                            menuItems.forEach { item ->
                                val result = chefRepository.addMenuItem(
                                    token,
                                    CreateMenuItemRequest(targetDate, item.name, item.description ?: "")
                                )
                                if (result.isSuccess) {
                                    successCount += 1
                                } else {
                                    failed = true
                                }
                            }
                            isCopying = false
                            showCopyDialog = false
                            if (targetDate == selectedDate.toString()) {
                                loadMenu()
                            }
                            if (failed) {
                                snackbarHostState.showSnackbar("Часть блюд не скопирована")
                            } else {
                                snackbarHostState.showSnackbar("Скопировано блюд: $successCount")
                            }
                        }
                    },
                    enabled = !isCopying
                ) {
                    Text("Скопировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun MenuItemCard(item: MenuItemDto, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!item.description.isNullOrBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

