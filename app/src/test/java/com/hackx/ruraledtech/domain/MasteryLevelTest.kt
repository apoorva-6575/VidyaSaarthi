package com.hackx.ruraledtech.domain

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.domain.model.MasteryLevel
import org.junit.Test

/** Verifies the exact thresholds from the PS (section 6): 0-0.39 / 0.40-0.69 / 0.70-0.84 / 0.85-1.00. */
class MasteryLevelTest {

    @Test
    fun `scores at and below 0_39 are Beginner`() {
        assertThat(MasteryLevel.fromScore(0f)).isEqualTo(MasteryLevel.BEGINNER)
        assertThat(MasteryLevel.fromScore(0.39f)).isEqualTo(MasteryLevel.BEGINNER)
    }

    @Test
    fun `scores from 0_40 to 0_69 are Developing`() {
        assertThat(MasteryLevel.fromScore(0.40f)).isEqualTo(MasteryLevel.DEVELOPING)
        assertThat(MasteryLevel.fromScore(0.69f)).isEqualTo(MasteryLevel.DEVELOPING)
    }

    @Test
    fun `scores from 0_70 to 0_84 are Proficient`() {
        assertThat(MasteryLevel.fromScore(0.70f)).isEqualTo(MasteryLevel.PROFICIENT)
        assertThat(MasteryLevel.fromScore(0.84f)).isEqualTo(MasteryLevel.PROFICIENT)
    }

    @Test
    fun `scores at and above 0_85 are Mastered`() {
        assertThat(MasteryLevel.fromScore(0.85f)).isEqualTo(MasteryLevel.MASTERED)
        assertThat(MasteryLevel.fromScore(1f)).isEqualTo(MasteryLevel.MASTERED)
    }
}
