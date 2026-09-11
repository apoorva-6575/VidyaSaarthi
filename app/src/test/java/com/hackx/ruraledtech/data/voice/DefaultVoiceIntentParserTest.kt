package com.hackx.ruraledtech.data.voice

import com.hackx.ruraledtech.domain.integration.VoiceIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultVoiceIntentParserTest {

    private val parser = DefaultVoiceIntentParser()

    @Test
    fun `parses English command keywords correctly`() {
        assertThat(parser.parse("start lesson")).isEqualTo(VoiceIntent.START_LESSON)
        assertThat(parser.parse("next")).isEqualTo(VoiceIntent.NEXT)
        assertThat(parser.parse("back")).isEqualTo(VoiceIntent.BACK)
        assertThat(parser.parse("repeat")).isEqualTo(VoiceIntent.REPEAT)
        assertThat(parser.parse("explain")).isEqualTo(VoiceIntent.EXPLAIN)
        assertThat(parser.parse("help")).isEqualTo(VoiceIntent.HELP)
        assertThat(parser.parse("pause")).isEqualTo(VoiceIntent.PAUSE)
        assertThat(parser.parse("submit")).isEqualTo(VoiceIntent.SUBMIT)
    }

    @Test
    fun `parses Hindi command keywords correctly`() {
        assertThat(parser.parse("shuru karo", "hi")).isEqualTo(VoiceIntent.START_LESSON)
        assertThat(parser.parse("aage badho", "hi")).isEqualTo(VoiceIntent.NEXT)
        assertThat(parser.parse("piche jao", "hi")).isEqualTo(VoiceIntent.BACK)
        assertThat(parser.parse("phir se batao", "hi")).isEqualTo(VoiceIntent.REPEAT)
        assertThat(parser.parse("samjhao", "hi")).isEqualTo(VoiceIntent.EXPLAIN)
        assertThat(parser.parse("maddad chahiye", "hi")).isEqualTo(VoiceIntent.HELP)
        assertThat(parser.parse("roko", "hi")).isEqualTo(VoiceIntent.PAUSE)
        assertThat(parser.parse("bhejo", "hi")).isEqualTo(VoiceIntent.SUBMIT)
    }

    @Test
    fun `parses Marathi command keywords correctly`() {
        assertThat(parser.parse("pudhe", "mr")).isEqualTo(VoiceIntent.NEXT)
        assertThat(parser.parse("maage", "mr")).isEqualTo(VoiceIntent.BACK)
        assertThat(parser.parse("chalu karo", "mr")).isEqualTo(VoiceIntent.START_LESSON)
        assertThat(parser.parse("thambo", "mr")).isEqualTo(VoiceIntent.PAUSE)
    }

    @Test
    fun `parses Tamil and Telugu command keywords correctly`() {
        assertThat(parser.parse("adutha", "ta")).isEqualTo(VoiceIntent.NEXT)
        assertThat(parser.parse("udavi", "ta")).isEqualTo(VoiceIntent.HELP)
        assertThat(parser.parse("thadupari", "te")).isEqualTo(VoiceIntent.NEXT)
        assertThat(parser.parse("niru", "te")).isEqualTo(VoiceIntent.PAUSE)
    }

    @Test
    fun `unknown command returns UNKNOWN intent`() {
        assertThat(parser.parse("random unknown text")).isEqualTo(VoiceIntent.UNKNOWN)
        assertThat(parser.parse("")).isEqualTo(VoiceIntent.UNKNOWN)
    }
}
