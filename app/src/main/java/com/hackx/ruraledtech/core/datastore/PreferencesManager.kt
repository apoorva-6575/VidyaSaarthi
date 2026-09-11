package com.hackx.ruraledtech.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "rural_edtech_prefs")

/**
 * Lightweight settings only (PS section 32) — device id, current learner, language,
 * onboarding flag, accessibility toggles. Structured learning data lives in Room, not here.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val CURRENT_LEARNER_ID = stringPreferencesKey("current_learner_id")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LARGE_TEXT = booleanPreferencesKey("accessibility_large_text")
        val AUDIO_NAV_ENABLED = booleanPreferencesKey("accessibility_audio_nav")
    }

    val currentLearnerId: Flow<String?> = context.dataStore.data.map { it[Keys.CURRENT_LEARNER_ID] }
    val uiLanguage: Flow<String> = context.dataStore.data.map { it[Keys.UI_LANGUAGE] ?: "en" }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val largeTextEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.LARGE_TEXT] ?: false }
    val audioNavEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUDIO_NAV_ENABLED] ?: true }

    suspend fun setCurrentLearnerId(learnerId: String?) {
        context.dataStore.edit {
            if (learnerId == null) it.remove(Keys.CURRENT_LEARNER_ID) else it[Keys.CURRENT_LEARNER_ID] = learnerId
        }
    }

    suspend fun setUiLanguage(languageTag: String) {
        context.dataStore.edit { it[Keys.UI_LANGUAGE] = languageTag }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setLargeTextEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LARGE_TEXT] = enabled }
    }

    suspend fun setAudioNavEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_NAV_ENABLED] = enabled }
    }

    suspend fun getOrCreateDeviceId(generate: () -> String): String {
        var deviceId: String? = null
        context.dataStore.edit { prefs ->
            deviceId = prefs[Keys.DEVICE_ID] ?: generate().also { prefs[Keys.DEVICE_ID] = it }
        }
        return deviceId!!
    }
}
