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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.theme.HeroCardShape
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.viewmodels.ChefViewModel
import com.example.pgk_food.shared.ui.viewmodels.ScanState
import com.example.pgk_food.shared.ui.viewmodels.SyncState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.QuartzCore.CALayer
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ChefScannerScreenShared(
    token: String,
    viewModel: ChefViewModel,
    showHints: Boolean,
    onHideHints: () -> Unit,
) {
    val scanState by viewModel.scanState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isOfflineMode by viewModel.isOffline.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var scanResetKey by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> hasCameraPermission = true
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        hasCameraPermission = granted
                    }
                }
            }

            else -> hasCameraPermission = false
        }
    }

    LaunchedEffect(token) {
        viewModel.updateUnsyncedCount()
    }

    LaunchedEffect(syncState) {
        when (val state = syncState) {
            is SyncState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetSyncState()
            }

            is SyncState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetSyncState()
            }

            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Сканер питания", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    if (unsyncedCount > 0) {
                        BadgedBox(
                            badge = { Badge { Text(unsyncedCount.toString()) } },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = { viewModel.syncTransactions() }) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Синхронизировать")
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.downloadData() }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Скачать данные")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isCompactHeight = maxHeight < 760.dp
            val horizontalPadding = if (isCompactHeight) 16.dp else 24.dp
            val verticalPadding = if (isCompactHeight) 12.dp else 24.dp
            val cameraHeightCandidate = if (isCompactHeight) maxHeight * 0.44f else maxHeight * 0.5f
            val cameraHeight = when {
                cameraHeightCandidate < 280.dp -> 280.dp
                cameraHeightCandidate > 430.dp -> 430.dp
                else -> cameraHeightCandidate
            }
            val frameByHeight = cameraHeight - if (isCompactHeight) 22.dp else 28.dp
            val frameByWidth = maxWidth - if (isCompactHeight) 56.dp else 72.dp
            val frameSizeCandidate = if (frameByHeight < frameByWidth) frameByHeight else frameByWidth
            val scannerFrameSize = when {
                frameSizeCandidate < 244.dp -> 244.dp
                frameSizeCandidate > 372.dp -> 372.dp
                else -> frameSizeCandidate
            }
            val hintsTopSpacer = if (isCompactHeight) 8.dp else 12.dp
            val hintsBottomSpacer = if (isCompactHeight) 16.dp else 24.dp
            val loadingTopSpacer = if (isCompactHeight) 24.dp else 40.dp
            val resultTopSpacer = if (isCompactHeight) 16.dp else 24.dp
            val controlsSpacer = if (isCompactHeight) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Icon(
                        if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null
                    )
                }
            }

            if (showHints) {
                Spacer(modifier = Modifier.height(hintsTopSpacer))
                HowItWorksCard(
                    steps = listOf(
                        "Сканируйте QR студента и дождитесь карточки результата.",
                        "Если есть отказ, исправьте причину и повторите проверку.",
                        "При потере сети сканер автоматически уходит в оффлайн с последующей синхронизацией.",
                        "Сверяйте тип питания и группу в карточке перед выдачей."
                    ),
                    note = "Онлайн-результат приоритетнее оффлайн-проверки.",
                    onHideHints = onHideHints,
                )
                Spacer(modifier = Modifier.height(hintsBottomSpacer))
            } else {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 20.dp))
            }

            val isScannerActive =
                hasCameraPermission && scanState is ScanState.Idle && syncState is SyncState.Idle

            if (hasCameraPermission && scanState is ScanState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cameraHeight)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    IOSCameraPreview(
                        isActive = isScannerActive,
                        torchEnabled = torchEnabled,
                        resetKey = scanResetKey,
                        onQrScanned = { qrContent ->
                            scannerError = null
                            viewModel.scanQr(qrContent)
                        },
                        onError = { scannerError = it }
                    )
                    ScannerOverlayIos(
                        isActive = isScannerActive,
                        frameSize = scannerFrameSize,
                    )
                }
            } else if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cameraHeight)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Нужно разрешение на камеру", modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = {
                                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                                    dispatch_async(dispatch_get_main_queue()) {
                                        hasCameraPermission = granted
                                    }
                                }
                            },
                            shape = PillShape,
                        ) {
                            Text("Разрешить")
                        }
                    }
                }
            }

            if (scanState is ScanState.Loading || syncState is SyncState.Loading) {
                Spacer(modifier = Modifier.height(loadingTopSpacer))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(controlsSpacer))
                Text(if (syncState is SyncState.Loading) (syncState as SyncState.Loading).message else "Проверка...")
            }

            if (scanState is ScanState.Error) {
                Spacer(modifier = Modifier.height(resultTopSpacer))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ошибка", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text((scanState as ScanState.Error).message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(controlsSpacer))
                Button(onClick = { viewModel.resetScanState() }, modifier = Modifier.fillMaxWidth(), shape = PillShape) {
                    Text("ПОПРОБОВАТЬ СНОВА")
                }
            }

            scannerError?.let { cameraError ->
                if (scanState is ScanState.Idle && syncState !is SyncState.Loading) {
                    Spacer(modifier = Modifier.height(controlsSpacer))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cameraError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (scanState is ScanState.Success) {
                val scanResponse = (scanState as ScanState.Success).response
                val isValid = scanResponse.isValid
                Spacer(modifier = Modifier.height(resultTopSpacer))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isValid)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = HeroCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isValid)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                if (isValid) "ОТМЕЧЕНО" else "ОТКАЗАНО",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (scanResponse.studentName != null) {
                                Text(scanResponse.studentName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            if (scanResponse.groupName != null) {
                                Text("Группа: ${scanResponse.groupName}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (scanResponse.errorMessage != null) {
                                Text(scanResponse.errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(controlsSpacer))

                Button(
                    onClick = {
                        viewModel.resetScanState()
                        scanResetKey++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
                }
            }

            if (hasCameraPermission && scanState is ScanState.Idle) {
                Spacer(modifier = Modifier.height(controlsSpacer))
                Text(
                    text = "Наведите камеру на QR-код студента",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        }
    }
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

    LaunchedEffect(resetKey) {
        scanner.resetDebounce()
    }
    LaunchedEffect(torchEnabled) {
        scanner.setTorchEnabled(torchEnabled)
    }
    LaunchedEffect(isActive) {
        if (isActive) {
            scanner.start(onQrScannedState, onErrorState)
        } else {
            scanner.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.dispose()
        }
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
        modifier = Modifier.fillMaxSize(),
    )
}

private class IOSQrScannerController {
    private val session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var view: UIView? = null
    private var delegate: QrMetadataDelegate? = null
    private var videoDevice: AVCaptureDevice? = null
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
        if (active) {
            startInternal()
        } else {
            stop()
        }
    }

    fun start(onQrScanned: (String) -> Unit, onError: (String) -> Unit) {
        setCallbacks(onQrScanned, onError)
        setActive(true)
    }

    fun stop() {
        isActive = false
        if (session.running) {
            session.stopRunning()
        }
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

        val qrDelegate = QrMetadataDelegate(
            onQrScanned = { code -> onQrScanned?.invoke(code) },
            onError = { msg -> onError?.invoke(msg) },
            isActive = { isActive },
            getLastScannedAtMillis = { lastScannedAtMillis },
            setLastScannedAtMillis = { lastScannedAtMillis = it },
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
        if (!session.running) {
            session.startRunning()
        }
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

            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
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
private fun ScannerOverlayIos(
    isActive: Boolean,
    frameSize: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(frameSize)
                .border(
                    width = 2.dp,
                    color = if (isActive) Color(0xFF00E676) else Color.White,
                    shape = RoundedCornerShape(16.dp),
                )
        )
        if (isActive) {
            ScannerLineIos(frameSize = frameSize)
        }
    }
}

@Composable
private fun ScannerLineIos(frameSize: Dp) {
    val transition = rememberInfiniteTransition(label = "scanner-line")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanner-line-offset",
    )

    BoxWithConstraints(modifier = Modifier.size(frameSize)) {
        val y = maxHeight * offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = y)
                .background(Color(0xFF00E676))
        )
    }
}
