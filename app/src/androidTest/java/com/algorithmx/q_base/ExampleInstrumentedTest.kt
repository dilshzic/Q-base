package com.algorithmx.q_base

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testCrypto() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val cryptoManager = com.algorithmx.q_base.core_crypto.CryptoManager(appContext)
        
        android.util.Log.d("CryptoTest", "--- STARTING CRYPTO TEST ---")
        try {
            android.util.Log.d("CryptoTest", "Generating/Retrieving public key...")
            val pubKey = cryptoManager.initializeAndGetPublicKey()
            android.util.Log.d("CryptoTest", "Public key generated successfully: $pubKey")
        } catch (e: Throwable) {
            android.util.Log.e("CryptoTest", "initializeAndGetPublicKey failed!", e)
            e.printStackTrace()
            throw e
        }

        try {
            android.util.Log.d("CryptoTest", "Encrypting with session key...")
            val (cipher, handle) = cryptoManager.encryptWithSessionKey("test payload")
            android.util.Log.d("CryptoTest", "Encrypted successfully. Cipher length: ${cipher.length}")
        } catch (e: Throwable) {
            android.util.Log.e("CryptoTest", "encryptWithSessionKey failed!", e)
            e.printStackTrace()
            throw e
        }
        android.util.Log.d("CryptoTest", "--- CRYPTO TEST COMPLETED ---")
    }
}