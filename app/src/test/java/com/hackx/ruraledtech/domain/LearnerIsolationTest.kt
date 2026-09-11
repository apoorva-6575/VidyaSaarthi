package com.hackx.ruraledtech.domain

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.engine.AnswerEvaluator
import com.hackx.ruraledtech.domain.engine.EvaluationResult
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionType
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.QuizRepository
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import com.hackx.ruraledtech.domain.usecase.quiz.SubmitQuizAttemptUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Two learners answering the same question on a shared device must never cross-contaminate
 * mastery records (PS section 31's shared-device isolation guarantee) — each learnerId must
 * flow through to exactly its own Mastery row.
 */
class LearnerIsolationTest {

    @Test
    fun `mastery updates are strictly isolated by learnerId`() = runBlocking {
        val capturedMastery = mutableListOf<Mastery>()
        val progressRepo: ProgressRepository = mockk(relaxed = true)

        val quizRepo: QuizRepository = mockk(relaxed = true)
        val syncRepo: SyncRepository = mockk(relaxed = true)
        val recommendationRepo: RecommendationRepository = mockk(relaxed = true)

        val masterySlot = slot<Mastery>()
        coEvery { progressRepo.upsertMastery(capture(masterySlot)) } answers { capturedMastery.add(masterySlot.captured) }

        val engine: LearningEngine = mockk()
        coEvery { engine.processAttempt(any(), any()) } answers {
            val learnerId = firstArg<String>()
            val attempt = secondArg<com.hackx.ruraledtech.domain.model.Attempt>()
            LearningResult(
                updatedMastery = Mastery(
                    learnerId = learnerId, // Key check: engine receives and forwards the scoped learnerId
                    conceptId = attempt.conceptId,
                    conceptName = attempt.conceptId,
                    score = 0.5f,
                    confidence = 0.5f,
                    attemptCount = 1,
                    lastUpdated = 0L,
                ),
                recommendation = null,
            )
        }

        val idGenerator: IdGenerator = mockk()
        coEvery { idGenerator.attemptId() } returns "a1"
        coEvery { idGenerator.eventId() } returns "e1"

        val deviceIdProvider: DeviceIdProvider = mockk()
        coEvery { deviceIdProvider.get() } returns "d1"

        val clock: AppClock = mockk()
        coEvery { clock.nowMillis() } returns 1000L

        val answerEvaluator: AnswerEvaluator = mockk()
        coEvery { answerEvaluator.evaluate(any(), any()) } returns EvaluationResult(correct = true, score = 1f, explanation = "good")

        val useCase = SubmitQuizAttemptUseCase(
            quizRepository = quizRepo,
            syncRepository = syncRepo,
            progressRepository = progressRepo,
            recommendationRepository = recommendationRepo,
            learningEngine = engine,
            idGenerator = idGenerator,
            deviceIdProvider = deviceIdProvider,
            clock = clock,
            answerEvaluator = answerEvaluator,
        )

        val question = Question("q1", "l1", "c1", QuestionType.SINGLE_CHOICE, "en", "prompt", emptyList(), "opt1", 0.5f, "hint")

        // Act: learner1 answers
        useCase("learner1", question, listOf("opt1"), 500L)

        // Act: learner2 answers
        useCase("learner2", question, listOf("opt1"), 500L)

        // Assert: 2 mastery records were saved, one for learner1 and one for learner2
        assertEquals(2, capturedMastery.size)
        assertEquals("learner1", capturedMastery[0].learnerId)
        assertEquals("learner2", capturedMastery[1].learnerId)
    }
}
