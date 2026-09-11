package com.hackx.ruraledtech.domain.usecase.quiz

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.model.SyncEventType
import com.hackx.ruraledtech.domain.model.SyncStatus
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.QuizRepository
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.hackx.ruraledtech.data.engine.DefaultAnswerEvaluator
import com.hackx.ruraledtech.domain.engine.AnswerEvaluator
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import javax.inject.Inject

data class QuizAttemptOutcome(val correct: Boolean, val learningResult: LearningResult)

/**
 * The single orchestration point for PS section 15-19: evaluate offline, persist the
 * attempt before anything else touches the network, create a durable sync event, then hand
 * off to whichever [LearningEngine] is wired in (mock today, Group 2's real engine later)
 * and persist what it returns. Every step here must succeed with zero connectivity.
 */
class SubmitQuizAttemptUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val syncRepository: SyncRepository,
    private val progressRepository: ProgressRepository,
    private val recommendationRepository: RecommendationRepository,
    private val learningEngine: LearningEngine,
    private val idGenerator: IdGenerator,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: AppClock,
    private val answerEvaluator: AnswerEvaluator = DefaultAnswerEvaluator(),
    private val learningMesh: LearningMesh? = null,
) {
    suspend operator fun invoke(
        learnerId: String,
        question: Question,
        selectedOptionIds: List<String>,
        responseTimeMs: Long,
    ): QuizAttemptOutcome {
        val evalResult = answerEvaluator.evaluate(question, selectedOptionIds)
        val correct = evalResult.correct
        val deviceId = deviceIdProvider.get()

        val attempt = Attempt(
            attemptId = idGenerator.attemptId(),
            learnerId = learnerId,
            questionId = question.questionId,
            conceptId = question.conceptId,
            selectedOptionIds = selectedOptionIds,
            correct = correct,
            responseTimeMs = responseTimeMs,
            timestamp = clock.nowMillis(),
            deviceId = deviceId,
            syncStatus = SyncStatus.PENDING,
        )
        quizRepository.saveAttempt(attempt)

        syncRepository.enqueueEvent(
            SyncEvent(
                eventId = idGenerator.eventId(),
                learnerId = learnerId,
                deviceId = deviceId,
                eventType = SyncEventType.QUIZ_ATTEMPT,
                timestamp = attempt.timestamp,
                payloadJson = quizAttemptPayload(question.questionId, correct),
                syncStatus = SyncStatus.PENDING,
            ),
        )

        val result = learningEngine.processAttempt(learnerId, attempt)
        progressRepository.upsertMastery(result.updatedMastery)

        val mastery = result.updatedMastery
        syncRepository.enqueueEvent(
            SyncEvent(
                eventId = idGenerator.eventId(),
                learnerId = learnerId,
                deviceId = deviceId,
                eventType = SyncEventType.MASTERY_UPDATED,
                timestamp = mastery.lastUpdated,
                payloadJson = masteryUpdatedPayload(mastery),
                syncStatus = SyncStatus.PENDING,
            ),
        )

        result.recommendation?.let { recommendationRepository.saveRecommendation(it) }

        // Automatically dispatch missing or remedial ContentRequirements to LearningMesh for P2P acquisition
        result.contentRequirements.forEach { requirement ->
            learningMesh?.requestContent(requirement)
        }

        return QuizAttemptOutcome(correct = correct, learningResult = result)
    }

    private fun quizAttemptPayload(questionId: String, correct: Boolean): String =
        Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("question_id" to JsonPrimitive(questionId), "correct" to JsonPrimitive(correct))),
        )

    private fun masteryUpdatedPayload(mastery: com.hackx.ruraledtech.domain.model.Mastery): String =
        Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "concept_id" to JsonPrimitive(mastery.conceptId),
                    "mastery" to JsonPrimitive(mastery.score),
                    "confidence" to JsonPrimitive(mastery.confidence),
                    "attempt_count" to JsonPrimitive(mastery.attemptCount),
                ),
            ),
        )
}

class GetQuestionsForLessonUseCase @Inject constructor(private val repository: QuizRepository) {
    suspend operator fun invoke(lessonId: String): List<Question> = repository.getQuestionsForLesson(lessonId)
}
