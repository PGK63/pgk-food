package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCsvImportLauncher(
    onFileBytes: (ByteArray?) -> Unit,
): () -> Unit {
    val latestCallback = rememberUpdatedState(onFileBytes)
    val delegate = remember {
        CsvDocumentPickerDelegate { bytes ->
            latestCallback.value(bytes)
        }
    }

    DisposableEffect(Unit) {
        onDispose { delegate.clear() }
    }

    return remember(delegate) {
        {
            val picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.comma-separated-values-text", "public.text"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
            )
            picker.delegate = delegate
            delegate.activePicker = picker

            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (root == null) {
                latestCallback.value(null)
            } else {
                root.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CsvDocumentPickerDelegate(
    private val onPicked: (ByteArray?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol, UINavigationControllerDelegateProtocol {
    var activePicker: UIDocumentPickerViewController? = null

    fun clear() {
        activePicker = null
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onPicked(readUrlBytes(url))
        activePicker = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(null)
        activePicker = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readUrlBytes(url: NSURL?): ByteArray? {
    if (url == null) return null
    val path = url.path ?: return null
    val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
    val bytes = data.bytes ?: return null
    return bytes.reinterpret<ByteVar>().readBytes(data.length.toInt())
}
