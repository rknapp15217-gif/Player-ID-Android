package com.playerid.app.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.playerid.app.domain.auth.AccountException
import com.playerid.app.domain.auth.EmailAuth
import com.playerid.app.domain.auth.SecureSessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidAccount(context: Context) {
    private val client = configureAccountClient(HttpClient(OkHttp))
    val auth = EmailAuth(SupabaseEmailAuthService(client), AndroidSecureSessionStorage(context.applicationContext))
}

private class AndroidSecureSessionStorage(context: Context) : SecureSessionStorage {
    private val path = File(context.noBackupFilesDir, "playloom-account-v1")
    private val file = AtomicFile(path)
    private val alias = "playloom-account-v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(alias)) return store.getKey(alias, null) as? SecretKey
            ?: throw AccountException("The secure sign-in key is unavailable. Sign out on this device and retry.")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    private suspend fun <T> secureOperation(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (failure: GeneralSecurityException) {
            throw AccountException("Secure sign-in storage is unavailable. Sign out on this device and try again.", failure)
        } catch (failure: IOException) {
            throw AccountException("Cannot read or save secure sign-in data. Check device storage and retry.", failure)
        }
    }

    override suspend fun read(): String? = secureOperation {
        if (!path.exists() && !File("${path.path}.bak").exists()) return@secureOperation null
        val bytes = file.readFully()
        if (bytes.size < 29 || bytes[0] != 1.toByte()) throw AccountException("Saved sign-in data is invalid. Sign out on this device and retry.")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8)
    }

    override suspend fun write(value: String) = secureOperation {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = byteArrayOf(1) + cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (failure: IOException) {
            file.failWrite(output)
            throw failure
        }
    }

    override suspend fun clear() = secureOperation {
        file.delete()
        if (path.exists() || File("${path.path}.bak").exists() || File("${path.path}.new").exists()) {
            throw AccountException("Cannot clear the saved sign-in. Check device storage and retry.")
        }
    }
}
