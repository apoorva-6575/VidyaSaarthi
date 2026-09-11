package com.hackx.ruraledtech.domain

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionOption
import com.hackx.ruraledtech.domain.model.QuestionType
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.QuizRepository
import com.hackx.ruraledtech.domain.usecase.quiz.GetQuestionsForLessonUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Verifies quiz question order adapts to the learner's current mastery instead of being static. */
class GetQuestionsForLessonUseCaseTest {

    private val quizRepository: QuizRepository = mockk()
    private val progressRepository: ProgressRepository = mockk()
    private val useCase = GetQuestionsForLessonUseCase(quizRepository, progressRepository)

    private fun question(id: String, difficulty: Float) = Question(
        questionId = id,
        lessonId = "lesson1",
        conceptId = "fraction_basics",
        questionType = QuestionType.SINGLE_CHOICE,
        language = "en",
        prompt = "prompt $id",
        options = listOf(QuestionOption("a", "x"), QuestionOption("b", "y")),
        correctOptionId = "a",
        difficulty = difficulty,
        explanation = null,
    )

    @Test
    fun `a struggling learner sees easiest questions first`() = runTest {
        val questions = listOf(question("hard", 0.9f), question("easy", 0.2f), question("medium", 0.5f))
        coEvery { quizRepository.getQuestionsForLesson("lesson1") } returns questions
        coEvery { progressRepository.getMastery("L-1", "fraction_basics") } returns
            Mastery("L-1", "fraction_basics", "Fractions", score = 0.1f, confidence = 0.5f, attemptCount = 1, lastUpdated = 0L)

        val result = useCase("lesson1", "L-1")

        assertThat(result.map { it.questionId }).containsExactly("easy", "medium", "hard").inOrder()
    }

    @Test
    fun `a proficient learner is not stuck starting with the easiest question`() = runTest {
        val questions = listOf(question("hard", 0.9f), question("easy", 0.2f), question("medium", 0.5f))
        coEvery { quizRepository.getQuestionsForLesson("lesson1") } returns questions
        coEvery { progressRepository.getMastery("L-1", "fraction_basics") } returns
            Mastery("L-1", "fraction_basics", "Fractions", score = 0.85f, confidence = 0.8f, attemptCount = 10, lastUpdated = 0L)

        val result = useCase("lesson1", "L-1")

        assertThat(result.first().questionId).isEqualTo("hard")
    }

    @Test
    fun `no mastery record yet defaults to starting easy`() = runTest {
        val questions = listOf(question("hard", 0.9f), question("easy", 0.2f), question("medium", 0.5f))
        coEvery { quizRepository.getQuestionsForLesson("lesson1") } returns questions
        coEvery { progressRepository.getMastery("L-1", "fraction_basics") } returns null

        val result = useCase("lesson1", "L-1")

        assertThat(result.first().questionId).isEqualTo("easy")
    }
}
