package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.pgk_food.shared.data.remote.dto.CuratorCreateStudentRequest
import com.example.pgk_food.shared.data.remote.dto.CuratorStudentRow
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.repository.CuratorRepository
import com.example.pgk_food.shared.model.StudentCategory
import com.example.pgk_food.shared.ui.components.CredentialsDialog
import com.example.pgk_food.shared.ui.components.UserCredentialsUi
import kotlinx.coroutines.launch

private fun StudentCategory.titleRu(): String = when (this) {
    StudentCategory.SVO -> "СВО"
    StudentCategory.MANY_CHILDREN -> "Многодетные"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratorCategoriesScreen(
    token: String,
    curatorId: String,
    curatorRepository: CuratorRepository
) {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var students by remember { mutableStateOf<List<CuratorStudentRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showGroupMenu by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createdCredentials by remember { mutableStateOf<UserCredentialsUi?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    fun loadGroups() {
        scope.launch {
            val result = curatorRepository.getCuratorGroups(token, curatorId)
            groups = result.getOrDefault(emptyList())
            if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
                selectedGroupId = null
            }
            if (result.isFailure) {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.userMessageOr("Не удалось загрузить группы") ?: "Не удалось загрузить группы")
            }
        }
    }

    fun loadStudents() {
        scope.launch {
            isLoading = true
            val result = curatorRepository.listMyStudents(token, selectedGroupId)
            students = result.getOrDefault(emptyList())
            if (result.isFailure) {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.userMessageOr("Не удалось загрузить студентов") ?: "Не удалось загрузить студентов")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadGroups() }
    LaunchedEffect(selectedGroupId) { loadStudents() }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Категории студентов", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

            if (groups.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = showGroupMenu, onExpandedChange = { showGroupMenu = it }) {
                    OutlinedTextField(
                        value = groups.firstOrNull { it.id == selectedGroupId }?.name ?: "Все группы",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Группа") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGroupMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Все группы") },
                            onClick = {
                                selectedGroupId = null
                                showGroupMenu = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    showGroupMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { showCreateDialog = true },
                enabled = selectedGroupId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp).padding(4.dp))
                Text("Добавить студента")
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedGroupId == null) "У вас пока нет студентов"
                        else "В выбранной группе нет студентов",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(students, key = { it.userId }) { student ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
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
                                            onClick = {
                                                if (student.studentCategory == category) return@FilterChip
                                                scope.launch {
                                                    curatorRepository.updateStudentCategory(token, student.userId, category)
                                                        .onSuccess {
                                                            students = students.map {
                                                                if (it.userId == student.userId) it.copy(studentCategory = category) else it
                                                            }
                                                            snackbarHostState.showSnackbar("Категория обновлена")
                                                        }
                                                        .onFailure {
                                                            snackbarHostState.showSnackbar(it.userMessageOr("Ошибка обновления категории"))
                                                        }
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

    if (showCreateDialog) {
        CreateCuratorStudentDialog(
            groupName = groups.firstOrNull { it.id == selectedGroupId }?.name,
            onDismiss = { showCreateDialog = false },
            onConfirm = { req ->
                val targetGroupId = selectedGroupId
                if (targetGroupId == null) {
                    scope.launch { snackbarHostState.showSnackbar("Сначала выберите группу") }
                    return@CreateCuratorStudentDialog
                }
                scope.launch {
                    curatorRepository.createStudent(
                        token = token,
                        request = CuratorCreateStudentRequest(
                            name = req.name,
                            surname = req.surname,
                            fatherName = req.fatherName,
                            groupId = targetGroupId,
                            studentCategory = req.studentCategory
                        )
                    ).onSuccess {
                        createdCredentials = UserCredentialsUi(
                            login = it.login,
                            password = it.passwordClearText
                        )
                        showCreateDialog = false
                        loadStudents()
                    }.onFailure {
                        snackbarHostState.showSnackbar(it.userMessageOr("Ошибка создания студента"))
                    }
                }
            }
        )
    }

    createdCredentials?.let { created ->
        CredentialsDialog(
            credentials = created,
            onDismiss = { createdCredentials = null },
            onCopiedAndDismissed = {
                scope.launch { snackbarHostState.showSnackbar("Данные скопированы") }
                createdCredentials = null
            }
        )
    }
}

private data class CuratorNewStudentForm(
    val surname: String,
    val name: String,
    val fatherName: String,
    val studentCategory: StudentCategory
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCuratorStudentDialog(
    groupName: String?,
    onDismiss: () -> Unit,
    onConfirm: (CuratorNewStudentForm) -> Unit,
) {
    var surname by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(StudentCategory.MANY_CHILDREN) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый студент") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Группа: ${groupName ?: "-"}")
                OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Фамилия") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Отчество") }, singleLine = true)

                ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = it }) {
                    OutlinedTextField(
                        value = category.titleRu(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        StudentCategory.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.titleRu()) },
                                onClick = {
                                    category = item
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CuratorNewStudentForm(
                            surname = surname,
                            name = name,
                            fatherName = fatherName.ifBlank { "-" },
                            studentCategory = category
                        )
                    )
                },
                enabled = surname.isNotBlank() && name.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
