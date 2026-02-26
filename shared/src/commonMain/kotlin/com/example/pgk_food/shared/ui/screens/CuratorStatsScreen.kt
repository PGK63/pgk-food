package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.StudentMealStatus
import com.example.pgk_food.shared.data.repository.CuratorRepository
import com.example.pgk_food.shared.ui.components.ThreeInputDatePicker
import com.example.pgk_food.shared.ui.util.todayDateParts
import kotlinx.coroutines.launch

@Composable
fun CuratorStatsScreen(token: String, curatorRepository: CuratorRepository) {
    var stats by remember { mutableStateOf<List<StudentMealStatus>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val today = remember { todayDateParts() }

    // States for viewing stats
    var viewDay by remember { mutableStateOf(today.day) }
    var viewMonth by remember { mutableStateOf(today.month) }
    var viewYear by remember { mutableStateOf(today.year) }

    fun loadStats() {
        scope.launch {
            isLoading = true
            val dateStr = "$viewYear-${viewMonth.padStart(2, '0')}-${viewDay.padStart(2, '0')}"
            val result = curatorRepository.getMyGroupStatistics(token, dateStr)
            stats = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStats()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Статистика группы", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { loadStats() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text("Выберите дату:", style = MaterialTheme.typography.titleSmall)
        ThreeInputDatePicker(
            day = viewDay,
            month = viewMonth,
            year = viewYear,
            onDayChange = { viewDay = it },
            onMonthChange = { viewMonth = it },
            onYearChange = { viewYear = it },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { loadStats() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            enabled = viewDay.length == 2 && viewMonth.length == 2 && viewYear.length == 4
        ) {
            Text("Показать статистику")
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(stats) { item ->
                    ListItem(
                        headlineContent = { Text(item.fullName) },
                        supportingContent = { 
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MealStatusIcon("З", item.hadBreakfast)
                                MealStatusIcon("О", item.hadLunch)
                                MealStatusIcon("У", item.hadDinner)
                                MealStatusIcon("П", item.hadSnack)
                                MealStatusIcon("С", item.hadSpecial)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun MealStatusIcon(label: String, hasHad: Boolean) {
    Surface(
        color = if (hasHad) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasHad) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
