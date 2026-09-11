package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionOption
import com.hackx.ruraledtech.domain.model.QuestionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultAnswerEvaluatorTest {

    private val evaluator = DefaultAnswerEvaluator()

    @Test
    fun `single choice correct returns correct true with score 1`() {
        val question = createQuestion(
            type = QuestionType.SINGLE_CHOICE,
            correctOptionId = "opt_2",
        )

        val result = evaluator.evaluate(question, listOf("opt_2"))

        assertThat(result.correct).isTrue()
        assertThat(result.score).isEqualTo(1.0f)
        assertThat(result.explanation).isEqualTo("Explanation text")
    }

    @Test
    fun `single choice incorrect returns correct false with score 0`() {
        val question = createQuestion(
            type = QuestionType.SINGLE_CHOICE,
            correctOptionId = "opt_2",
        )

        val result = evaluator.evaluate(question, listOf("opt_1"))

        assertThat(result.correct).isFalse()
        assertThat(result.score).isEqualTo(0.0f)
    }

    @Test
    fun `multiple choice exact match returns correct true`() {
        val question = createQuestion(
            type = QuestionType.MULTIPLE_CHOICE,
            correctOptionId = "opt_1,opt_3",
        )

        val result = evaluator.evaluate(question, listOf("opt_1", "opt_3"))

        assertThat(result.correct).isTrue()
        assertThat(result.score).isEqualTo(1.0f)
    }

    @Test
    fun `multiple choice missing option returns correct false`() {
        val question = createQuestion(
            type = QuestionType.MULTIPLE_CHOICE,
            correctOptionId = "opt_1,opt_3",
        )

        val result = evaluator.evaluate(question, listOf("opt_1"))

        assertThat(result.correct).isFalse()
        assertThat(result.score).isEqualTo(0.0f)
    }

    @Test
    fun `multiple choice extra option returns correct false`() {
        val question = createQuestion(
            type = QuestionType.MULTIPLE_CHOICE,
            correctOptionId = "opt_1,opt_3",
        )

        val result = evaluator.evaluate(question, listOf("opt_1", "opt_2", "opt_3"))

        assertThat(result.correct).isFalse()
        assertThat(result.score).isEqualTo(0.0f)
    }

    @Test
    fun `true false correct returns correct true`() {
        val question = createQuestion(
            type = QuestionType.TRUE_FALSE,
            correctOptionId = "true",
        )

        val result = evaluator.evaluate(question, listOf("TRUE"))

        assertThat(result.correct).isTrue()
        assertThat(result.score).isEqualTo(1.0f)
    }

    @Test
    fun `true false incorrect returns correct false`() {
        val question = createQuestion(
            type = QuestionType.TRUE_FALSE,
            correctOptionId = "true",
        )

        val result = evaluator.evaluate(question, listOf("false"))

        assertThat(result.correct).isFalse()
        assertThat(result.score).isEqualTo(0.0f)
    }

    @Test
    fun `empty selection returns correct false`() {
        val question = createQuestion(
            type = QuestionType.SINGLE_CHOICE,
            correctOptionId = "opt_1",
        )

        val result = evaluator.evaluate(question, emptyList())

        assertThat(result.correct).isFalse()
        assertThat(result.score).isEqualTo(0.0f)
    }

    private fun createQuestion(
        type: QuestionType,
        correctOptionId: String,
    ): Question {
        return Question(
            questionId = "q_101",
            lessonId = "lesson_1",
            conceptId = "concept_fractions",
            questionType = type,
            language = "en",
            prompt = "Sample prompt",
            options = listOf(
                QuestionOption("opt_1", "Option 1"),
                QuestionOption("opt_2", "Option 2"),
                QuestionOption("opt_3", "Option 3"),
            ),
            correctOptionId = correctOptionId,
            difficulty = 0.5f,
            explanation = "Explanation text",
        )
    }
}
