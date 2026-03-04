@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.pgk_food.shared.platform

import cnames.structs.__CFData
import cnames.structs.__CFDictionary
import cnames.structs.__CFString
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SECURE_STORE_PREFIX = "pgk_food_secure:"
private const val SECURE_STORE_SERVICE = "com.example.pgk_food.secure_store"

actual object PlatformSecureStore {
    actual fun contains(key: String): Boolean = readData(prefixed(key)) != null

    actual fun getString(key: String): String? {
        val bytes = readData(prefixed(key)) ?: return null
        return bytes.decodeToString()
    }

    actual fun putString(key: String, value: String) {
        val account = prefixed(key)
        val valueData = value.encodeToByteArray().toCFData() ?: return

        val baseQuery = buildBaseQuery(account) ?: return
        val updateAttrs = buildUpdateAttributes(valueData) ?: return
        val updateStatus = SecItemUpdate(baseQuery, updateAttrs)

        if (updateStatus == errSecSuccess) return
        if (updateStatus != errSecItemNotFound) {
            SecItemDelete(baseQuery)
        }

        val addQuery = buildAddQuery(account, valueData) ?: return
        SecItemAdd(addQuery, null)
    }

    actual fun remove(key: String) {
        val query = buildBaseQuery(prefixed(key)) ?: return
        SecItemDelete(query)
    }

    private fun readData(account: String): ByteArray? {
        val query = buildReadQuery(account) ?: return null
        return memScoped {
            val resultRef = allocPointerTo<CPointed>()
            val status = SecItemCopyMatching(query, resultRef.ptr.reinterpret<CPointerVarOf<CPointer<out CPointed>>>())
            if (status != errSecSuccess) return@memScoped null
            val data = resultRef.value?.reinterpret<__CFData>() ?: return@memScoped null
            data.toByteArray()
        }
    }

    private fun buildBaseQuery(account: String): CPointer<__CFDictionary>? {
        val service = cfString(SECURE_STORE_SERVICE) ?: return null
        val accountRef = cfString(account) ?: return null
        val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
            ?.reinterpret<__CFDictionary>()
            ?: return null
        CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dict, kSecAttrService, service)
        CFDictionaryAddValue(dict, kSecAttrAccount, accountRef)
        return dict
    }

    private fun buildReadQuery(account: String): CPointer<__CFDictionary>? {
        val query = buildBaseQuery(account) ?: return null
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        return query
    }

    private fun buildUpdateAttributes(valueData: CPointer<__CFData>): CPointer<__CFDictionary>? {
        val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
            ?.reinterpret<__CFDictionary>()
            ?: return null
        CFDictionaryAddValue(dict, kSecValueData, valueData)
        CFDictionaryAddValue(dict, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        return dict
    }

    private fun buildAddQuery(account: String, valueData: CPointer<__CFData>): CPointer<__CFDictionary>? {
        val query = buildBaseQuery(account) ?: return null
        CFDictionaryAddValue(query, kSecValueData, valueData)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        return query
    }

    private fun prefixed(key: String): String = "$SECURE_STORE_PREFIX$key"
}

private fun cfString(value: String): CPointer<__CFString>? {
    return CFStringCreateWithCString(kCFAllocatorDefault, value, kCFStringEncodingUTF8)
        ?.reinterpret<__CFString>()
}

private fun ByteArray.toCFData(): CPointer<__CFData>? = usePinned {
    CFDataCreate(
        kCFAllocatorDefault,
        it.addressOf(0).reinterpret(),
        size.toLong()
    )?.reinterpret<__CFData>()
}

private fun CPointer<__CFData>.toByteArray(): ByteArray {
    val len = CFDataGetLength(this).toInt()
    if (len <= 0) return ByteArray(0)
    val src = CFDataGetBytePtr(this) ?: return ByteArray(0)
    return src.reinterpret<ByteVar>().readBytes(len)
}
