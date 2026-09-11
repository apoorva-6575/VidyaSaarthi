package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.domain.engine.ConceptGraphResolver
import com.hackx.ruraledtech.domain.model.Concept
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.RecommendationType
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultRecommendationEngineTest {

    private val conceptGraphResolver: ConceptGraphResolver = mockk()
    private val clock: AppClock = mockk()
    private val engine = DefaultRecommendationEngine(conceptGraphResolver, clock)

    @Test
    fun `beginner mastery generates REMEDIATION recommendation targeting parent concept`() = runTest {
        every { clock.nowMillis() } returns 1000L
        coEvery { conceptGraphResolver.getRemedialTargetConcept("c_fractions_addition") } returns Concept(
            conceptId = "c_fractions_basics",
            subject = "Math",
            grade = 6,
            parentConceptId = null,
            name = "Fraction Basics",
        )

        val mastery = Mastery(
            learnerId = "l_1",
            conceptId = "c_fractions_addition",
            conceptName = "Fraction Addition",
            score = 0.20f,
            confidence = 0.5f,
            attemptCount = 3,
            lastUpdated = 1000L,
        )

        val recommendation = engine.generateRecommendation(mastery)

        assertThat(recommendation.recommendationType).isEqualTo(RecommendationType.REMEDIATION)
        assertThat(recommendation.conceptId).isEqualTo("c_fractions_basics")
        assertThat(recommendation.conceptName).isEqualTo("Fraction Basics")
        assertThat(recommendation.difficulty).isEqualTo(0.25f)
    }

    @Test
    fun `developing mastery generates PRACTICE recommendation`() = runTest {
        every { clock.nowMillis() } returns 1000L
        coEvery { conceptGraphResolver.getConcept("c_fractions") } returns Concept(
            conceptId = "c_fractions",
            subject = "Math",
            grade = 6,
            parentConceptId = null,
            name = "Fractions",
        )

        val mastery = Mastery(
            learnerId = "l_1",
            conceptId = "c_fractions",
            conceptName = "Fractions",
            score = 0.55f,
            confidence = 0.8f,
            attemptCount = 8,
            lastUpdated = 1000L,
        )

        val recommendation = engine.generateRecommendation(mastery)

        assertThat(recommendation.recommendationType).isEqualTo(RecommendationType.PRACTICE)
        assertThat(recommendation.conceptId).isEqualTo("c_fractions")
        assertThat(recommendation.difficulty).isEqualTo(0.45f)
    }

    @Test
    fun `proficient mastery generates NEXT_CONCEPT recommendation`() = runTest {
        every { clock.nowMillis() } returns 1000L
        coEvery { conceptGraphResolver.getConcept("c_fractions") } returns null

        val mastery = Mastery(
            learnerId = "l_1",
            conceptId = "c_fractions",
            conceptName = "Fractions",
            score = 0.75f,
            confidence = 1.0f,
            attemptCount = 10,
            lastUpdated = 1000L,
        )

        val recommendation = engine.generateRecommendation(mastery)

        assertThat(recommendation.recommendationType).isEqualTo(RecommendationType.NEXT_CONCEPT)
        assertThat(recommendation.difficulty).isEqualTo(0.65f)
    }

    @Test
    fun `mastered level generates ADVANCED recommendation`() = runTest {
        every { clock.nowMillis() } returns 1000L
        coEvery { conceptGraphResolver.getConcept("c_fractions") } returns null

        val mastery = Mastery(
            learnerId = "l_1",
            conceptId = "c_fractions",
            conceptName = "Fractions",
            score = 0.90f,
            confidence = 1.0f,
            attemptCount = 12,
            lastUpdated = 1000L,
        )

        val recommendation = engine.generateRecommendation(mastery)

        assertThat(recommendation.recommendationType).isEqualTo(RecommendationType.ADVANCED)
        assertThat(recommendation.difficulty).isEqualTo(0.85f)
    }
}
