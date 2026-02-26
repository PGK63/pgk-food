package com.example.pgk_food.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.pgk_food.data.remote.dto.MenuItemDto
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.util.MenuMealTypeCodec
import com.example.pgk_food.util.sortMenuItemsForUi
import kotlinx.coroutines.launch

@Composable
fun MenuScreenV2(token: String, studentRepository: StudentRepository) {
    var isLoading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<MenuItemDto>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadMenu() {
        scope.launch {
            isLoading = true
            errorText = null
            studentRepository.getMenu(token)
                .onSuccess { items = it }
                .onFailure { errorText = "Не удалось загрузить меню: ${it.userMessage}" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMenu() }

    if (isLoading) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorText != null) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorText!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val sortedItems = remember(items) { sortMenuItemsForUi(items) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Меню столовой", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sortedItems.isEmpty()) {
            item { Text("Меню пока пустое", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(sortedItems, key = { it.id }) { item ->
                val decoded = MenuMealTypeCodec.decode(item.description)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        androidx.compose.material3.Icon(Icons.Rounded.Restaurant, contentDescription = null)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(decoded.mealType.titleRu) }
                        )
                        if (decoded.description.isNotBlank()) {
                            Text(decoded.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(item.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
