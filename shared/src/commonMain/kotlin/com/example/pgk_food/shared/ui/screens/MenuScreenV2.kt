package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.remote.dto.MenuItemDto
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.util.HintScreenKey
import com.example.pgk_food.shared.util.MenuMealTypeCodec
import com.example.pgk_food.shared.util.sortMenuItemsForUi
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuScreenV2(
    token: String,
    studentRepository: StudentRepository,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<MenuItemDto>>(emptyList()) }
    var locations by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hintContent = remember { HintCatalog.content(HintScreenKey.STUDENT_MENU) }

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    fun loadMenu(loadLocations: Boolean = false) {
        scope.launch {
            isLoading = true
            errorText = null
            if (loadLocations) {
                studentRepository.getMenuLocations(token)
                    .onSuccess { loaded ->
                        locations = loaded.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                        if (selectedLocation != null && selectedLocation !in locations) {
                            selectedLocation = null
                        }
                    }
            }
            studentRepository.getMenu(token, location = selectedLocation)
                .onSuccess { items = it }
                .onFailure { errorText = it.userMessageOr("Не удалось загрузить меню") }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMenu(loadLocations = true) }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorText != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { loadMenu() }) {
                    Text("Повторить")
                }
            }
        }
        return
    }

    val sortedItems = remember(items) { sortMenuItemsForUi(items) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Меню столовой",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.springEntrance(),
            )
            if (showHints) {
                Spacer(modifier = Modifier.height(8.dp))
                HowItWorksCard(
                    title = hintContent.title,
                    steps = hintContent.steps,
                    note = hintContent.note,
                    onDismiss = onDismissHints,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (locations.isNotEmpty()) {
                Text(
                    "Столовая",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLocation == null,
                        onClick = {
                            selectedLocation = null
                            loadMenu()
                        },
                        label = { Text("Все") }
                    )
                    locations.forEach { location ->
                        FilterChip(
                            selected = selectedLocation == location,
                            onClick = {
                                selectedLocation = location
                                loadMenu()
                            },
                            label = { Text(location) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (sortedItems.isEmpty()) {
            item { Text("Меню пока пустое", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            itemsIndexed(sortedItems, key = { _, item -> item.id }) { index, item ->
                val decoded = MenuMealTypeCodec.decode(item.description)
                Card(
                    modifier = Modifier.fillMaxWidth().springEntrance(index * 60),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Restaurant, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        AssistChip(onClick = {}, enabled = false, label = { Text(decoded.mealType.titleRu) })
                        if (decoded.description.isNotBlank()) {
                            Text(decoded.description, style = MaterialTheme.typography.bodySmall)
                        }
                        if (selectedLocation == null) {
                            Text(
                                item.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
