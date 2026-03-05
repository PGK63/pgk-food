package com.example.pgk_food.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.remote.dto.QrPayload
import com.example.pgk_food.shared.data.repository.MealsTodayResponse
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.platform.PlatformQrCodeImage
import com.example.pgk_food.shared.platform.PlatformQrBrightnessEffect
import com.example.pgk_food.shared.platform.currentTimeMillis
import com.example.pgk_food.shared.platform.generateQrNonce
import com.example.pgk_food.shared.platform.generateQrSignature
import com.example.pgk_food.shared.platform.getLastQrSignatureDebugInfo
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.theme.GlassSurface
import com.example.pgk_food.shared.ui.theme.HeroCardShape
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.ui.viewmodels.DownloadKeysState
import com.example.pgk_food.shared.ui.viewmodels.StudentViewModel
import com.example.pgk_food.shared.util.HintScreenKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val BOOTSTRAP_TIMEOUT_MS = 5000L
private const val QR_REFRESH_INTERVAL_SEC = 60

@Composable
fun StudentQrScreenShared(
    session: UserSession,
    mealType: String,
    viewModel: StudentViewModel,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    val studentRepository = remember { StudentRepository() }
    var timeLeft by remember { mutableIntStateOf(0) }
    var qrContent by remember { mutableStateOf("") }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var qrError by remember { mutableStateOf<String?>(null) }
    var qrWarning by remember { mutableStateOf<String?>(null) }
    var qrSignatureDebugInfo by remember { mutableStateOf("SIG_NOT_RUN") }
    var signatureRetryTriggered by remember { mutableStateOf(false) }
    var bootstrapTrigger by remember(mealType) { mutableIntStateOf(0) }
    var awaitingKeyRefreshForGeneration by remember(mealType) { mutableStateOf(false) }
    var generationEnabled by remember(mealType) { mutableStateOf(false) }
    var usingCachedKeys by remember(mealType) { mutableStateOf(false) }
    var awaitingCachedKeysConsent by remember(mealType) { mutableStateOf(false) }
    val downloadKeysState by viewModel.downloadKeysState.collectAsState()
    val hintContent = remember { HintCatalog.content(HintScreenKey.STUDENT_QR) }

    // --- Offline mode state ---
    var isOfflineMode by remember(mealType) { mutableStateOf(false) }
    var showOfflineSuggestion by remember(mealType) { mutableStateOf(false) }
    var isBootstrapping by remember(mealType) { mutableStateOf(true) }

    // --- Bootstrap with timeout → offline mode suggestion ---
    LaunchedEffect(mealType, bootstrapTrigger) {
        generationEnabled = false
        awaitingKeyRefreshForGeneration = false
        usingCachedKeys = false
        awaitingCachedKeysConsent = false
        signatureRetryTriggered = false
        qrContent = ""
        qrError = null
        qrWarning = null
        qrSignatureDebugInfo = "SIG_BOOTSTRAP"
        timeLeft = 0
        showOfflineSuggestion = false
        isBootstrapping = true

        if (isOfflineMode) {
            // Offline mode: use local time, cached coupons, cached keys
            serverTimeOffset = 0L
            val cachedMeals = studentRepository.getMealsTodayCached()
            if (cachedMeals == null) {
                qrError = "ERROR_OFFLINE_DATA_MISSING"
                qrSignatureDebugInfo = "SIG_OFFLINE_DATA_MISSING"
                isBootstrapping = false
                return@LaunchedEffect
            }
            val policy = resolveMealStatusPolicy(
                status = cachedMeals.statusForMealType(mealType),
                source = MealStatusSource.OFFLINE_CACHE,
            )
            if (policy.errorCode != null) {
                qrError = policy.errorCode
                qrSignatureDebugInfo = when (policy.errorCode) {
                    "ERROR_COUPON_USED" -> "SIG_COUPON_USED"
                    "ERROR_COUPON_UNAVAILABLE" -> "SIG_COUPON_UNAVAILABLE"
                    else -> "SIG_PERMISSION_UNKNOWN"
                }
                isBootstrapping = false
                return@LaunchedEffect
            }
            qrWarning = policy.warningCode

            // In offline mode — skip key download, use cached keys directly
            generationEnabled = true
            usingCachedKeys = true
            isBootstrapping = false
            refreshTrigger++
            return@LaunchedEffect
        }

        // Online mode: try server with timeout
        val bootstrapResult = withTimeoutOrNull(BOOTSTRAP_TIMEOUT_MS) {
            val serverTime = studentRepository.getCurrentTime()
            serverTimeOffset = serverTime - currentTimeMillis()
            val mealResult = studentRepository.getMealsToday(session.token)
            Pair(serverTimeOffset, mealResult)
        }

        if (bootstrapResult == null) {
            // Timeout! Show offline suggestion
            serverTimeOffset = 0L
            showOfflineSuggestion = true
            isBootstrapping = false
            return@LaunchedEffect
        }

        val (_, mealResult) = bootstrapResult
        if (mealResult.isFailure) {
            val failure = mealResult.exceptionOrNull()
            qrError = if (failure.isAuthFailure()) "ERROR_AUTH_REQUIRED" else "ERROR_PERMISSION_UNKNOWN"
            qrSignatureDebugInfo = "SIG_BOOTSTRAP_${qrError ?: "UNKNOWN"}"
            isBootstrapping = false
            return@LaunchedEffect
        }
        val policy = resolveMealStatusPolicy(
            status = mealResult.getOrNull()
                ?.statusForMealType(mealType)
                ?: MealCouponStatus.UNKNOWN,
            source = MealStatusSource.ONLINE,
        )
        if (policy.errorCode != null) {
            qrError = policy.errorCode
            qrSignatureDebugInfo = when (policy.errorCode) {
                "ERROR_COUPON_USED" -> "SIG_COUPON_USED"
                "ERROR_COUPON_UNAVAILABLE" -> "SIG_COUPON_UNAVAILABLE"
                else -> "SIG_PERMISSION_UNKNOWN"
            }
            isBootstrapping = false
            return@LaunchedEffect
        }
        qrWarning = policy.warningCode

        isBootstrapping = false
        awaitingKeyRefreshForGeneration = true
        viewModel.resetDownloadKeysState()
        viewModel.downloadKeys()
    }

    LaunchedEffect(downloadKeysState, awaitingKeyRefreshForGeneration) {
        if (!awaitingKeyRefreshForGeneration) return@LaunchedEffect

        when (downloadKeysState) {
            is DownloadKeysState.Success -> {
                awaitingKeyRefreshForGeneration = false
                generationEnabled = true
                usingCachedKeys = false
                viewModel.resetDownloadKeysState()
                refreshTrigger++
            }

            is DownloadKeysState.Error -> {
                awaitingKeyRefreshForGeneration = false
                if (isOfflineMode) {
                    generationEnabled = true
                    usingCachedKeys = true
                } else {
                    generationEnabled = false
                    usingCachedKeys = false
                    awaitingCachedKeysConsent = true
                    qrError = "ERROR_KEY_REFRESH_REQUIRED"
                }
                viewModel.resetDownloadKeysState()
                if (generationEnabled) {
                    refreshTrigger++
                }
            }

            else -> Unit
        }
    }

    LaunchedEffect(refreshTrigger, generationEnabled) {
        if (refreshTrigger <= 0 || !generationEnabled) return@LaunchedEffect

        val activeSession = SessionStore.session.value ?: session
        val privateKey = activeSession.privateKey
        if (privateKey.isNullOrBlank()) {
            qrError = "ERROR_KEY"
            qrSignatureDebugInfo = "SIG_PRIVATE_KEY_EMPTY"
            qrContent = ""
            timeLeft = 0
            return@LaunchedEffect
        }

        val timestampMs = currentTimeMillis() + serverTimeOffset
        val timestampSec = timestampMs / 1000
        val roundedTimestamp = (timestampSec / 60) * 60
        val nonce = generateQrNonce()

        val signature = generateQrSignature(
            userId = activeSession.userId,
            timestamp = roundedTimestamp,
            mealType = mealType,
            nonce = nonce,
            privateKeyBase64 = privateKey,
            publicKeyBase64 = activeSession.publicKey,
        )

        if (signature.isBlank()) {
            qrError = "ERROR_SIG"
            qrSignatureDebugInfo = getLastQrSignatureDebugInfo().ifBlank { "SIG_UNKNOWN" }
            qrContent = ""
            timeLeft = 0
            return@LaunchedEffect
        }

        qrContent = Json.encodeToString(
            QrPayload(
                userId = activeSession.userId,
                timestamp = roundedTimestamp,
                mealType = mealType,
                nonce = nonce,
                signature = signature,
            )
        )
        qrError = null
        qrSignatureDebugInfo = "SIG_OK"
        signatureRetryTriggered = false
        timeLeft = QR_REFRESH_INTERVAL_SEC
    }

    LaunchedEffect(qrError, signatureRetryTriggered) {
        if (qrError != "ERROR_SIG" || signatureRetryTriggered) return@LaunchedEffect
        signatureRetryTriggered = true
        awaitingKeyRefreshForGeneration = true
        viewModel.downloadKeys()
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        PlatformQrBrightnessEffect(enabled = true)

        val isCompactHeight = maxHeight < 760.dp
        val verticalPadding = if (isCompactHeight) 48.dp else 64.dp
        val cardPadding = if (isCompactHeight) 20.dp else 32.dp
        val maxQrByWidth = maxWidth - if (isCompactHeight) 72.dp else 96.dp
        val maxQrByHeight = maxHeight - if (isCompactHeight) 440.dp else 500.dp
        val preferredQrSize = if (maxQrByWidth < maxQrByHeight) maxQrByWidth else maxQrByHeight
        val qrSizeCandidate = if (preferredQrSize > 0.dp) preferredQrSize else maxQrByWidth
        val qrSize = when {
            qrSizeCandidate < 228.dp -> 228.dp
            qrSizeCandidate > 360.dp -> 360.dp
            else -> qrSizeCandidate
        }
        val qrInnerPadding = 10.dp
        val qrInnerSize = qrSize - (qrInnerPadding * 2)
        val sectionSpacer = if (isCompactHeight) 20.dp else 32.dp
        val infoWidthFraction = if (isCompactHeight) 1f else 0.9f
        val contentBottomInset = if (isCompactHeight) 88.dp else 104.dp
        val density = LocalDensity.current
        val qrInnerSizePx = with(density) { qrInnerSize.roundToPx().coerceAtLeast(1) }
        val contentModifier = if (isCompactHeight) {
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = verticalPadding, bottom = contentBottomInset)
        } else {
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .fillMaxHeight()
                .padding(top = verticalPadding, bottom = contentBottomInset)
        }
        val contentArrangement = if (isCompactHeight) Arrangement.Top else Arrangement.Center

        Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = contentArrangement
        ) {
            if (showHints) {
                HowItWorksCard(
                    title = hintContent.title,
                    steps = hintContent.steps,
                    note = hintContent.note,
                    onDismiss = onDismissHints,
                )
                hintContent.inlineHints.firstOrNull()?.let { inline ->
                    Spacer(modifier = Modifier.height(8.dp))
                    InlineHint(text = inline)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- Offline badge ---
            AnimatedVisibility(
                visible = isOfflineMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = PillShape,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Оффлайн-режим",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .springEntrance(),
                    shape = HeroCardShape,
                    fillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Column(
                        modifier = Modifier.padding(cardPadding),
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

                        Spacer(modifier = Modifier.height(sectionSpacer))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(qrSize)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(qrInnerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    // Show offline suggestion when bootstrap timed out
                                    showOfflineSuggestion -> {
                                        OfflineSuggestionContent(
                                            onEnableOffline = {
                                                showOfflineSuggestion = false
                                                isOfflineMode = true
                                                bootstrapTrigger++
                                            },
                                            onRetry = {
                                                showOfflineSuggestion = false
                                                isOfflineMode = false
                                                bootstrapTrigger++
                                            },
                                        )
                                    }

                                    qrError != null -> {
                                        QrErrorContentShared(
                                            qrError = qrError!!,
                                            retryAttempted = signatureRetryTriggered,
                                            signatureDebugInfo = qrSignatureDebugInfo,
                                            onDownloadKeys = {
                                                awaitingKeyRefreshForGeneration = true
                                                viewModel.downloadKeys()
                                            },
                                            onUseCachedKeys = {
                                                awaitingCachedKeysConsent = false
                                                usingCachedKeys = true
                                                qrError = null
                                                generationEnabled = true
                                                refreshTrigger++
                                            },
                                            onRefresh = {
                                                if (shouldRebootstrapForError(qrError)) {
                                                    bootstrapTrigger++
                                                } else {
                                                    refreshTrigger++
                                                }
                                            },
                                        )
                                    }

                                    qrContent.isNotEmpty() -> {
                                        PlatformQrCodeImage(
                                            content = qrContent,
                                            modifier = Modifier.fillMaxSize(),
                                            sizePx = qrInnerSizePx
                                        )
                                    }

                                    else -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (!isOfflineMode) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Подключение...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                TextButton(
                                                    onClick = {
                                                        awaitingKeyRefreshForGeneration = false
                                                        viewModel.resetDownloadKeysState()
                                                        showOfflineSuggestion = false
                                                        isOfflineMode = true
                                                        bootstrapTrigger++
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.CloudOff,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Перейти в оффлайн сейчас",
                                                        style = MaterialTheme.typography.labelSmall,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(sectionSpacer))

                        if (qrContent.isNotEmpty() && qrError == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
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
                            }
                        }

                        if (qrContent.isNotEmpty() && qrError == null && qrWarning != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = warningTextForCode(qrWarning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        if (downloadKeysState is DownloadKeysState.Loading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Загрузка ключей...", style = MaterialTheme.typography.labelMedium)
                        }

                        if (usingCachedKeys && qrContent.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isOfflineMode) "Используются сохранённые ключи"
                                else "Ключи не обновились, используется сохранённая версия.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        if (downloadKeysState is DownloadKeysState.Error && !isOfflineMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val state = downloadKeysState as DownloadKeysState.Error
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }

                        if (timeLeft == 0 && qrContent.isEmpty() && qrError == null && !showOfflineSuggestion && !isBootstrapping) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = {
                                    if (qrError == "ERROR_COUPON_USED" || qrError == "ERROR_COUPON_UNAVAILABLE") {
                                        bootstrapTrigger++
                                    } else {
                                        refreshTrigger++
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                shape = PillShape
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Обновить сейчас")
                            }
                        }

                        // Manual offline toggle when QR is visible and online
                        if (!isOfflineMode && qrContent.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        isOfflineMode = true
                                        bootstrapTrigger++
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Перейти в оффлайн",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }

                        // Switch back to online from offline
                        if (isOfflineMode && qrContent.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        isOfflineMode = false
                                        bootstrapTrigger++
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Вернуться в онлайн",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = PillShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(infoWidthFraction)
                .widthIn(max = 560.dp)
                .padding(bottom = verticalPadding)
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
private fun OfflineSuggestionContent(
    onEnableOffline: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            Icons.Rounded.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Сервер не отвечает",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Включить оффлайн-режим?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onEnableOffline,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Icon(
                Icons.Rounded.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Оффлайн", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(
            onClick = onRetry,
            shape = PillShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("Повторить", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun QrErrorContentShared(
    qrError: String,
    retryAttempted: Boolean,
    signatureDebugInfo: String,
    onDownloadKeys: () -> Unit,
    onUseCachedKeys: () -> Unit,
    onRefresh: () -> Unit,
) {
    val signatureTail = signatureDebugInfo.substringAfter('|', "").takeIf { it.isNotBlank() }?.take(72)
    val errorText = when (qrError) {
        "ERROR_KEY" -> "Ключи отсутствуют.\nСкачайте ключи для продолжения."
        "ERROR_KEY_REFRESH_REQUIRED" -> "Не удалось обновить ключи.\nПродолжить с сохранённой версией или повторить загрузку?"
        "ERROR_COUPON_USED" -> "Талон уже использован.\nОбновите список талонов."
        "ERROR_COUPON_UNAVAILABLE" -> "Талон недоступен на сегодня."
        "ERROR_AUTH_REQUIRED" -> "Сессия истекла.\nВойдите заново и обновите экран."
        "ERROR_OFFLINE_DATA_MISSING" -> "Нет оффлайн-данных талона.\nПодключитесь к сети и обновите данные."
        "ERROR_PERMISSION_UNKNOWN" -> "Не удалось определить статус талона.\nОбновите данные."
        else -> "Не удалось подписать QR.\nОбновите ключи."
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorText,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        if (qrError == "ERROR_SIG" && retryAttempted) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Повтор уже выполнен после обновления ключей.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (qrError == "ERROR_SIG" && signatureTail != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = signatureTail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (qrError == "ERROR_KEY") {
            TextButton(onClick = onDownloadKeys) {
                Text("Скачать ключи")
            }
        } else if (qrError == "ERROR_KEY_REFRESH_REQUIRED") {
            TextButton(onClick = onUseCachedKeys) {
                Text("Использовать сохранённые")
            }
            TextButton(onClick = onDownloadKeys) {
                Text("Обновить ключи")
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
    else -> mealType.uppercase()
}

private fun shouldRebootstrapForError(error: String?): Boolean = when (error) {
    "ERROR_COUPON_USED",
    "ERROR_COUPON_UNAVAILABLE",
    "ERROR_AUTH_REQUIRED",
    "ERROR_OFFLINE_DATA_MISSING",
    "ERROR_PERMISSION_UNKNOWN",
    "ERROR_KEY_REFRESH_REQUIRED",
    -> true
    else -> false
}

private fun warningTextForCode(code: String?): String = when (code) {
    "WARN_PERMISSION_UNKNOWN_OFFLINE" ->
        "Статус талона не подтверждён сервером. Используются оффлайн-данные."
    "WARN_PERMISSION_UNKNOWN_ONLINE" ->
        "Статус талона частично подтверждён. QR сформирован в деградированном режиме."
    else -> "Статус талона не подтверждён. Используется безопасный деградированный режим."
}

internal enum class MealStatusSource {
    ONLINE,
    OFFLINE_CACHE,
}

internal data class MealStatusPolicy(
    val errorCode: String? = null,
    val warningCode: String? = null,
)

internal fun resolveMealStatusPolicy(
    status: MealCouponStatus,
    source: MealStatusSource,
): MealStatusPolicy {
    return when (status) {
        MealCouponStatus.USED -> MealStatusPolicy(errorCode = "ERROR_COUPON_USED")
        MealCouponStatus.UNAVAILABLE -> MealStatusPolicy(errorCode = "ERROR_COUPON_UNAVAILABLE")
        MealCouponStatus.AVAILABLE -> MealStatusPolicy()
        MealCouponStatus.UNKNOWN -> {
            val warningCode = when (source) {
                MealStatusSource.OFFLINE_CACHE -> "WARN_PERMISSION_UNKNOWN_OFFLINE"
                MealStatusSource.ONLINE -> "WARN_PERMISSION_UNKNOWN_ONLINE"
            }
            MealStatusPolicy(warningCode = warningCode)
        }
    }
}

internal fun Throwable?.isAuthFailure(): Boolean {
    val api = this as? ApiCallException ?: return false
    val status = api.apiError.httpStatus
    val code = api.apiError.code
    return status == 401 || status == 403 || code == "HTTP_401" || code == "HTTP_403" || code == "ACCESS_DENIED"
}

internal enum class MealCouponStatus {
    AVAILABLE,
    USED,
    UNAVAILABLE,
    UNKNOWN,
}

internal fun MealsTodayResponse.statusForMealType(mealType: String): MealCouponStatus {
    val normalized = mealType.uppercase()
    val allowed = when (normalized) {
        "BREAKFAST" -> isBreakfastAllowed
        "LUNCH" -> isLunchAllowed
        else -> false
    }
    val consumed = when (normalized) {
        "BREAKFAST" -> isBreakfastConsumed
        "LUNCH" -> isLunchConsumed
        else -> null
    }
    return when {
        consumed == true -> MealCouponStatus.USED
        !allowed -> MealCouponStatus.UNAVAILABLE
        consumed == false -> MealCouponStatus.AVAILABLE
        else -> MealCouponStatus.UNKNOWN
    }
}
