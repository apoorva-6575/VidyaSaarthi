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
    ;

    companion object {
        fun fromTag(tag: String): SupportedLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
