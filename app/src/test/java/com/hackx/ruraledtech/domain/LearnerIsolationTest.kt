package com.hackx.ruraledtech.domain

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.engine.AnswerEvaluator
import com.hackx.ruraledtech.domain.engine.AnswerEvaluationResult
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionType
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.QuizRepository
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import com.hackx.ruraledtech.domain.usecase.quiz.SubmitQuizAttemptUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LearnerIsolationTest {

    @Test
    fun `mastery updates are strictly isolated by learnerId`() = runBlocking {
        // Setup mock repositories capturing the saved data
        val capturedMastery = mutableListOf<Mastery>()
        val progressRepo = object : ProgressRepository {
            override suspend fun upsertMastery(mastery: Mastery) { capturedMastery.add(mastery) }
            override suspend fun getMastery(learnerId: String, conceptId: String): Mastery? = null
            override suspend fun upsertLessonProgress(progress: com.hackx.ruraledtech.domain.model.LessonProgress) {}
            override fun observeLessonProgress(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.hackx.ruraledtech.domain.model.LessonProgress>>()
            override fun observeSubjectProgress(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.hackx.ruraledtech.domain.model.SubjectProgress>>()
            override fun observeMastery(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<List<Mastery>>()
        }

        val quizRepo = object : QuizRepository {
            override suspend fun saveAttempt(attempt: com.hackx.ruraledtech.domain.model.Attempt) {}
            override suspend fun getQuestionsForLesson(lessonId: String) = emptyList<Question>()
            override fun observeAttempts(learnerId: String, conceptId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.hackx.ruraledtech.domain.model.Attempt>>()
            override suspend fun getAttemptCount(learnerId: String, conceptId: String) = 0
        }

        val syncRepo = object : SyncRepository {
            override suspend fun enqueueEvent(event: SyncEvent) {}
            override suspend fun getPendingEvents() = emptyList<SyncEvent>()
            override suspend fun markEventSynced(eventId: String) {}
            override suspend fun markEventFailed(eventId: String, error: String) {}
        }
        
        val recommendationRepo = object : RecommendationRepository {
            override suspend fun saveRecommendation(recommendation: com.hackx.ruraledtech.domain.model.Recommendation) {}
            override fun observeLatestRecommendation(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<com.hackx.ruraledtech.domain.model.Recommendation?>()
            override suspend fun getLatestRecommendation(learnerId: String) = null
        }

        val engine = object : LearningEngine {
            override suspend fun processAttempt(learnerId: String, attempt: com.hackx.ruraledtech.domain.model.Attempt): LearningResult {
                return LearningResult(
                    updatedMastery = Mastery(
                        learnerId = learnerId, // Key check: engine receives and forwards the scoped learnerId
                        conceptId = attempt.conceptId,
                        score = 0.5f,
                        confidence = 0.5f,
                        level = MasteryLevel.PRACTICING,
                        attemptCount = 1,
                        lastUpdated = 0L
                    ),
                    recommendation = null,
                    contentRequirements = emptyList()
                )
            }
        }

        val useCase = SubmitQuizAttemptUseCase(
            quizRepository = quizRepo,
            syncRepository = syncRepo,
            progressRepository = progressRepo,
            recommendationRepository = recommendationRepo,
            learningEngine = engine,
            idGenerator = object : IdGenerator {
                override fun attemptId() = "a1"
                override fun eventId() = "e1"
            },
            deviceIdProvider = object : DeviceIdProvider {
                override fun get() = "d1"
            },
            clock = object : AppClock {
                override fun nowMillis() = 1000L
            },
            answerEvaluator = object : AnswerEvaluator {
                override fun evaluate(question: Question, selectedOptionIds: List<String>) = AnswerEvaluationResult(true, "good")
            },
            learningMesh = null
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
