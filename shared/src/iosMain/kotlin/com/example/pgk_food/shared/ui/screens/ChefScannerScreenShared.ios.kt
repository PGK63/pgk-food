@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.pgk_food.shared.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import com.example.pgk_food.shared.data.remote.dto.QrValidationResponse
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import platform.AVFoundation.*
import platform.QuartzCore.CALayer
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun ChefScannerScreenShared(token: String, chefRepository: ChefRepository) {
    var result by remember { mutableStateOf<QrValidationResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var scanResetKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> hasCameraPermission = true
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) { hasCameraPermission = granted }
                }
            }
            else -> hasCameraPermission = false
        }
    }

    ScannerScreenLayoutIos(
        hasCameraPermission = hasCameraPermission,
        result = result,
        isLoading = isLoading,
        scannerError = scannerError,
        torchEnabled = torchEnabled,
        isOfflineMode = isOfflineMode,
        onToggleTorch = { torchEnabled = !torchEnabled },
        onToggleOfflineMode = { isOfflineMode = !isOfflineMode },
        onRequestPermission = {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) { hasCameraPermission = granted }
            }
        },
        onReset = {
            result = null
            scannerError = null
            isLoading = false
            scanResetKey++
        },
        scannerContent = {
            val isScannerActive = hasCameraPermission && result == null && !isLoading
            IOSCameraPreview(
                isActive = isScannerActive,
                torchEnabled = torchEnabled,
                resetKey = scanResetKey,
                onQrScanned = { qrContent ->
                    if (!isLoading) {
                        scannerError = null
                        isLoading = true
                        scope.launch {
                            val res = chefRepository.validateQr(token, qrContent, isOfflineMode)
                            result = res.getOrNull()
                            if (result == null) {
                                scannerError = res.exceptionOrNull()?.message ?: "Ошибка проверки QR"
                            }
                            isLoading = false
                        }
                    }
                },
                onError = { scannerError = it }
            )
            ScannerOverlayIos(isActive = isScannerActive)
        }
    )
}

@Composable
private fun IOSCameraPreview(
    isActive: Boolean,
    torchEnabled: Boolean,
    resetKey: Int,
    onQrScanned: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val onQrScannedState by rememberUpdatedState(onQrScanned)
    val onErrorState by rememberUpdatedState(onError)
    val scanner = remember { IOSQrScannerController() }

    LaunchedEffect(resetKey) { scanner.resetDebounce() }
    LaunchedEffect(torchEnabled) { scanner.setTorchEnabled(torchEnabled) }
    LaunchedEffect(isActive) {
        if (isActive) scanner.start(onQrScannedState, onErrorState) else scanner.stop()
    }

    DisposableEffect(Unit) {
        onDispose { scanner.dispose() }
    }

    UIKitView(
        factory = {
            scanner.attachView(it = UIView().apply { backgroundColor = UIColor.blackColor })
        },
        update = { view ->
            scanner.attachView(view)
            scanner.setCallbacks(onQrScannedState, onErrorState)
            scanner.setActive(isActive)
            scanner.setTorchEnabled(torchEnabled)
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class IOSQrScannerController {
    private val session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var view: UIView? = null
    private var metadataOutput: AVCaptureMetadataOutput? = null
    private var videoDevice: AVCaptureDevice? = null
    private var delegate: QrMetadataDelegate? = null
    private var isConfigured = false
    private var isActive = false
    private var lastScannedAtMillis: Long = 0L
    private var onQrScanned: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun attachView(it: UIView): UIView {
        view = it
        ensurePreviewLayer()
        return it
    }

    fun setCallbacks(onQrScanned: (String) -> Unit, onError: (String) -> Unit) {
        this.onQrScanned = onQrScanned
        this.onError = onError
        delegate?.onQrScanned = onQrScanned
        delegate?.onError = onError
    }

    fun setActive(active: Boolean) {
        isActive = active
        if (active) startInternal() else stop()
    }

    fun start(onQrScanned: (String) -> Unit, onError: (String) -> Unit) {
        setCallbacks(onQrScanned, onError)
        setActive(true)
    }

    fun stop() {
        isActive = false
        if (session.running) session.stopRunning()
    }

    fun dispose() {
        stop()
        previewLayer?.removeFromSuperlayer()
        previewLayer = null
        view = null
    }

    fun resetDebounce() {
        lastScannedAtMillis = 0L
        delegate?.lastScannedAtMillis = 0L
    }

    fun setTorchEnabled(enabled: Boolean) {
        val device = videoDevice ?: return
        if (!device.hasTorch()) return
        if (device.lockForConfiguration(null)) {
            device.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            device.unlockForConfiguration()
        }
    }

    private fun ensurePreviewLayer() {
        val targetView = view ?: return
        if (previewLayer == null) {
            previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
                videoGravity = AVLayerVideoGravityResizeAspectFill
            }
            targetView.layer.addSublayer(previewLayer as CALayer)
        }
        previewLayer?.frame = targetView.bounds
        if (!isConfigured) configureSession()
    }

    private fun configureSession() {
        if (isConfigured) return
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) {
            onError?.invoke("Камера недоступна")
            return
        }
        videoDevice = device
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) as? AVCaptureDeviceInput
        if (input == null || !session.canAddInput(input)) {
            onError?.invoke("Не удалось подключить камеру")
            return
        }
        session.addInput(input)

        val output = AVCaptureMetadataOutput()
        if (!session.canAddOutput(output)) {
            onError?.invoke("Не удалось запустить сканер")
            return
        }
        session.addOutput(output)
        metadataOutput = output

        val qrDelegate = QrMetadataDelegate(
            onQrScanned = { code -> onQrScanned?.invoke(code) },
            onError = { msg -> onError?.invoke(msg) },
            isActive = { isActive },
            getLastScannedAtMillis = { lastScannedAtMillis },
            setLastScannedAtMillis = { lastScannedAtMillis = it }
        )
        delegate = qrDelegate
        output.setMetadataObjectsDelegate(qrDelegate, dispatch_get_main_queue())
        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        isConfigured = true
        if (isActive) startInternal()
    }

    private fun startInternal() {
        ensurePreviewLayer()
        if (!isConfigured) return
        if (!session.running) session.startRunning()
    }
}

