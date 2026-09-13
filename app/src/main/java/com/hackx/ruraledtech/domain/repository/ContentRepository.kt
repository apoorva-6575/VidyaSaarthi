package com.hackx.ruraledtech.domain.repository

import com.hackx.ruraledtech.domain.model.ContentLookupResult
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun observeInstalledPackages(learnerId: String): Flow<List<ContentPackage>>
    suspend fun getInstalledPackages(learnerId: String): List<ContentPackage>
    suspend fun getSubjects(grade: Int, learnerId: String): List<String>
    suspend fun getLessons(subject: String, grade: Int, language: String, learnerId: String): List<Lesson>
    suspend fun getLesson(lessonId: String): ContentLookupResult
    suspend fun removePackage(packageId: String)
    suspend fun registerPackage(contentPackage: ContentPackage)
}
