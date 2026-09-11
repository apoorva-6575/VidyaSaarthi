package com.hackx.ruraledtech.data.passport

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao
import com.hackx.ruraledtech.data.local.entities.AttemptEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.local.entities.LessonProgressEntity
import com.hackx.ruraledtech.data.local.entities.MasteryEntity
import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LearnerDataImporterTest {

    private val learnerDao: LearnerDao = mockk(relaxed = true)
    private val progressDao: ProgressDao = mockk(relaxed = true)
    private val masteryDao: MasteryDao = mockk(relaxed = true)
    private val attemptDao: AttemptDao = mockk(relaxed = true)
    private val clock: AppClock = mockk()

    private lateinit var importer: LearnerDataImporterImpl

    @Before
    fun setUp() {
        every { clock.nowMillis() } returns 10000L
        importer = LearnerDataImporterImpl(
            learnerDao = learnerDao,
            progressDao = progressDao,
            masteryDao = masteryDao,
            attemptDao = attemptDao,
            clock = clock,
        )
    }

    @Test
    fun `importLearnerData imports attempts and is idempotent by attemptId`() = runTest {
        coEvery { learnerDao.getById("learner_1") } returns null
        coEvery { attemptDao.getById("att_1") } returns null
        coEvery { attemptDao.getById("att_2") } returns AttemptEntity(
            attemptId = "att_2",
            learnerId = "learner_1",
            questionId = "q_2",
            conceptId = "c_2",
            selectedOptionIds = emptyList(),
            correct = true,
            responseTimeMs = 1000L,
            timestamp = 5000L,
            deviceId = "dev_local",
            syncStatus = "SYNCED",
        )

        val exportData = LearnerExportData(
            learnerId = "learner_1",
            name = "Rohan",
            grade = 5,
            preferredLanguage = "hi",
            passportVersion = 5000L,
            progressJson = "[]",
            masteryJson = "[]",
            attemptsJson = """[
                {"attemptId":"att_1","questionId":"q_1","conceptId":"c_1","correct":true,"timestamp":4000},
                {"attemptId":"att_2","questionId":"q_2","conceptId":"c_2","correct":true,"timestamp":5000}
            ]""",
            exportedAt = 5000L,
        )

        val result = importer.importLearnerData(exportData)
        assertThat(result).isInstanceOf(ImportResult.Imported::class.java)

        val insertedAttempt = slot<AttemptEntity>()
        coVerify(exactly = 1) { attemptDao.insert(capture(insertedAttempt)) }
        assertThat(insertedAttempt.captured.attemptId).isEqualTo("att_1")
    }

    @Test
    fun `importLearnerData does not overwrite newer local progress with older passport data`() = runTest {
        coEvery { learnerDao.getById("learner_1") } returns LearnerEntity("learner_1", "Rohan", 5, "hi", "default", 1000L, 1000L, 1000L)
        coEvery { progressDao.get("learner_1", "lesson_math") } returns LessonProgressEntity(
            learnerId = "learner_1",
            lessonId = "lesson_math",
            completionPercentage = 100f,
            completed = true,
            lastPosition = 10,
            updatedAt = 9000L, // Newer local progress
        )

        val exportData = LearnerExportData(
            learnerId = "learner_1",
            name = "Rohan",
            grade = 5,
            preferredLanguage = "hi",
            passportVersion = 5000L,
            progressJson = """[
                {"lessonId":"lesson_math","completionPercentage":50.0,"completed":false,"lastPosition":5,"updatedAt":3000}
            ]""",
            masteryJson = "[]",
            attemptsJson = "[]",
            exportedAt = 5000L,
        )

        val result = importer.importLearnerData(exportData)
        assertThat(result).isInstanceOf(ImportResult.Merged::class.java)

        // Older progress (updatedAt 3000) should NOT be upserted over local progress (updatedAt 9000)
        coVerify(exactly = 0) { progressDao.upsert(any()) }
    }

    @Test
    fun `importLearnerData does not overwrite newer local mastery with older passport data`() = runTest {
        coEvery { learnerDao.getById("learner_1") } returns LearnerEntity("learner_1", "Rohan", 5, "hi", "default", 1000L, 1000L, 1000L)
        coEvery { masteryDao.get("learner_1", "concept_fractions") } returns MasteryEntity(
            learnerId = "learner_1",
            conceptId = "concept_fractions",
            conceptName = "Fractions",
            score = 0.95f,
            confidence = 0.90f,
            attemptCount = 10,
            lastUpdated = 9500L, // Newer local mastery
        )

        val exportData = LearnerExportData(
            learnerId = "learner_1",
            name = "Rohan",
            grade = 5,
            preferredLanguage = "hi",
            passportVersion = 5000L,
            progressJson = "[]",
            masteryJson = """[
                {"conceptId":"concept_fractions","conceptName":"Fractions","score":0.50,"confidence":0.40,"attemptCount":2,"lastUpdated":2000}
            ]""",
            attemptsJson = "[]",
            exportedAt = 5000L,
        )

        importer.importLearnerData(exportData)

        // Older mastery (lastUpdated 2000) should NOT be upserted over local mastery (lastUpdated 9500)
        coVerify(exactly = 0) { masteryDao.upsert(any()) }
    }
}