private class QrMetadataDelegate(
    var onQrScanned: (String) -> Unit,
    var onError: (String) -> Unit,
    private val isActive: () -> Boolean,
    private val getLastScannedAtMillis: () -> Long,
    private val setLastScannedAtMillis: (Long) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    var lastScannedAtMillis: Long = 0L

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        if (!isActive()) return
        for (obj in didOutputMetadataObjects) {
            val codeObject = obj as? AVMetadataMachineReadableCodeObject ?: continue
            val value = codeObject.stringValue ?: continue
            val now = Clock.System.now().toEpochMilliseconds()
            val last = getLastScannedAtMillis()
            if (now - last > 2000) {
                setLastScannedAtMillis(now)
                onQrScanned(value)
            }
            return
        }
    }
}

@Composable
private fun ScannerScreenLayoutIos(
    hasCameraPermission: Boolean,
    result: QrValidationResponse?,
    isLoading: Boolean,
    scannerError: String?,
    torchEnabled: Boolean,
    isOfflineMode: Boolean,
    onToggleTorch: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onRequestPermission: () -> Unit,
    onReset: () -> Unit,
    scannerContent: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "РЕЖИМ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isOfflineMode) "ОФФЛАЙН (АВТО)" else "ОНЛАЙН (СЕРВЕР)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOfflineMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (hasCameraPermission && result == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Оффлайн", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isOfflineMode, onCheckedChange = { onToggleOfflineMode() })
                    IconButton(onClick = onToggleTorch) {
                        Icon(if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = null)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (hasCameraPermission && result == null) {
            HowItWorksCard(
                steps = listOf(
                    "Сканируйте QR студента и дождитесь карточки результата.",
                    "При отказе смотрите причину в тексте ошибки перед повторной проверкой.",
                    "При нестабильной сети возможен оффлайн-режим с отложенной синхронизацией.",
                    "Сверяйте тип питания и группу в карточке перед выдачей."
                ),
                note = "Онлайн-результат приоритетнее оффлайн-проверки."
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(20.dp))
        }

        if (hasCameraPermission && result == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black),
                contentAlignment = Alignment.Center,
                content = scannerContent
            )
        } else if (!hasCameraPermission) {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Нужно разрешение на камеру", modifier = Modifier.padding(16.dp))
                    Button(onClick = onRequestPermission) { Text("Разрешить") }
                }
            }
        }

        if (result != null) Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Проверка...")
        }

        scannerError?.let { error ->
            if (!isLoading && result == null) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Ошибка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        result?.let {
            val isValid = it.isValid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isValid) "ОТМЕЧЕНО" else "ОТКАЗАНО",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        it.studentName?.let { name -> Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
                        it.groupName?.let { group -> Text("Группа: $group", style = MaterialTheme.typography.bodySmall) }
                        it.mealType?.let { meal -> Text("Питание: $meal", style = MaterialTheme.typography.bodySmall) }
                        it.errorMessage?.let { msg ->
                            Text(
                                msg,
                                color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
            }
        }

        if (hasCameraPermission && result == null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Наведите камеру на QR-код студента",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScannerOverlayIos(isActive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(220.dp).border(
                width = 2.dp,
                color = if (isActive) Color(0xFF00E676) else Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        )
        if (isActive) ScannerLineIos()
    }
}

@Composable
private fun ScannerLineIos() {
    val transition = rememberInfiniteTransition(label = "scanner-line")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "scanner-line-offset"
    )
    BoxWithConstraints(modifier = Modifier.size(220.dp)) {
        val y = maxHeight * offset
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = y).background(Color(0xFF00E676)))
    }
}
