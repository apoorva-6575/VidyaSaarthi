package com.hackx.ruraledtech.feature.common

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data object Empty : UiState<Nothing>()
    data class Error(val message: String) : UiState<Nothing>()
}

enum class SupportedLanguage(val tag: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    MARATHI("mr", "Marathi", "मराठी"),
    BENGALI("bn", "Bengali", "বাংলা"),
    TELUGU("te", "Telugu", "తెలుగు"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ"),
    MALAYALAM("ml", "Malayalam", "മലയാളം"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ"),
    ASSAMESE("as", "Assamese", "অসমীয়া"),
    URDU("ur", "Urdu", "اردو"),
    ;

    companion object {
        fun fromTag(tag: String): SupportedLanguage = entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH
    }
}
