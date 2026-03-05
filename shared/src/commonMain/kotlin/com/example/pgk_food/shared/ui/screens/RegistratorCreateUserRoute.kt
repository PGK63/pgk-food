@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.remote.dto.CreateUserRequest
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.repository.RegistratorRepository
import com.example.pgk_food.shared.model.StudentCategory
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.ui.components.LocalAppSnackbarDispatcher
import com.example.pgk_food.shared.ui.components.CredentialsDialog
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.components.UserCredentialsUi
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.util.HintScreenKey
import kotlinx.coroutines.launch

private fun UserRole.titleRu(): String = when (this) {
    UserRole.ADMIN -> "Администратор"
    UserRole.REGISTRATOR -> "Регистратор"
    UserRole.CHEF -> "Повар"
    UserRole.CURATOR -> "Куратор"
    UserRole.STUDENT -> "Студент"
}

private fun StudentCategory.titleRu(): String = when (this) {
    StudentCategory.SVO -> "СВО"
    StudentCategory.MANY_CHILDREN -> "Многодетные"
}

@Composable
fun RegistratorCreateUserRoute(
    token: String,
    registratorRepository: RegistratorRepository,
    initialGroupId: Int? = null,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
    onBack: () -> Unit,
    onUserCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarDispatcher = LocalAppSnackbarDispatcher.current
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isSubmitting = actionState.value.isLoading

    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var credentialsDialog by remember { mutableStateOf<UserCredentialsUi?>(null) }

    fun loadGroups() {
        scope.launch {
            isLoading = true
            val groupsResult = registratorRepository.getGroups(token)
            groups = groupsResult.getOrDefault(emptyList())
            if (groupsResult.isFailure) {
                snackbarDispatcher.show("Не удалось загрузить группы")
            }
            isLoading = false
        }
    }

    LaunchedEffect(token) { loadGroups() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                RegistratorCreateUserForm(
                    groups = groups,
                    initialGroupId = initialGroupId,
                    isSubmitting = isSubmitting,
                    showHints = showHints,
                    onDismissHints = onDismissHints,
                    contentPadding = PaddingValues(0.dp),
                    onBack = onBack,
                    onSubmit = { request ->
                        scope.launch {
                            val result = registratorRepository.createUser(token, request)
                            val ok = runUiAction(
                                actionState = actionState,
                                successMessage = "Пользователь создан",
                                fallbackErrorMessage = "Ошибка создания пользователя",
                                emitSuccessFeedback = false
                            ) { result }
                            if (ok) {
                                result.getOrNull()?.let {
                                    credentialsDialog = UserCredentialsUi(it.login, it.passwordClearText)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    credentialsDialog?.let { result ->
        CredentialsDialog(
            credentials = result,
            onDismiss = {
                credentialsDialog = null
                onUserCreated()
            },
            onCopiedAndDismissed = {
                scope.launch { snackbarDispatcher.show("Данные скопированы") }
                credentialsDialog = null
                onUserCreated()
            }
        )
    }
}

@Composable
private fun RegistratorCreateUserForm(
    groups: List<GroupDto>,
    initialGroupId: Int?,
    isSubmitting: Boolean,
    showHints: Boolean,
    onDismissHints: () -> Unit,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSubmit: (CreateUserRequest) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val surnameRequester = remember { FocusRequester() }
    val nameRequester = remember { FocusRequester() }
    val fatherNameRequester = remember { FocusRequester() }

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(setOf(UserRole.STUDENT)) }
    var selectedGroupId by remember(initialGroupId) { mutableStateOf(initialGroupId) }
    var expandedGroup by remember { mutableStateOf(false) }
    var selectedStudentCategory by remember { mutableStateOf<StudentCategory?>(null) }
    val hintContent = remember { HintCatalog.content(HintScreenKey.REGISTRATOR_USER_CREATE) }
    val requiresGroup = UserRole.STUDENT in selectedRoles || UserRole.CURATOR in selectedRoles
    val requiresCategory = UserRole.STUDENT in selectedRoles
    val canSubmit = !isSubmitting &&
        name.isNotBlank() &&
        surname.isNotBlank() &&
        fatherName.isNotBlank() &&
        selectedRoles.isNotEmpty() &&
        (!requiresGroup || selectedGroupId != null) &&
        (!requiresCategory || selectedStudentCategory != null)

    LaunchedEffect(Unit) {
        surnameRequester.requestFocus()
    }
    LaunchedEffect(initialGroupId) {
        if (initialGroupId != null && selectedGroupId == null) {
            selectedGroupId = initialGroupId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Создать пользователя",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.springEntrance()
        )
        if (showHints) {
            HowItWorksCard(
                title = hintContent.title,
                steps = hintContent.steps,
                note = hintContent.note,
                onDismiss = onDismissHints,
                modifier = Modifier.springEntrance(20),
            )
            hintContent.inlineHints.firstOrNull()?.let { inline ->
                InlineHint(
                    text = inline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .springEntrance(30)
                )
            }
        }

        if (isSubmitting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Фамилия") },
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(surnameRequester)
                .springEntrance(40),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onNext = { nameRequester.requestFocus() }
            )
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя") },
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameRequester)
                .springEntrance(70),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onNext = { fatherNameRequester.requestFocus() }
            )
        )

        OutlinedTextField(
            value = fatherName,
            onValueChange = { fatherName = it },
            label = { Text("Отчество") },
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fatherNameRequester)
                .springEntrance(100),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Text(
            "Роли",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.springEntrance(130)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().springEntrance(150),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserRole.entries.forEach { role ->
                FilterChip(
                    selected = role in selectedRoles,
                    onClick = {
                        selectedRoles = if (role in selectedRoles) selectedRoles - role else selectedRoles + role
                    },
                    label = { Text(role.titleRu()) },
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        if (UserRole.STUDENT in selectedRoles || UserRole.CURATOR in selectedRoles) {
            Text(
                "Группа",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.springEntrance(180)
            )
            ExposedDropdownMenuBox(
                expanded = expandedGroup,
                onExpandedChange = { expandedGroup = it }
            ) {
                OutlinedTextField(
                    value = groups.find { it.id == selectedGroupId }?.name ?: "Без группы",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroup) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().springEntrance(200),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                ExposedDropdownMenu(expanded = expandedGroup, onDismissRequest = { expandedGroup = false }) {
                    DropdownMenuItem(
                        text = { Text("Без группы") },
                        onClick = { selectedGroupId = null; expandedGroup = false }
                    )
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = { selectedGroupId = group.id; expandedGroup = false }
                        )
                    }
                }
            }
        }

        if (UserRole.STUDENT in selectedRoles) {
            Text(
                "Категория студента",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.springEntrance(220)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().springEntrance(240),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudentCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedStudentCategory == category,
                        onClick = { selectedStudentCategory = category },
                        label = { Text(category.titleRu()) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().springEntrance(280),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            Button(
                onClick = {
                    onSubmit(
                        CreateUserRequest(
                            roles = selectedRoles.toList(),
                            name = name,
                            surname = surname,
                            fatherName = fatherName,
                            groupId = selectedGroupId,
                            studentCategory = if (UserRole.STUDENT in selectedRoles) selectedStudentCategory else null
                        )
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.weight(1f)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создание...")
                } else {
                    Text("Создать")
                }
            }
        }
    }
}
