package com.hackx.ruraledtech.domain.multilingual

import kotlinx.serialization.Serializable

/**
 * Representation of multilingual text mapping language codes (en, hi, mr, ta, te) to localized strings.
 */
@Serializable
data class LocalizedText(
    val translations: Map<String, String> = emptyMap(),
) {
    companion object {
        fun of(vararg pairs: Pair<String, String>): LocalizedText =
            LocalizedText(mapOf(*pairs))
    }
}
