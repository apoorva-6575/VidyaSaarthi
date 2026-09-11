package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.domain.engine.AnswerEvaluator
import com.hackx.ruraledtech.domain.engine.EvaluationResult
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionType
import javax.inject.Inject

/**
 * Production implementation of [AnswerEvaluator] supporting SINGLE_CHOICE, MULTIPLE_CHOICE, and TRUE_FALSE.
 * Operates offline with zero network or cloud LLM dependency.
 */
class DefaultAnswerEvaluator @Inject constructor() : AnswerEvaluator {

    override fun evaluate(question: Question, selectedOptionIds: List<String>): EvaluationResult {
        if (selectedOptionIds.isEmpty()) {
            return EvaluationResult(
                correct = false,
                score = 0.0f,
                explanation = question.explanation,
            )
        }

        val isCorrect = when (question.questionType) {
            QuestionType.SINGLE_CHOICE -> evaluateSingleChoice(question, selectedOptionIds)
            QuestionType.MULTIPLE_CHOICE -> evaluateMultipleChoice(question, selectedOptionIds)
            QuestionType.TRUE_FALSE -> evaluateTrueFalse(question, selectedOptionIds)
        }

        return EvaluationResult(
            correct = isCorrect,
            score = if (isCorrect) 1.0f else 0.0f,
            explanation = question.explanation,
        )
    }

    private fun evaluateSingleChoice(question: Question, selectedOptionIds: List<String>): Boolean {
        return selectedOptionIds.size == 1 && selectedOptionIds.first() == question.correctOptionId
    }

    private fun evaluateMultipleChoice(question: Question, selectedOptionIds: List<String>): Boolean {
        val expectedOptions = parseExpectedOptions(question.correctOptionId)
        val selectedOptions = selectedOptionIds.toSet()
        return selectedOptions == expectedOptions
    }

    private fun evaluateTrueFalse(question: Question, selectedOptionIds: List<String>): Boolean {
        return selectedOptionIds.size == 1 &&
            selectedOptionIds.first().trim().lowercase() == question.correctOptionId.trim().lowercase()
    }

    private fun parseExpectedOptions(correctOptionIdRaw: String): Set<String> {
        return correctOptionIdRaw
            .split(",", "|", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
