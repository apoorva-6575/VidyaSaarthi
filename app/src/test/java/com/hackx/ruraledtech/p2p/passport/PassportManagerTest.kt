package com.hackx.ruraledtech.p2p.passport

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.domain.integration.LearnerDataExporter
import com.hackx.ruraledtech.domain.integration.LearnerDataImporter
import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.passport.crypto.PassportCrypto
import com.hackx.ruraledtech.p2p.passport.model.LearningPassport
import com.hackx.ruraledtech.p2p.passport.transport.PassportImportResult
import com.hackx.ruraledtech.p2p.passport.transport.PassportManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassportManagerTest {

    private val exporter: LearnerDataExporter = mockk(relaxed = true)
    private val importer: LearnerDataImporter = mockk(relaxed = true)
    private val connectionManager: P2PConnectionManager = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var passportManager: PassportManager
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        mockkStatic(android.util.Log::class)
        io.mockk.every { android.util.Log.d(any(), any()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any(), any()) } returns 0
        io.mockk.every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        io.mockk.every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        io.mockk.every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }

        passportManager = PassportManager(
            exporter = exporter,
            importer = importer,
            connectionManager = connectionManager,
            coroutineScope = testScope,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onPassportReceived updates pendingPassport StateFlow`() {
        val passport = LearningPassport(
            passportId = "pass_001",
            learnerId = "learner_42",
            version = 1,
            timestamp = 1000L,
            encryptedPayload = "encrypted",
            iv = "iv_str",
        )

        assertThat(passportManager.pendingPassport.value).isNull()
        passportManager.onPassportReceived(passport)
        assertThat(passportManager.pendingPassport.value).isEqualTo(passport)
    }

    @Test
    fun `importPassport with valid PIN decrypts and imports data successfully`() = runTest {
        val pin = "1234"
        val exportData = LearnerExportData(
            learnerId = "learner_42",
            name = "Ananya",
            grade = 4,
            preferredLanguage = "hi",
            passportVersion = 1000L,
            progressJson = "[]",
            masteryJson = "[]",
            attemptsJson = "[]",
            exportedAt = 1000L,
        )
        val plainJson = json.encodeToString(LearnerExportData.serializer(), exportData)
        val (encrypted, iv) = PassportCrypto.encrypt(plainJson, pin)

        val passport = LearningPassport(
            passportId = "pass_002",
            learnerId = "learner_42",
            version = 1,
            timestamp = 1000L,
            encryptedPayload = encrypted,
            iv = iv,
        )

        passportManager.onPassportReceived(passport)
        coEvery { importer.importLearnerData(exportData) } returns ImportResult.Imported("learner_42")

        val result = passportManager.importPassport(passport, pin)

        assertThat(result).isInstanceOf(PassportImportResult.Success::class.java)
        val success = result as PassportImportResult.Success
        assertThat(success.learnerId).isEqualTo("learner_42")
        assertThat(passportManager.pendingPassport.value).isNull()
        coVerify(exactly = 1) { importer.importLearnerData(exportData) }
    }

    @Test
    fun `importPassport with invalid PIN returns InvalidPin`() = runTest {
        val validPin = "1234"
        val wrongPin = "9999"
        val exportData = LearnerExportData(
            learnerId = "learner_42",
            name = "Ananya",
            grade = 4,
            preferredLanguage = "hi",
            passportVersion = 1000L,
            progressJson = "[]",
            masteryJson = "[]",
            attemptsJson = "[]",
            exportedAt = 1000L,
        )
        val plainJson = json.encodeToString(LearnerExportData.serializer(), exportData)
        val (encrypted, iv) = PassportCrypto.encrypt(plainJson, validPin)

        val passport = LearningPassport(
            passportId = "pass_003",
            learnerId = "learner_42",
            version = 1,
            timestamp = 1000L,
            encryptedPayload = encrypted,
            iv = iv,
        )

        val result = passportManager.importPassport(passport, wrongPin)
        assertThat(result).isEqualTo(PassportImportResult.InvalidPin)
    }

    @Test
    fun `importPassport with malformed payload returns MalformedPassport`() = runTest {
        val pin = "1234"
        val malformedJson = "{ invalid json structure }"
        val (encrypted, iv) = PassportCrypto.encrypt(malformedJson, pin)

        val passport = LearningPassport(
            passportId = "pass_004",
            learnerId = "learner_42",
            version = 1,
            timestamp = 1000L,
            encryptedPayload = encrypted,
            iv = iv,
        )

        val result = passportManager.importPassport(passport, pin)
        assertThat(result).isEqualTo(PassportImportResult.MalformedPassport)
    }
}
