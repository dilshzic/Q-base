package com.algorithmx.q_base.core_crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.hybrid.HybridKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class CryptoManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSET_NAME = "qbase_e2ee_keyset"
        private const val PREF_FILE_NAME = "qbase_crypto_prefs"
        private const val MASTER_KEY_URI = "android-keystore://qbase_master_key"
    }

    init {
        AeadConfig.register()
        HybridConfig.register()
    }

    private var privateKeysetHandle: KeysetHandle? = null

    fun initializeAndGetPublicKey(): String {
        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()

        val handle = keysetManager.keysetHandle
        privateKeysetHandle = handle

        val publicHandle = handle.publicKeysetHandle
        val outputStream = java.io.ByteArrayOutputStream()
        com.google.crypto.tink.CleartextKeysetHandle.write(
            publicHandle,
            com.google.crypto.tink.BinaryKeysetWriter.withOutputStream(outputStream)
        )

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun encryptMessage(plaintext: String, receiverPublicKeyBase64: String): String {
        val publicBytes = Base64.decode(receiverPublicKeyBase64, Base64.NO_WRAP)
        val publicHandle = com.google.crypto.tink.CleartextKeysetHandle.read(
            com.google.crypto.tink.BinaryKeysetReader.withBytes(publicBytes)
        )

        val hybridEncrypt = publicHandle.getPrimitive(HybridEncrypt::class.java)
        val ciphertext = hybridEncrypt.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)

        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    fun encryptSessionKey(sessionKey: ByteArray, receiverPublicKeyBase64: String): String {
        val publicBytes = Base64.decode(receiverPublicKeyBase64, Base64.NO_WRAP)
        val publicHandle = com.google.crypto.tink.CleartextKeysetHandle.read(
            com.google.crypto.tink.BinaryKeysetReader.withBytes(publicBytes)
        )

        val hybridEncrypt = publicHandle.getPrimitive(HybridEncrypt::class.java)
        val ciphertext = hybridEncrypt.encrypt(sessionKey, null)

        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    fun decryptMessage(ciphertextBase64: String): Result<String> {
        return try {
            val plaintextBytes = decryptRaw(ciphertextBase64)
            Result.success(String(plaintextBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun decryptSessionKey(wrappedKeyBase64: String): Result<ByteArray> {
        return try {
            Result.success(decryptRaw(wrappedKeyBase64))
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to decrypt session key", e)
            Result.failure(e)
        }
    }

    private fun decryptRaw(ciphertextBase64: String): ByteArray {
        if (privateKeysetHandle == null) {
            val keysetManager = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
            privateKeysetHandle = keysetManager.keysetHandle
        }

        val hybridDecrypt = privateKeysetHandle?.getPrimitive(HybridDecrypt::class.java)
            ?: throw IllegalStateException("Privacy keyset not available")

        try {
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            return hybridDecrypt.decrypt(cipherBytes, null)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Hybrid decryption failed", e)
            throw e
        }
    }

    fun clearKeys() {
        privateKeysetHandle = null
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun getPublicKeyFingerprint(): String {
        return try {
            val pubKey = initializeAndGetPublicKey()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pubKey.toByteArray(Charsets.UTF_8))
            val shortHash = if (hash.size >= 8) hash.copyOfRange(0, 8) else hash
            Base64.encodeToString(shortHash, Base64.NO_WRAP)
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun encryptFileContent(plaintext: ByteArray): Pair<ByteArray, String> {
        val handle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        val aead = handle.getPrimitive(Aead::class.java)

        val ciphertext = aead.encrypt(plaintext, null)

        val keyOut = java.io.ByteArrayOutputStream()
        com.google.crypto.tink.CleartextKeysetHandle.write(
            handle,
            com.google.crypto.tink.BinaryKeysetWriter.withOutputStream(keyOut)
        )
        val keyBase64 = Base64.encodeToString(keyOut.toByteArray(), Base64.NO_WRAP)

        return Pair(ciphertext, keyBase64)
    }

    fun encryptWithSessionKey(plaintext: String): Pair<String, String> {
        val handle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        val aead = handle.getPrimitive(Aead::class.java)

        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)

        val keyOut = java.io.ByteArrayOutputStream()
        com.google.crypto.tink.CleartextKeysetHandle.write(
            handle,
            com.google.crypto.tink.BinaryKeysetWriter.withOutputStream(keyOut)
        )
        val keyBase64 = Base64.encodeToString(keyOut.toByteArray(), Base64.NO_WRAP)

        return Pair(Base64.encodeToString(ciphertext, Base64.NO_WRAP), keyBase64)
    }

    fun decryptWithSessionKey(ciphertextBase64: String, keyHandleBase64: String): Result<String> {
        return try {
            val keyBytes = Base64.decode(keyHandleBase64, Base64.NO_WRAP)
            val handle = com.google.crypto.tink.CleartextKeysetHandle.read(
                com.google.crypto.tink.BinaryKeysetReader.withBytes(keyBytes)
            )
            val aead = handle.getPrimitive(Aead::class.java)
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val plaintext = aead.decrypt(cipherBytes, null)
            Result.success(String(plaintext, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to decrypt with session key", e)
            Result.failure(e)
        }
    }

    fun decryptFileContent(ciphertext: ByteArray, keyBase64: String): ByteArray {
        return try {
            if (keyBase64.isEmpty()) return ciphertext

            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val handle = com.google.crypto.tink.CleartextKeysetHandle.read(
                com.google.crypto.tink.BinaryKeysetReader.withBytes(keyBytes)
            )
            val aead = handle.getPrimitive(Aead::class.java)
            aead.decrypt(ciphertext, null)
        } catch (e: Exception) {
            e.printStackTrace()
            ciphertext
        }
    }

    fun exportEncryptedKeyset(passphrase: String): String {
        val handle = privateKeysetHandle ?: throw IllegalStateException("Keyset not initialized")
        val outputStream = java.io.ByteArrayOutputStream()

        com.google.crypto.tink.CleartextKeysetHandle.write(
            handle,
            com.google.crypto.tink.BinaryKeysetWriter.withOutputStream(outputStream)
        )
        val keysetBytes = outputStream.toByteArray()

        return encryptBackup(keysetBytes, passphrase)
    }

    fun importEncryptedKeyset(encryptedBackupBase64: String, passphrase: String): Result<Unit> {
        return try {
            val keysetBytes = decryptBackup(encryptedBackupBase64, passphrase)
            val handle = com.google.crypto.tink.CleartextKeysetHandle.read(
                com.google.crypto.tink.BinaryKeysetReader.withBytes(keysetBytes)
            )

            clearKeys()

            val masterKey = AndroidKeystoreKmsClient.getOrGenerateNewAeadKey(MASTER_KEY_URI)
            handle.write(
                com.google.crypto.tink.integration.android.SharedPrefKeysetWriter(context, KEYSET_NAME, PREF_FILE_NAME),
                masterKey
            )

            privateKeysetHandle = handle
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to import keyset", e)
            Result.failure(e)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun encryptBackup(data: ByteArray, passphrase: String): String {
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)

        val keyBytes = deriveKey(passphrase, salt)
        val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)

        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertext = cipher.doFinal(data)

        val output = java.io.ByteArrayOutputStream()
        output.write(salt)
        output.write(iv)
        output.write(ciphertext)

        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun decryptBackup(encryptedBase64: String, passphrase: String): ByteArray {
        val input = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        val salt = input.copyOfRange(0, 16)
        val iv = input.copyOfRange(16, 28)
        val ciphertext = input.copyOfRange(28, input.size)

        val keyBytes = deriveKey(passphrase, salt)
        val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)

        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        return cipher.doFinal(ciphertext)
    }
}
