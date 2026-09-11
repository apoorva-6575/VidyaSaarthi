package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.domain.model.SyncStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultMasteryCalculatorTest {

    private val calculator = DefaultMasteryCalculator()

    @Test
    fun `empty attempts returns zero mastery and zero confidence`() {
        val mastery = calculator.calculateMastery(
            learnerId = "learner_1",
            conceptId = "concept_math",
            conceptName = "Fractions",
            attempts = emptyList(),
            questionDifficulty = 0.5f,
            timestamp = 1000L,
        )

        assertThat(mastery.score).isEqualTo(0.0f)
        assertThat(mastery.confidence).isEqualTo(0.0f)
        assertThat(mastery.level).isEqualTo(MasteryLevel.BEGINNER)
    }

    @Test
    fun `perfect performance on difficult question produces high score`() {
        val attempts = (1..5).map { index ->
            createAttempt(id = "a_$index", correct = true)
        }

        val mastery = calculator.calculateMastery(
            learnerId = "learner_1",
            conceptId = "concept_math",
            conceptName = "Fractions",
            attempts = attempts,
            questionDifficulty = 1.0f,
            timestamp = 1000L,
        )

        // accuracy = 1.0 (0.50)
        // recentAccuracy = 1.0 (0.25)
        // difficultyFactor = 1.0 (0.15)
        // consistency = 1.0 (0.10)
        // total = 0.50 + 0.25 + 0.15 + 0.10 = 1.0
        assertThat(mastery.score).isEqualTo(1.0f)
        assertThat(mastery.level).isEqualTo(MasteryLevel.MASTERED)
    }

    @Test
    fun `poor performance produces low score in beginner level`() {
        val attempts = (1..5).map { index ->
            createAttempt(id = "a_$index", correct = false)
        }

        val mastery = calculator.calculateMastery(
            learnerId = "learner_1",
            conceptId = "concept_math",
            conceptName = "Fractions",
            attempts = attempts,
            questionDifficulty = 0.5f,
            timestamp = 1000L,
        )

        // accuracy = 0.0, recentAccuracy = 0.0, difficultyFactor = 0.0, consistency = 1.0
        // total = 0.10
        assertThat(mastery.score).isEqualTo(0.10f)
        assertThat(mastery.level).isEqualTo(MasteryLevel.BEGINNER)
    }

    @Test
    fun `recent accuracy affects score on improving learner`() {
        // 5 old wrong attempts, 5 recent correct attempts (10 total)
        val attempts = (1..5).map { createAttempt(id = "new_$it", correct = true) } +
            (1..5).map { createAttempt(id = "old_$it", correct = false) }

        val mastery = calculator.calculateMastery(
            learnerId = "learner_1",
            conceptId = "concept_math",
            conceptName = "Fractions",
            attempts = attempts,
            questionDifficulty = 0.6f,
            timestamp = 1000L,
        )

        // accuracy = 5/10 = 0.50 (0.50 * 0.50 = 0.25)
        // recentAccuracy = 5/5 = 1.0 (0.25 * 1.0 = 0.25)
        // difficultyFactor = 0.6 (0.15 * 0.6 = 0.09)
        // consistency = 1.0 (0.10 * 1.0 = 0.10)
        // total = 0.25 + 0.25 + 0.09 + 0.10 = 0.69
        assertThat(mastery.score).isWithin(0.01f).of(0.69f)
        assertThat(mastery.level).isEqualTo(MasteryLevel.DEVELOPING)
    }

    @Test
    fun `mastery thresholds boundaries check`() {
        assertThat(MasteryLevel.fromScore(0.00f)).isEqualTo(MasteryLevel.BEGINNER)
        assertThat(MasteryLevel.fromScore(0.39f)).isEqualTo(MasteryLevel.BEGINNER)
        assertThat(MasteryLevel.fromScore(0.40f)).isEqualTo(MasteryLevel.DEVELOPING)
        assertThat(MasteryLevel.fromScore(0.69f)).isEqualTo(MasteryLevel.DEVELOPING)
        assertThat(MasteryLevel.fromScore(0.70f)).isEqualTo(MasteryLevel.PROFICIENT)
        assertThat(MasteryLevel.fromScore(0.84f)).isEqualTo(MasteryLevel.PROFICIENT)
        assertThat(MasteryLevel.fromScore(0.85f)).isEqualTo(MasteryLevel.MASTERED)
        assertThat(MasteryLevel.fromScore(1.00f)).isEqualTo(MasteryLevel.MASTERED)
    }

    @Test
    fun `deterministic repeated inputs produce identical outputs`() {
        val attempts = listOf(
            createAttempt(id = "1", correct = true),
            createAttempt(id = "2", correct = false),
            createAttempt(id = "3", correct = true),
        )

        val m1 = calculator.calculateMastery("l1", "c1", "Concept", attempts, 0.5f, 100L)
        val m2 = calculator.calculateMastery("l1", "c1", "Concept", attempts, 0.5f, 100L)

        assertThat(m1).isEqualTo(m2)
    }

    private fun createAttempt(id: String, correct: Boolean): Attempt {
        return Attempt(
            attemptId = id,
            learnerId = "learner_1",
            questionId = "q_1",
            conceptId = "concept_math",
            selectedOptionIds = listOf("opt_1"),
            correct = correct,
            responseTimeMs = 2000L,
            timestamp = 1000L,
            deviceId = "dev_1",
            syncStatus = SyncStatus.PENDING,
        )
    }
}
