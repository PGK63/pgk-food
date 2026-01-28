package com.example.pgk_food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.repository.MealsTodayResponse
import com.example.pgk_food.data.repository.StudentRepository

@Composable
fun MyCouponsScreen(
    token: String,
    studentRepository: StudentRepository,
    onCouponClick: (String) -> Unit
) {
    var mealsToday by remember { mutableStateOf<MealsTodayResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = studentRepository.getMealsToday(token)
        mealsToday = result.getOrNull()
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
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Выбери талон, чтобы показать QR-код",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        CouponItem(name) { onCouponClick(type) }
                    }
                }
            }
        }
    }
}

@Composable
fun CouponItem(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
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
                        Icons.Default.ConfirmationNumber, 
                        contentDescription = null, 
                        tint = Color.White
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
                    Icons.Default.QrCode, 
                    contentDescription = null, 
                    modifier = Modifier.padding(8.dp).size(24.dp),
                    tint = Color.White
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
            Icons.Default.ConfirmationNumber, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "У вас нет доступных талонов",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
