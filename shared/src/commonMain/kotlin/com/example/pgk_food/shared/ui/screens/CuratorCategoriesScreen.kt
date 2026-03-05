package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.remote.dto.CuratorStudentRow
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.repository.CuratorRepository
import com.example.pgk_food.shared.model.StudentCategory
import com.example.pgk_food.shared.ui.components.GroupPickerDialog
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.components.LocalAppSnackbarDispatcher
import com.example.pgk_food.shared.ui.components.longPressHelp
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.util.HintScreenKey
import kotlinx.coroutines.launch

private fun StudentCategory.titleRu(): String = when (this) {
    StudentCategory.SVO -> "СВО"
    StudentCategory.MANY_CHILDREN -> "Многодетные"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorCategoriesScreen(
    token: String,
    curatorRepository: CuratorRepository,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var students by remember { mutableStateOf<List<CuratorStudentRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var groupSearchQuery by remember { mutableStateOf("") }
    var updatingStudentId by remember { mutableStateOf<String?>(null) }

    val snackbarDispatcher = LocalAppSnackbarDispatcher.current
    val scope = rememberCoroutineScope()
    val hintContent = remember { HintCatalog.content(HintScreenKey.CURATOR_CATEGORIES) }

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    fun loadGroups() {
        scope.launch {
            val result = curatorRepository.getCuratorGroups(token)
            groups = result.getOrDefault(emptyList())
            selectedGroupId = when {
                groups.isEmpty() -> null
                selectedGroupId == null -> groups.first().id
                groups.none { it.id == selectedGroupId } -> groups.first().id
                else -> selectedGroupId
            }
            if (result.isFailure) {
                snackbarDispatcher.show(result.exceptionOrNull()?.userMessageOr("Не удалось загрузить группы") ?: "Не удалось загрузить группы")
            }
        }
    }

    fun loadStudents() {
        scope.launch {
            isLoading = true
            val result = curatorRepository.listMyStudents(token, selectedGroupId)
            students = result.getOrDefault(emptyList())
            if (result.isFailure) {
                snackbarDispatcher.show(result.exceptionOrNull()?.userMessageOr("Не удалось загрузить студентов") ?: "Не удалось загрузить студентов")
            }
            isLoading = false
        }
    }

    LaunchedEffect(token) { loadGroups() }
    LaunchedEffect(selectedGroupId) { loadStudents() }

    val selectedGroupLabel = remember(groups, selectedGroupId) {
        groups.firstOrNull { it.id == selectedGroupId }?.let { "${it.name} (#${it.id})" }
            ?: "Выберите группу"
    }

    if (showGroupPicker) {
        GroupPickerDialog(
            groups = groups,
            searchQuery = groupSearchQuery,
            onSearchQueryChange = { groupSearchQuery = it },
            onDismiss = {
                showGroupPicker = false
                groupSearchQuery = ""
            },
            onSelectAll = {
                selectedGroupId = groups.firstOrNull()?.id
                showGroupPicker = false
                groupSearchQuery = ""
            },
            onSelectGroup = {
                selectedGroupId = it.id
                showGroupPicker = false
                groupSearchQuery = ""
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showHints) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HowItWorksCard(
                        title = hintContent.title,
                        steps = hintContent.steps,
                        note = hintContent.note,
                        onDismiss = onDismissHints,
                        modifier = Modifier.springEntrance(40)
                    )
                    hintContent.inlineHints.firstOrNull()?.let { inline ->
                        Spacer(modifier = Modifier.height(8.dp))
                        InlineHint(text = inline, modifier = Modifier.springEntrance(50))
                    }
                }

                if (groups.isNotEmpty()) {
                    OutlinedTextField(
                        value = groupSearchQuery,
                        onValueChange = {
                            groupSearchQuery = it
                            showGroupPicker = true
                        },
                        label = { Text("Группа") },
                        placeholder = { Text(selectedGroupLabel) },
                        trailingIcon = {
                            IconButton(
                                onClick = { showGroupPicker = true },
                                modifier = Modifier.longPressHelp(
                                    actionId = "categories.group.search",
                                    fallbackDescription = "Поиск группы",
                                ),
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = "Поиск группы")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().springEntrance(60),
                        singleLine = true
                    )
                }

                if (updatingStudentId != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (students.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            if (groups.isEmpty()) "У вас нет доступных групп"
                            else if (selectedGroupId == null) "Сначала выберите группу"
                            else "В выбранной группе нет студентов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(students, key = { _, student -> student.userId }) { index, student ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springEntrance((index.coerceAtMost(9) * 40) + 110),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(student.groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (student.studentCategory == null) {
                                        Text(
                                            "Категория не назначена",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StudentCategory.entries.forEach { category ->
                                            FilterChip(
                                                selected = student.studentCategory == category,
                                                enabled = updatingStudentId == null,
                                                onClick = {
                                                    if (updatingStudentId != null) return@FilterChip
                                                    if (student.studentCategory == category) return@FilterChip
                                                    scope.launch {
                                                        updatingStudentId = student.userId
                                                        curatorRepository.updateStudentCategory(token, student.userId, category)
                                                            .onSuccess {
                                                                loadStudents()
                                                                snackbarDispatcher.show("Категория обновлена")
                                                            }
                                                            .onFailure {
                                                                snackbarDispatcher.show(it.userMessageOr("Ошибка обновления категории"))
                                                            }
                                                        updatingStudentId = null
                                                    }
                                                },
                                                label = { Text(category.titleRu()) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
