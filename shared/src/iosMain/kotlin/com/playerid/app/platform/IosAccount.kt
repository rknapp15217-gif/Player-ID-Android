@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.auth.AccountException
import com.playerid.app.domain.auth.EmailAuth
import com.playerid.app.domain.auth.SecureSessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

class IosAccount {
    private val client = configureAccountClient(HttpClient(Darwin))
    val auth = EmailAuth(SupabaseEmailAuthService(client), IosSecureSessionStorage())
}

private class IosSecureSessionStorage : SecureSessionStorage {
    private fun query(): CFMutableDictionaryRef {
        val result = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
            ?: throw AccountException("Cannot initialize secure sign-in storage.")
        CFDictionarySetValue(result, kSecClass, kSecClassGenericPassword)
        setObject(result, kSecAttrService, "com.playerid.app.playloom.auth")
        setObject(result, kSecAttrAccount, "session-v1")
        return result
    }

    private fun setObject(dictionary: CFMutableDictionaryRef, key: CFStringRef?, value: Any) {
        val retained = CFBridgingRetain(value)
        try {
            CFDictionarySetValue(dictionary, key, retained)
        } finally {
            CFRelease(retained)
        }
    }

    private fun checkStatus(status: Int) {
        if (status != errSecSuccess) throw AccountException("Secure sign-in storage is unavailable (Keychain $status). Unlock your iPhone and retry.")
    }

    override suspend fun read(): String? = memScoped {
        val query = query()
        try {
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            checkStatus(status)
            val data = CFBridgingRelease(result.value) as? NSData
                ?: throw AccountException("Cannot read the saved sign-in. Sign out on this device and retry.")
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                ?: throw AccountException("The saved sign-in has an invalid encoding. Sign out on this device and retry.")
        } finally {
            CFRelease(query)
        }
    }

    override suspend fun write(value: String) {
        val attributes = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
            ?: throw AccountException("Cannot initialize secure sign-in storage.")
        try {
            val query = query()
            try {
                val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
                    ?: throw AccountException("Cannot encode the saved sign-in.")
                setObject(attributes, kSecValueData, data)
                CFDictionarySetValue(attributes, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                val status = SecItemUpdate(query, attributes)
                if (status == errSecItemNotFound) {
                    setObject(query, kSecValueData, data)
                    CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                    checkStatus(SecItemAdd(query, null))
                } else checkStatus(status)
            } finally {
                CFRelease(query)
            }
        } finally {
            CFRelease(attributes)
        }
    }

    override suspend fun clear() {
        val query = query()
        try {
            val status = SecItemDelete(query)
            if (status != errSecItemNotFound) checkStatus(status)
        } finally {
            CFRelease(query)
        }
    }
}
