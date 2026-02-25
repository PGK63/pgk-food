package com.example.pgk_food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.repository.MealsTodayResponse
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.ui.theme.HeroCardShape
import com.example.pgk_food.ui.theme.springEntrance

@Composable
fun MyCouponsScreen(
    token: String,
    studentRepository: StudentRepository,
    onCouponClick: (String) -> Unit
) {
    var mealsToday by remember { mutableStateOf<MealsTodayResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var isOfflineMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = studentRepository.getMealsToday(token)
        if (result.isSuccess) {
            mealsToday = result.getOrNull()
        } else {
            isOfflineMode = true
            mealsToday = MealsTodayResponse(
                date = "Сегодня",
                isBreakfastAllowed = true,
                isLunchAllowed = true,
                isDinnerAllowed = true,
                isSnackAllowed = false,
                isSpecialAllowed = false
            )
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Твои талоны",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.springEntrance()
                )
                Text(
                    text = "Выбери талон, чтобы показать QR-код",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOfflineMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = com.example.pgk_food.ui.theme.PillShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.Warning, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Нет сети. Показаны базовые талоны.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            mealsToday?.let { meals ->
                val availableMeals = mutableListOf<Pair<String, String>>()
                if (meals.isBreakfastAllowed) availableMeals.add("Завтрак" to "BREAKFAST")
                if (meals.isLunchAllowed) availableMeals.add("Обед" to "LUNCH")
                if (meals.isDinnerAllowed) availableMeals.add("Ужин" to "DINNER")
                if (meals.isSnackAllowed) availableMeals.add("Полдник" to "SNACK")
                if (meals.isSpecialAllowed) availableMeals.add("Спец. питание" to "SPECIAL")

                if (availableMeals.isEmpty()) {
                    item {
                        EmptyCouponsState()
                    }
                } else {
                    items(availableMeals.size) { index ->
                        val (name, type) = availableMeals[index]
                        CouponItem(name, delay = index * 60) { onCouponClick(type) }
                    }
                }
            }
        }
    }
}

@Composable
fun CouponItem(name: String, delay: Int = 0, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .springEntrance(delay = delay)
            .clickable { onClick() },
        shape = HeroCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ConfirmationNumber, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name, 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "На сегодня",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Rounded.QrCode, 
                    contentDescription = null, 
                    modifier = Modifier.padding(8.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyCouponsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.ConfirmationNumber, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "У вас нет доступных талонов",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
