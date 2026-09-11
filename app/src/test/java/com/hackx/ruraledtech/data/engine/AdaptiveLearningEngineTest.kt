package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.local.entities.ConceptEntity
import com.hackx.ruraledtech.data.local.entities.QuestionEntity
import com.hackx.ruraledtech.domain.integration.ContentAvailabilityProvider
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionOption
import com.hackx.ruraledtech.domain.model.QuestionType
import com.hackx.ruraledtech.domain.model.RecommendationType
import com.hackx.ruraledtech.domain.model.SyncStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AdaptiveLearningEngineTest {

    private val attemptDao: AttemptDao = mockk(relaxed = true)
    private val questionDao: QuestionDao = mockk(relaxed = true)
    private val conceptDao: ConceptDao = mockk(relaxed = true)
    private val clock: AppClock = mockk()
    private val answerEvaluator = DefaultAnswerEvaluator()
    private val masteryCalculator = DefaultMasteryCalculator()
    private val conceptGraphResolver = ConceptGraphResolverImpl(conceptDao)
    private val recommendationEngine = DefaultRecommendationEngine(conceptGraphResolver, clock)

    private val contentAvailabilityProvider: ContentAvailabilityProvider = mockk(relaxed = true)

    private val engine = AdaptiveLearningEngineImpl(
        attemptDao = attemptDao,
        questionDao = questionDao,
        conceptDao = conceptDao,
        masteryCalculator = masteryCalculator,
        recommendationEngine = recommendationEngine,
        contentAvailabilityProvider = contentAvailabilityProvider,
        clock = clock,
    )

    @Test
    fun `complete end to end evaluation attempt flow produces learning result`() = runTest {
        every { clock.nowMillis() } returns 5000L

        val question = Question(
            questionId = "q_fractions_1",
            lessonId = "lesson_f1",
            conceptId = "concept_fractions",
            questionType = QuestionType.SINGLE_CHOICE,
            language = "en",
            prompt = "What is 1/2 + 1/2?",
            options = listOf(
                QuestionOption("opt_1", "1"),
                QuestionOption("opt_2", "2"),
            ),
            correctOptionId = "opt_1",
            difficulty = 0.6f,
            explanation = "1/2 + 1/2 = 1",
        )

        val evalResult = answerEvaluator.evaluate(question, listOf("opt_1"))
        assertThat(evalResult.correct).isTrue()

        val attempt = Attempt(
            attemptId = "att_101",
            learnerId = "learner_42",
            questionId = question.questionId,
            conceptId = question.conceptId,
            selectedOptionIds = listOf("opt_1"),
            correct = evalResult.correct,
            responseTimeMs = 1500L,
            timestamp = 5000L,
            deviceId = "dev_01",
            syncStatus = SyncStatus.PENDING,
        )

        coEvery { attemptDao.getRecent("learner_42", "concept_fractions", limit = 1000) } returns emptyList()
        coEvery { questionDao.getById("q_fractions_1") } returns QuestionEntity(
            questionId = "q_fractions_1",
            lessonId = "lesson_f1",
            conceptId = "concept_fractions",
            questionType = "SINGLE_CHOICE",
            language = "en",
            prompt = "What is 1/2 + 1/2?",
            optionsJson = "[]",
            correctOptionId = "opt_1",
            difficulty = 0.6f,
            explanation = null,
        )
        coEvery { conceptDao.getById("concept_fractions") } returns ConceptEntity(
            conceptId = "concept_fractions",
            subject = "Math",
            grade = 6,
            parentConceptId = null,
            name = "Fractions Addition",
        )

        val result = engine.processAttempt("learner_42", attempt)

        assertThat(result.updatedMastery.learnerId).isEqualTo("learner_42")
        assertThat(result.updatedMastery.conceptId).isEqualTo("concept_fractions")
        assertThat(result.updatedMastery.conceptName).isEqualTo("Fractions Addition")
        assertThat(result.updatedMastery.score).isGreaterThan(0.0f)
        assertThat(result.recommendation).isNotNull()
        assertThat(result.recommendation?.learnerId).isEqualTo("learner_42")
        assertThat(result.recommendation?.conceptId).isEqualTo("concept_fractions")
    }

    @Test
    fun `remediation flow produces ContentRequirement for Group 3 P2P mesh`() = runTest {
        every { clock.nowMillis() } returns 5000L

        val attempt = Attempt(
            attemptId = "att_102",
            learnerId = "learner_42",
            questionId = "q_hard_1",
            conceptId = "concept_fractions_addition",
            selectedOptionIds = listOf("wrong_opt"),
            correct = false,
            responseTimeMs = 2500L,
            timestamp = 5000L,
            deviceId = "dev_01",
            syncStatus = SyncStatus.PENDING,
        )

        coEvery { attemptDao.getRecent("learner_42", "concept_fractions_addition", limit = 1000) } returns emptyList()
        coEvery { questionDao.getById("q_hard_1") } returns null
        coEvery { conceptDao.getById("concept_fractions_addition") } returns ConceptEntity(
            conceptId = "concept_fractions_addition",
            subject = "Math",
            grade = 6,
            parentConceptId = "concept_fractions_basics",
            name = "Fraction Addition",
        )
        coEvery { conceptDao.getById("concept_fractions_basics") } returns ConceptEntity(
            conceptId = "concept_fractions_basics",
            subject = "Math",
            grade = 6,
            parentConceptId = null,
            name = "Fraction Basics",
        )
        coEvery { contentAvailabilityProvider.isPackageAvailable(any()) } returns true

        val result = engine.processAttempt("learner_42", attempt)

        assertThat(result.updatedMastery.level).isEqualTo(MasteryLevel.BEGINNER)
        assertThat(result.recommendation?.recommendationType).isEqualTo(RecommendationType.REMEDIATION)
        assertThat(result.recommendation?.conceptId).isEqualTo("concept_fractions_basics")

        // Verify ContentRequirement for Group 3 mesh
        assertThat(result.contentRequirements).isNotEmpty()
        val req = result.contentRequirements.first()
        assertThat(req.packageId).isEqualTo("concept_fractions_basics")
        assertThat(req.conceptId).isEqualTo("concept_fractions_basics")
        assertThat(req.priority).isEqualTo(0.90f)
    }
}
