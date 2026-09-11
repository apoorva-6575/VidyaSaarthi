package com.hackx.ruraledtech.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.teacherAuthDataStore by preferencesDataStore(name = "teacher_auth_prefs")

/**
 * Holds the teacher's JWT locally. A teacher account only exists on Group 4's backend —
 * unlike learner profiles, this genuinely requires connectivity to establish, so it lives
 * in its own small store separate from [PreferencesManager]'s offline-first settings.
 */
@Singleton
class TeacherAuthStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("teacher_access_token")
        val TEACHER_NAME = stringPreferencesKey("teacher_name")
        val TEACHER_EMAIL = stringPreferencesKey("teacher_email")
    }

    val accessToken: Flow<String?> = context.teacherAuthDataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val teacherName: Flow<String?> = context.teacherAuthDataStore.data.map { it[Keys.TEACHER_NAME] }

    suspend fun currentToken(): String? = accessToken.first()

    suspend fun saveSession(token: String, name: String, email: String) {
        context.teacherAuthDataStore.edit {
            it[Keys.ACCESS_TOKEN] = token
            it[Keys.TEACHER_NAME] = name
            it[Keys.TEACHER_EMAIL] = email
        }
    }

    suspend fun clearSession() {
        context.teacherAuthDataStore.edit { it.clear() }
    }
}
