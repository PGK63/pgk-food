package com.example.pgk_food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.remote.dto.*
import com.example.pgk_food.data.repository.ChefRepository
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.ui.components.ThreeInputDatePicker
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefMenuManageScreen(token: String, chefRepository: ChefRepository) {
    val studentRepository = remember { StudentRepository() }
    var menuItems by remember { mutableStateOf<List<MenuItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCopying by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val calendar = remember { Calendar.getInstance() }
    
    var viewDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')) }
    var viewMonth by remember { mutableStateOf((calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')) }
    var viewYear by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    
    var newDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')) }
    var newMonth by remember { mutableStateOf((calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')) }
    var newYear by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }

    var copyDay by remember { mutableStateOf(viewDay) }
    var copyMonth by remember { mutableStateOf(viewMonth) }
    var copyYear by remember { mutableStateOf(viewYear) }

    fun loadMenu() {
        scope.launch {
            isLoading = true
            val dateStr = "$viewYear-${viewMonth.padStart(2, '0')}-${viewDay.padStart(2, '0')}"
            val result = studentRepository.getMenu(token, dateStr)
            menuItems = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMenu() }

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
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Выбрать дату", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        ThreeInputDatePicker(
                            day = viewDay,
                            month = viewMonth,
                            year = viewYear,
                            onDayChange = { viewDay = it; loadMenu() },
                            onMonthChange = { viewMonth = it; loadMenu() },
                            onYearChange = { viewYear = it; loadMenu() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                copyDay = viewDay
                                copyMonth = viewMonth
                                copyYear = viewYear
                                showCopyDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCopying
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
                            item {
                                EmptyMenuState()
                            }
                        } else {
                            items(items = menuItems) { item ->
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
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ДОБАВИТЬ БЛЮДО")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Новое блюдо") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Дата подачи", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThreeInputDatePicker(
                        day = newDay, month = newMonth, year = newYear,
                        onDayChange = { newDay = it }, onMonthChange = { newMonth = it }, onYearChange = { newYear = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val dateStr = "$newYear-${newMonth.padStart(2, '0')}-${newDay.padStart(2, '0')}"
                            chefRepository.addMenuItem(token, CreateMenuItemRequest(dateStr, newName, newDescription))
                            showAddDialog = false
                            newName = ""; newDescription = ""
                            loadMenu()
                        }
                    },
                    enabled = newName.isNotBlank() && newDay.length == 2 && newMonth.length == 2 && newYear.length == 4
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
            shape = RoundedCornerShape(28.dp),
            title = { Text("Скопировать меню") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Куда скопировать блюда?")
                    Spacer(modifier = Modifier.height(12.dp))
                    ThreeInputDatePicker(
                        day = copyDay, month = copyMonth, year = copyYear,
                        onDayChange = { copyDay = it },
                        onMonthChange = { copyMonth = it },
                        onYearChange = { copyYear = it },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                            val targetDate = "$copyYear-${copyMonth.padStart(2, '0')}-${copyDay.padStart(2, '0')}"
                            isCopying = true
                            var successCount = 0
                            var failed = false
                            menuItems.forEach { item ->
                                val result = chefRepository.addMenuItem(
                                    token,
                                    CreateMenuItemRequest(targetDate, item.name, item.description ?: "-")
                                )
                                if (result.isSuccess) {
                                    successCount += 1
                                } else {
                                    failed = true
                                }
                            }
                            isCopying = false
                            showCopyDialog = false
                            if (targetDate == "$viewYear-${viewMonth.padStart(2, '0')}-${viewDay.padStart(2, '0')}") {
                                loadMenu()
                            }
                            if (failed) {
                                snackbarHostState.showSnackbar("Часть блюд не скопирована")
                            } else {
                                snackbarHostState.showSnackbar("Скопировано блюд: $successCount")
                            }
                        }
                    },
                    enabled = copyDay.length == 2 && copyMonth.length == 2 && copyYear.length == 4 && !isCopying
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!item.description.isNullOrBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
