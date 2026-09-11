package com.hackx.ruraledtech.core.seed

import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.local.entities.ConceptEntity
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.CalloutTone
import com.hackx.ruraledtech.domain.model.ContentBlock
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.ContentPackageState
import com.hackx.ruraledtech.domain.model.Lesson
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionOption
import com.hackx.ruraledtech.domain.model.QuestionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ships a small "Fractions" package pre-installed, the way a phone would arrive from a
 * teacher/school already loaded with content (PS section 4.2 example: "Lesson Package v1").
 * This bypasses ContentInstaller's checksum path deliberately — a factory-preloaded package
 * was never transferred over any wire, so there's nothing to verify against. Everything a
 * package acquired via P2P or a backend download still goes through ContentInstaller.
 */
@Singleton
class DemoContentSeeder @Inject constructor(
    private val contentPackageDao: ContentPackageDao,
    private val lessonDao: LessonDao,
    private val questionDao: QuestionDao,
    private val conceptDao: ConceptDao,
) {
    private companion object {
        const val PACKAGE_ID = "math-grade5-fractions"
    }

    suspend fun seedIfNeeded() {
        if (contentPackageDao.getLatest(PACKAGE_ID) != null) return

        conceptDao.insertAll(
            listOf(
                ConceptEntity("fraction_basics", "Mathematics", 5, null, "Fractions"),
                ConceptEntity("decimal_basics", "Mathematics", 5, null, "Decimals"),
            ),
        )

        val lessons = listOf(englishLesson(), hindiLesson())
        lessonDao.insertAll(lessons.map { it.toEntity() })

        val questions = englishQuestions() + hindiQuestions()
        questionDao.insertAll(questions.map { it.toEntity() })

        contentPackageDao.upsert(
            ContentPackage(
                packageId = PACKAGE_ID,
                version = 1,
                subject = "Mathematics",
                grade = 5,
                languages = listOf("en", "hi"),
                sizeBytes = 40_000_000L,
                checksum = "preinstalled-no-transfer",
                installedAt = System.currentTimeMillis(),
                state = ContentPackageState.INSTALLED,
            ).toEntity(),
        )
    }

    private fun englishLesson() = Lesson(
        lessonId = "lesson_fractions_intro_en",
        packageId = PACKAGE_ID,
        subject = "Mathematics",
        grade = 5,
        conceptId = "fraction_basics",
        language = "en",
        title = "Understanding Fractions",
        orderIndex = 0,
        blocks = listOf(
            ContentBlock.Text("b1", "A fraction represents a part of a whole. If you cut a roti into 4 equal pieces and eat 1, you ate 1/4 of it."),
            ContentBlock.Example("b2", "Example: 1/2 + 1/4", "Convert 1/2 to 2/4, then add: 2/4 + 1/4 = 3/4."),
            ContentBlock.Callout("b3", "The bottom number (denominator) tells you how many equal parts the whole is split into.", CalloutTone.TIP),
        ),
    )

    private fun hindiLesson() = Lesson(
        lessonId = "lesson_fractions_intro_hi",
        packageId = PACKAGE_ID,
        subject = "Mathematics",
        grade = 5,
        conceptId = "fraction_basics",
        language = "hi",
        title = "भिन्न को समझना",
        orderIndex = 0,
        blocks = listOf(
            ContentBlock.Text("b1", "भिन्न किसी पूरी चीज़ के एक हिस्से को दर्शाता है। अगर आप एक रोटी को 4 बराबर टुकड़ों में काटें और 1 खाएं, तो आपने 1/4 रोटी खाई।"),
            ContentBlock.Example("b2", "उदाहरण: 1/2 + 1/4", "1/2 को 2/4 में बदलें, फिर जोड़ें: 2/4 + 1/4 = 3/4"),
            ContentBlock.Callout("b3", "नीचे की संख्या (हर) बताती है कि पूरी चीज़ कितने बराबर हिस्सों में बंटी है।", CalloutTone.TIP),
        ),
    )

    private fun englishQuestions() = listOf(
        Question("q_en_1", "lesson_fractions_intro_en", "fraction_basics", QuestionType.SINGLE_CHOICE, "en", "What is 1/2 + 1/4?", listOf(QuestionOption("a", "3/4"), QuestionOption("b", "2/6"), QuestionOption("c", "1/4"), QuestionOption("d", "3/6")), "a", 0.3f, "Convert to a common denominator first."),
        Question("q_en_2", "lesson_fractions_intro_en", "fraction_basics", QuestionType.SINGLE_CHOICE, "en", "Which fraction is equivalent to 1/2?", listOf(QuestionOption("a", "2/5"), QuestionOption("b", "2/4"), QuestionOption("c", "3/5"), QuestionOption("d", "1/3")), "b", 0.4f, "Multiply numerator and denominator by the same number."),
        Question("q_en_3", "lesson_fractions_intro_en", "fraction_basics", QuestionType.SINGLE_CHOICE, "en", "What is 3/4 - 1/4?", listOf(QuestionOption("a", "1/2"), QuestionOption("b", "2/4"), QuestionOption("c", "1/4"), QuestionOption("d", "2/8")), "b", 0.5f, "Same denominator — just subtract the numerators."),
    )

    private fun hindiQuestions() = listOf(
        Question("q_hi_1", "lesson_fractions_intro_hi", "fraction_basics", QuestionType.SINGLE_CHOICE, "hi", "1/2 + 1/4 कितना है?", listOf(QuestionOption("a", "3/4"), QuestionOption("b", "2/6"), QuestionOption("c", "1/4"), QuestionOption("d", "3/6")), "a", 0.3f, "पहले उभयनिष्ठ हर बनाएं।"),
        Question("q_hi_2", "lesson_fractions_intro_hi", "fraction_basics", QuestionType.SINGLE_CHOICE, "hi", "1/2 के बराबर कौन सी भिन्न है?", listOf(QuestionOption("a", "2/5"), QuestionOption("b", "2/4"), QuestionOption("c", "3/5"), QuestionOption("d", "1/3")), "b", 0.4f, "अंश और हर को समान संख्या से गुणा करें।"),
    )
}
