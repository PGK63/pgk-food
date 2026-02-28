package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.data.remote.dto.LoginRequest
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.platformName
import com.example.pgk_food.shared.ui.theme.GlassSurface
import com.example.pgk_food.shared.ui.theme.HeroCardShape
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.springEntrance
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authRepository: AuthRepository) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showValidation by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isLoginValid = login.trim().isNotEmpty()
    val isPasswordValid = password.isNotEmpty()
    val canSubmit = !isLoading && isLoginValid && isPasswordValid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            val isCompactHeight = maxHeight < 700.dp
            val verticalPadding = if (isCompactHeight) 12.dp else 24.dp
            val logoSize = if (isCompactHeight) 84.dp else 100.dp
            val logoSpacer = if (isCompactHeight) 16.dp else 24.dp
            val formSpacer = if (isCompactHeight) 20.dp else 32.dp
            val cardPadding = if (isCompactHeight) 16.dp else 20.dp
            val titleSpacer = if (isCompactHeight) 12.dp else 16.dp
            val buttonSpacer = if (isCompactHeight) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isCompactHeight) Arrangement.Top else Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(logoSize).springEntrance(),
                    shape = HeroCardShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("PGK", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(logoSpacer))

                Text(
                    text = "ПГК ПИТАНИЕ",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.springEntrance(50)
                )
                Text(
                    text = "Система управления питанием",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.springEntrance(100)
                )

                Spacer(Modifier.height(formSpacer))

                GlassSurface(
                    modifier = Modifier.fillMaxWidth().springEntrance(150),
                    shape = RoundedCornerShape(24.dp),
                    fillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ) {
                    Column(modifier = Modifier.padding(cardPadding)) {
                        Text(
                            "Вход",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(titleSpacer))
                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Логин или эл. почта") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            isError = showValidation && !isLoginValid,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Пароль") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            isError = showValidation && !isPasswordValid,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        if (showValidation && (!isLoginValid || !isPasswordValid)) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Введите логин и пароль",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        errorMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(buttonSpacer))
                        Button(
                            onClick = {
                                showValidation = true
                                if (!canSubmit) return@Button
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = authRepository.login(LoginRequest(login.trim(), password))
                                    isLoading = false
                                    if (result.isFailure) {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Ошибка авторизации"
                                    }
                                }
                            },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.width(18.dp).height(18.dp)
                                )
                            } else {
                                Text("Войти")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Платформа: ${platformName()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
