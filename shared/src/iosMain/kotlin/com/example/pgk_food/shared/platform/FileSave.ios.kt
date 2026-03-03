package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject

@Composable
actual fun rememberFileSaveLauncher(
    onResult: (success: Boolean, message: String?) -> Unit,
): (FileSaveRequest) -> Unit {
    val latestCallback = rememberUpdatedState(onResult)
    val delegate = remember {
        FileSavePickerDelegate { success, message ->
            latestCallback.value(success, message)
        }
    }

    DisposableEffect(Unit) {
        onDispose { delegate.clear() }
    }

    return remember(delegate) {
        { request ->
            val sourceUrl = createTempExportFile(request)
            if (sourceUrl == null) {
                latestCallback.value(false, "Не удалось подготовить файл")
            } else {
                val picker = UIDocumentPickerViewController(
                    forExportingURLs = listOf(sourceUrl),
                    asCopy = true
                )
                picker.delegate = delegate
                delegate.activePicker = picker

                val presenter = resolvePresenterViewController()
                if (presenter == null) {
                    latestCallback.value(false, "Не удалось открыть системный диалог")
                } else {
                    presenter.presentViewController(picker, animated = true, completion = null)
                }
            }
        }
    }
}

private class FileSavePickerDelegate(
    private val onResult: (Boolean, String?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol, UINavigationControllerDelegateProtocol {
    var activePicker: UIDocumentPickerViewController? = null

    fun clear() {
        activePicker = null
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onResult(true, null)
        activePicker = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(false, "Сохранение отменено")
        activePicker = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createTempExportFile(request: FileSaveRequest): NSURL? {
    val tempDirPath = NSTemporaryDirectory()
    if (tempDirPath.isBlank()) return null
    val tempDir = NSURL.fileURLWithPath(tempDirPath)
    val safeName = request.fileName.ifBlank { "export.bin" }
    val uniqueName = "${NSUUID().UUIDString}_$safeName"
    val url = tempDir.URLByAppendingPathComponent(uniqueName) ?: return null
    val data = request.bytes.toNSData() ?: return null
    return if (data.writeToURL(url, atomically = true)) url else null
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

private fun resolvePresenterViewController(): UIViewController? {
    val root = (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.rootViewController ?: return null
    return root.topPresented()
}

private fun UIViewController.topPresented(): UIViewController {
    val presented = presentedViewController
    return if (presented != null) presented.topPresented() else this
}
