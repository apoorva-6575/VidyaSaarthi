package com.hackx.ruraledtech.data.repository

import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.ContentLookupResult
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.ContentPackageState
import com.hackx.ruraledtech.domain.model.Lesson
import com.hackx.ruraledtech.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Local-first per PS section 34: every read comes from Room, never the network. When a
 * lesson isn't installed locally this returns [ContentLookupResult.NotAvailableLocally]
 * rather than an error, which is the hook Group 3's P2P mesh plugs into (PS section 20/58).
 */
class ContentRepositoryImpl @Inject constructor(
    private val lessonDao: LessonDao,
    private val contentPackageDao: ContentPackageDao,
) : ContentRepository {

    override fun observeInstalledPackages(): Flow<List<ContentPackage>> =
        contentPackageDao.observeInstalled().map { list -> list.map { it.toDomain() } }

    override suspend fun getInstalledPackages(): List<ContentPackage> =
        contentPackageDao.getInstalled().map { it.toDomain() }

    override suspend fun getSubjects(grade: Int): List<String> {
        val gradeSubjects = lessonDao.getSubjects(grade)
        val allSubjects = lessonDao.getAllSubjects()
        return (gradeSubjects + allSubjects).distinct().ifEmpty { listOf("Science", "Mathematics") }
    }

    override suspend fun getLessons(subject: String, grade: Int, language: String): List<Lesson> {
        val exact = lessonDao.getForSubject(subject, grade, language).map { it.toDomain() }
        if (exact.isNotEmpty()) return exact
        val anyLang = lessonDao.getForSubjectAnyLanguage(subject, grade).map { it.toDomain() }
        if (anyLang.isNotEmpty()) return anyLang
        return lessonDao.getForSubjectAllGrades(subject).map { it.toDomain() }
    }

    override suspend fun getLesson(lessonId: String): ContentLookupResult {
        val entity = lessonDao.getById(lessonId)
        return if (entity != null) {
            ContentLookupResult.Available(entity.toDomain())
        } else {
            ContentLookupResult.NotAvailableLocally(packageId = lessonId)
        }
    }

    override suspend fun removePackage(packageId: String) {
        lessonDao.deleteForPackage(packageId)
        contentPackageDao.deletePackage(packageId)
    }

    override suspend fun registerPackage(contentPackage: ContentPackage) {
        contentPackageDao.upsert(contentPackage.copy(state = ContentPackageState.INSTALLED).toEntity())
    }
}
