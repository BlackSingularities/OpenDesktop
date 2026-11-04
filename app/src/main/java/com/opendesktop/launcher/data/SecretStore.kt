package com.opendesktop.launcher.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Klucze API zaszyfrowane kluczem AES z Android Keystore (klucz nie opuszcza urządzenia i nie da się
 * go wyciągnąć z pamięci aplikacji). Plik [file] trzyma tylko szyfrogram: `base64(iv).base64(dane)`.
 * Bez klucza Keystore (np. po przywróceniu na innym telefonie) plik jest bezużyteczny – wtedy
 * zwracamy pustą mapę, a użytkownik wpisuje klucze od nowa.
 */
internal class SecretStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mapSer = MapSerializer(String.serializer(), String.serializer())

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun read(): Map<String, String> = runCatching {
        if (!file.exists()) return emptyMap()
        val (iv, data) = file.readText().trim().split('.', limit = 2).map { Base64.decode(it, Base64.NO_WRAP) }
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        json.decodeFromString(mapSer, String(c.doFinal(data), Charsets.UTF_8))
    }.getOrDefault(emptyMap())

    fun write(secrets: Map<String, String>) {
        if (secrets.isEmpty()) {
            file.delete()
            return
        }
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.ENCRYPT_MODE, key())
        val data = c.doFinal(json.encodeToString(mapSer, secrets).toByteArray(Charsets.UTF_8))
        val text = Base64.encodeToString(c.iv, Base64.NO_WRAP) + "." + Base64.encodeToString(data, Base64.NO_WRAP)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "opendesktop_api_keys"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
