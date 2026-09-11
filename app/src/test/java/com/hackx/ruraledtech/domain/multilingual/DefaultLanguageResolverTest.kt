package com.hackx.ruraledtech.domain.multilingual

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultLanguageResolverTest {

    private val resolver = DefaultLanguageResolver()

    @Test
    fun `resolves requested language when available`() {
        val text = LocalizedText.of(
            "en" to "Fractions",
            "hi" to "भिन्न",
            "mr" to "अपूर्णांक",
        )

        val resultHi = resolver.resolve(text, preferredLanguage = "hi")
        val resultMr = resolver.resolve(text, preferredLanguage = "mr")

        assertThat(resultHi).isEqualTo("भिन्न")
        assertThat(resultMr).isEqualTo("अपूर्णांक")
    }

    @Test
    fun `falls back to default language when requested language is missing`() {
        val text = LocalizedText.of(
            "en" to "Fractions",
            "hi" to "भिन्न",
        )

        val resultTa = resolver.resolve(text, preferredLanguage = "ta", defaultLanguage = "en")

        assertThat(resultTa).isEqualTo("Fractions")
    }

    @Test
    fun `falls back to first available translation when both preferred and default are missing`() {
        val text = LocalizedText.of(
            "mr" to "अपूर्णांक",
        )

        val resultTa = resolver.resolve(text, preferredLanguage = "ta", defaultLanguage = "en")

        assertThat(resultTa).isEqualTo("अपूर्णांक")
    }

    @Test
    fun `returns empty string safely when translations map is empty`() {
        val text = LocalizedText(emptyMap())

        val result = resolver.resolve(text, preferredLanguage = "hi")

        assertThat(result).isEmpty()
    }
}
