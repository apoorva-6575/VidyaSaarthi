package com.hackx.ruraledtech.p2p.passport

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.p2p.passport.crypto.PassportCrypto
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import android.util.Base64

class PassportCryptoTest {

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        io.mockk.every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        io.mockk.every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `encrypt and decrypt with valid PIN succeeds`() {
        val sampleJson = """{"learnerId":"learner_101","name":"Aarav"}"""
        val pin = "5678"

        val (encrypted, iv) = PassportCrypto.encrypt(sampleJson, pin)
        assertThat(encrypted).isNotEmpty()
        assertThat(iv).isNotEmpty()

        val decrypted = PassportCrypto.decrypt(encrypted, iv, pin)
        assertThat(decrypted).isEqualTo(sampleJson)
    }

    @Test
    fun `decrypt with invalid PIN returns null`() {
        val sampleJson = """{"learnerId":"learner_101","name":"Aarav"}"""
        val validPin = "5678"
        val wrongPin = "9999"

        val (encrypted, iv) = PassportCrypto.encrypt(sampleJson, validPin)
        val decrypted = PassportCrypto.decrypt(encrypted, iv, wrongPin)
        assertThat(decrypted).isNull()
    }

    @Test
    fun `decrypt with corrupted IV returns null`() {
        val sampleJson = """{"learnerId":"learner_101","name":"Aarav"}"""
        val pin = "5678"

        val (encrypted, _) = PassportCrypto.encrypt(sampleJson, pin)
        val dummyIv = java.util.Base64.getEncoder().encodeToString(ByteArray(12))
        val decrypted = PassportCrypto.decrypt(encrypted, dummyIv, pin)
        assertThat(decrypted).isNull()
    }
}
