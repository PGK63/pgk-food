package com.example.pgk_food.shared.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileSaveLauncher(
    onResult: (success: Boolean, message: String?) -> Unit,
): (FileSaveRequest) -> Unit {
    val context = LocalContext.current
    val latestCallback by rememberUpdatedState(onResult)
    var pendingRequest by remember { mutableStateOf<FileSaveRequest?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        val request = pendingRequest
        pendingRequest = null
        if (uri == null || request == null) {
            latestCallback(false, "Сохранение отменено")
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(request.bytes)
            } ?: error("Не удалось открыть файл для записи")
        }.onSuccess {
            latestCallback(true, null)
        }.onFailure {
            latestCallback(false, it.message ?: "Не удалось сохранить файл")
        }
    }

    return remember(launcher) {
        { request ->
            pendingRequest = request
            // MIME type задается контрактом, здесь оставляем единый универсальный контракт.
            launcher.launch(request.fileName)
        }
    }
}
