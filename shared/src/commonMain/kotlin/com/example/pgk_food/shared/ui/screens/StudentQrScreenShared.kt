package com.example.pgk_food.shared.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.data.remote.dto.QrPayload
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.platform.PlatformQrCodeImage
import com.example.pgk_food.shared.platform.currentTimeMillis
import com.example.pgk_food.shared.platform.generateQrNonce
import com.example.pgk_food.shared.platform.generateQrSignature
import com.example.pgk_food.shared.ui.theme.GlassSurface
import com.example.pgk_food.shared.ui.theme.HeroCardShape
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.ui.viewmodels.DownloadKeysState
import com.example.pgk_food.shared.ui.viewmodels.StudentViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun StudentQrScreenShared(
    session: UserSession,
    mealType: String,
    viewModel: StudentViewModel,
) {
    val studentRepository = remember { StudentRepository() }
    var timeLeft by remember { mutableIntStateOf(60) }
    var qrContent by remember { mutableStateOf("") }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var qrError by remember { mutableStateOf<String?>(null) }
    val downloadKeysState by viewModel.downloadKeysState.collectAsState()

    LaunchedEffect(downloadKeysState) {
        if (downloadKeysState is DownloadKeysState.Success) {
            viewModel.resetDownloadKeysState()
            refreshTrigger++
        }
    }

    LaunchedEffect(Unit) {
        val serverTime = studentRepository.getCurrentTime()
        serverTimeOffset = serverTime - currentTimeMillis()
        refreshTrigger++
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger <= 0) return@LaunchedEffect

        val privateKey = session.privateKey
        if (privateKey.isNullOrBlank()) {
            qrError = "ERROR_KEY"
            qrContent = ""
            timeLeft = 0
            return@LaunchedEffect
        }

        val timestampMs = currentTimeMillis() + serverTimeOffset
        val timestampSec = timestampMs / 1000
        val roundedTimestamp = (timestampSec / 60) * 60
        val nonce = generateQrNonce()

        val signature = generateQrSignature(
            userId = session.userId,
            timestamp = roundedTimestamp,
            mealType = mealType,
            nonce = nonce,
            privateKeyBase64 = privateKey,
        )

        if (signature.isBlank()) {
            qrError = "ERROR_SIG"
            qrContent = ""
            timeLeft = 0
            return@LaunchedEffect
        }

        qrContent = Json.encodeToString(
            QrPayload(
                userId = session.userId,
                timestamp = roundedTimestamp,
                mealType = mealType,
                nonce = nonce,
                signature = signature,
            )
        )
        qrError = null
        timeLeft = 60
    }

    LaunchedEffect(qrContent, refreshTrigger, qrError) {
        if (qrContent.isBlank() || qrError != null) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        refreshTrigger++
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qr-glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .springEntrance(),
            shape = HeroCardShape,
            fillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ТАЛОН НА ПИТАНИЕ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayMealType(mealType),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        qrError != null -> {
                            QrErrorContentShared(
                                qrError = qrError!!,
                                onDownloadKeys = { viewModel.downloadKeys() },
                                onRefresh = { refreshTrigger++ },
                            )
                        }

                        qrContent.isNotEmpty() -> {
                            PlatformQrCodeImage(
                                content = qrContent,
                                modifier = Modifier.fillMaxSize(),
                                sizePx = 512
                            )
                        }

                        else -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Обновление через $timeLeft сек",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (downloadKeysState is DownloadKeysState.Loading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Загрузка ключей...", style = MaterialTheme.typography.labelMedium)
                }

                if (downloadKeysState is DownloadKeysState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val state = downloadKeysState as DownloadKeysState.Error
                    Text(
                        text = "Ошибка ключей [${state.code}]: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                if (timeLeft == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { refreshTrigger++ },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = PillShape
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обновить сейчас")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = PillShape,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Покажите этот QR-код повару",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QrErrorContentShared(
    qrError: String,
    onDownloadKeys: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (qrError == "ERROR_KEY") {
                "Ключи отсутствуют.\nСкачайте ключи для продолжения."
            } else {
                "Ошибка подписи.\nПопробуйте обновить."
            },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (qrError == "ERROR_KEY") {
            TextButton(onClick = onDownloadKeys) {
                Text("Скачать ключи")
            }
        } else {
            TextButton(onClick = onRefresh) {
                Text("Обновить")
            }
        }
    }
}

private fun displayMealType(mealType: String): String = when (mealType.uppercase()) {
    "BREAKFAST" -> "ЗАВТРАК"
    "LUNCH" -> "ОБЕД"
    "DINNER" -> "УЖИН"
    "SNACK" -> "ПОЛДНИК"
    "SPECIAL" -> "СПЕЦПИТАНИЕ"
    else -> mealType.uppercase()
}
