package com.hackx.ruraledtech.domain.multilingual

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves localized strings deterministically with fallback.
 * Resolution order: preferred language -> default language -> first available translation -> empty string.
 */
interface LanguageResolver {
    fun resolve(text: LocalizedText, preferredLanguage: String, defaultLanguage: String = "en"): String
}

@Singleton
class DefaultLanguageResolver @Inject constructor() : LanguageResolver {

    override fun resolve(text: LocalizedText, preferredLanguage: String, defaultLanguage: String): String {
        if (text.translations.isEmpty()) return ""
        val normalizedPreferred = preferredLanguage.lowercase().trim()
        val normalizedDefault = defaultLanguage.lowercase().trim()

        return text.translations[normalizedPreferred]
            ?: text.translations[normalizedDefault]
            ?: text.translations.values.firstOrNull()
            ?: ""
    }
}
