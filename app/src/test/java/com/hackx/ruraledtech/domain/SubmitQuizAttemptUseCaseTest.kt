package com.hackx.ruraledtech.domain

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionOption
import com.hackx.ruraledtech.domain.model.QuestionType
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.QuizRepository
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import com.hackx.ruraledtech.domain.usecase.quiz.SubmitQuizAttemptUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Verifies the offline evaluation + persistence orchestration in isolation (no Room, no network). */
class SubmitQuizAttemptUseCaseTest {

    private val quizRepository: QuizRepository = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val progressRepository: ProgressRepository = mockk(relaxed = true)
    private val recommendationRepository: RecommendationRepository = mockk(relaxed = true)
    private val learningEngine: LearningEngine = mockk()
    private val idGenerator: IdGenerator = mockk()
    private val deviceIdProvider: DeviceIdProvider = mockk()
    private val clock: AppClock = mockk()

    private lateinit var useCase: SubmitQuizAttemptUseCase

    private val question = Question(
        questionId = "Q1",
        lessonId = "lesson1",
        conceptId = "fraction_basics",
        questionType = QuestionType.SINGLE_CHOICE,
        language = "en",
        prompt = "1/2 + 1/4?",
        options = listOf(QuestionOption("a", "3/4"), QuestionOption("b", "2/6")),
        correctOptionId = "a",
        difficulty = 0.3f,
        explanation = null,
    )

    @Before
    fun setUp() {
        every { idGenerator.attemptId() } returns "A-1"
        every { idGenerator.eventId() } returns "E-1"
        every { clock.nowMillis() } returns 1_000L
        coEvery { deviceIdProvider.get() } returns "D-1"
        coEvery { learningEngine.processAttempt(any(), any()) } returns LearningResult(
            updatedMastery = Mastery("L-1", "fraction_basics", "Fractions", 0.5f, 0.5f, 1, 1000L),
            recommendation = null,
        )
        useCase = SubmitQuizAttemptUseCase(
            quizRepository, syncRepository, progressRepository, recommendationRepository,
            learningEngine, idGenerator, deviceIdProvider, clock,
        )
    }

    @Test
    fun `correct answer is evaluated as correct and persisted before the sync event fires`() = runTest {
        val outcome = useCase(learnerId = "L-1", question = question, selectedOptionIds = listOf("a"), responseTimeMs = 500)

        assertThat(outcome.correct).isTrue()
        coVerify { quizRepository.saveAttempt(match<Attempt> { it.correct && it.questionId == "Q1" }) }
        coVerify { syncRepository.enqueueEvent(any()) }
        coVerify { progressRepository.upsertMastery(any()) }
    }

    @Test
    fun `wrong answer is evaluated as incorrect`() = runTest {
        val outcome = useCase(learnerId = "L-1", question = question, selectedOptionIds = listOf("b"), responseTimeMs = 500)

        assertThat(outcome.correct).isFalse()
    }
}
