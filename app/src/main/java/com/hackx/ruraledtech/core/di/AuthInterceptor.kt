package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.core.datastore.TeacherAuthStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Attaches the teacher's bearer token to every request; a no-op if no session exists. */
class AuthInterceptor @Inject constructor(
    private val teacherAuthStore: TeacherAuthStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { teacherAuthStore.currentToken() }
        val request = if (token != null) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
