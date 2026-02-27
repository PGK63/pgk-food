package com.example.pgk_food.shared.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCsvImportLauncher(
    onFileBytes: (ByteArray?) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val bytes = uri?.let { selected ->
            context.contentResolver.openInputStream(selected)?.use { it.readBytes() }
        }
        onFileBytes(bytes)
    }
    return { launcher.launch(arrayOf("text/*")) }
}
