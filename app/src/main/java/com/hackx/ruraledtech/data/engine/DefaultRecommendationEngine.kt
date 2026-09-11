package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.domain.engine.ConceptGraphResolver
import com.hackx.ruraledtech.domain.engine.RecommendationEngine
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.model.RecommendationType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [RecommendationEngine].
 * Uses [ConceptGraphResolver] to target remedial prerequisites when learner is in BEGINNER state.
 */
@Singleton
class DefaultRecommendationEngine @Inject constructor(
    private val conceptGraphResolver: ConceptGraphResolver,
    private val clock: AppClock,
) : RecommendationEngine {

    override suspend fun generateRecommendation(mastery: Mastery): Recommendation {
        val (type, targetDifficulty) = when (mastery.level) {
            MasteryLevel.BEGINNER -> RecommendationType.REMEDIATION to 0.25f
            MasteryLevel.DEVELOPING -> RecommendationType.PRACTICE to 0.45f
            MasteryLevel.PROFICIENT -> RecommendationType.NEXT_CONCEPT to 0.65f
            MasteryLevel.MASTERED -> RecommendationType.ADVANCED to 0.85f
        }

        val targetConcept = if (type == RecommendationType.REMEDIATION) {
            conceptGraphResolver.getRemedialTargetConcept(mastery.conceptId)
        } else {
            conceptGraphResolver.getConcept(mastery.conceptId)
        }

        val targetConceptId = targetConcept?.conceptId ?: mastery.conceptId
        val targetConceptName = targetConcept?.name ?: mastery.conceptName

        return Recommendation(
            learnerId = mastery.learnerId,
            conceptId = targetConceptId,
            conceptName = targetConceptName,
            mastery = mastery.score,
            recommendationType = type,
            contentPackageId = targetConceptId,
            targetLessonId = null,
            difficulty = targetDifficulty,
            generatedAt = clock.nowMillis(),
        )
    }
}
