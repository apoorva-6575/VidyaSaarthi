package com.hackx.ruraledtech.p2p.passport.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PassportCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT = "RuralEdTechSalt_HackX" // In production, generate per-user and pass with IV

    private fun deriveKey(pin: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), SALT.toByteArray(), ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(plainTextJson: String, pin: String): Pair<String, String> {
        val secretKey = deriveKey(pin)
        val cipher = Cipher.getInstance(ALGORITHM)
        
        // Generate random IV
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val cipherText = cipher.doFinal(plainTextJson.toByteArray(Charsets.UTF_8))
        
        return Pair(
            Base64.encodeToString(cipherText, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(encryptedPayload: String, ivString: String, pin: String): String? {
        return try {
            val secretKey = deriveKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val parameterSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            val cipherTextBytes = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            val plainTextBytes = cipher.doFinal(cipherTextBytes)
            
            String(plainTextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null // Decryption failed (likely wrong PIN)
        }
    }
}
