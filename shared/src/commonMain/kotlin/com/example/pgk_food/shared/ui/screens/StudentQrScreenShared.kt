package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.data.remote.dto.QrPayload
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.platform.PlatformQrCodeImage
import com.example.pgk_food.shared.platform.currentTimeMillis
import com.example.pgk_food.shared.platform.generateQrNonce
import com.example.pgk_food.shared.platform.generateQrSignature
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun StudentQrScreenShared(session: UserSession, mealType: String) {
    val studentRepository = remember { StudentRepository() }
    var timeLeft by remember { mutableIntStateOf(30) }
    var qrContent by remember { mutableStateOf("") }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val serverTime = studentRepository.getCurrentTime()
        serverTimeOffset = serverTime - currentTimeMillis()
    }

    LaunchedEffect(qrContent) {
        if (qrContent.isNotEmpty()) {
            timeLeft = 30
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            qrContent = ""
        }
    }

    LaunchedEffect(qrContent, mealType, session.userId, session.privateKey, serverTimeOffset) {
        if (qrContent.isNotEmpty()) return@LaunchedEffect
        val privateKey = session.privateKey
        if (privateKey.isNullOrBlank()) return@LaunchedEffect

        val timestampMs = currentTimeMillis() + serverTimeOffset
        val timestampSec = timestampMs / 1000
        val roundedTimestamp = (timestampSec / 30) * 30
        val nonce = generateQrNonce()

        val signature = generateQrSignature(
            userId = session.userId,
            timestamp = roundedTimestamp,
            mealType = mealType,
            nonce = nonce,
            privateKeyBase64 = privateKey,
        )
        if (signature.isBlank()) return@LaunchedEffect

        qrContent = Json.encodeToString(
            QrPayload(
                userId = session.userId,
                timestamp = roundedTimestamp,
                mealType = mealType,
                nonce = nonce,
                signature = signature,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ПРОПУСК В СТОЛОВУЮ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = mealType.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrContent.isNotEmpty()) {
                        PlatformQrCodeImage(
                            content = qrContent,
                            modifier = Modifier.fillMaxSize(),
                            sizePx = 512
                        )
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Обновление: $timeLeft сек",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (timeLeft == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { qrContent = "" }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ОБНОВИТЬ СЕЙЧАС")
                    }
                }

                if (session.privateKey.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "У пользователя отсутствует приватный ключ",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Покажите этот код повару на раздаче",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
