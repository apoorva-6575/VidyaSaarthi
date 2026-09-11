package com.hackx.ruraledtech.p2p.passport.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PassportCryptoTest {

    @Test
    fun `test encryption generates unique ciphertexts and salts`() {
        val payload = """{"learnerId": "123", "progress": 50}"""
        val pin = "1234"

        val (ciphertext1, iv1, salt1) = PassportCrypto.encrypt(payload, pin)
        val (ciphertext2, iv2, salt2) = PassportCrypto.encrypt(payload, pin)

        assertNotNull(ciphertext1)
        assertNotNull(iv1)
        assertNotNull(salt1)
        
        // Random IVs and Salts should make ciphertext and salt unique every time
        assertNotEquals(ciphertext1, ciphertext2)
        assertNotEquals(iv1, iv2)
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun `test successful decryption with correct pin`() {
        val payload = """{"learnerId": "123", "progress": 50}"""
        val pin = "1234"

        val (ciphertext, iv, salt) = PassportCrypto.encrypt(payload, pin)
        
        val decrypted = PassportCrypto.decrypt(ciphertext, iv, salt, pin)
        
        assertEquals(payload, decrypted)
    }

    @Test
    fun `test failed decryption returns null with incorrect pin`() {
        val payload = """{"learnerId": "123", "progress": 50}"""
        val correctPin = "1234"
        val wrongPin = "4321"

        val (ciphertext, iv, salt) = PassportCrypto.encrypt(payload, correctPin)
        
        val decrypted = PassportCrypto.decrypt(ciphertext, iv, salt, wrongPin)
        
        assertNull(decrypted)
    }
}
