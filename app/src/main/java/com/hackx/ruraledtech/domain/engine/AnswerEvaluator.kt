package com.hackx.ruraledtech.domain.engine

import com.hackx.ruraledtech.domain.model.Question

/**
 * Modular interface for offline answer evaluation.
 * Evaluates learner option selections against question metadata deterministically.
 */
interface AnswerEvaluator {
    fun evaluate(question: Question, selectedOptionIds: List<String>): EvaluationResult
}

/**
 * Result of evaluating an answer attempt.
 */
data class EvaluationResult(
    val correct: Boolean,
    val score: Float,
    val explanation: String? = null,
)
