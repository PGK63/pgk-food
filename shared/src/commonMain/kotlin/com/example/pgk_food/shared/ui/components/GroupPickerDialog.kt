package com.example.pgk_food.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.GroupDto

@Composable
fun GroupPickerDialog(
    groups: List<GroupDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectGroup: (GroupDto) -> Unit,
) {
    val queryTokens = remember(searchQuery) {
        searchQuery.trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }
    val filteredGroups = remember(groups, queryTokens) {
        groups
            .filter { group ->
                if (queryTokens.isEmpty()) return@filter true
                val searchable = "${group.name} ${group.id}".lowercase()
                queryTokens.all { token -> searchable.contains(token) }
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Выбор группы") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск группы") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Найдено: ${filteredGroups.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.heightIn(max = 420.dp)) {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text("Все группы") },
                                supportingContent = { Text("Без фильтра по группе") },
                                modifier = Modifier.clickable { onSelectAll() }
                            )
                            HorizontalDivider()
                        }
                        if (filteredGroups.isEmpty()) {
                            item {
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        } else {
                            items(filteredGroups, key = { it.id }) { group ->
                                ListItem(
                                    headlineContent = { Text(group.name) },
                                    supportingContent = { Text("ID: ${group.id}") },
                                    modifier = Modifier.clickable { onSelectGroup(group) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
