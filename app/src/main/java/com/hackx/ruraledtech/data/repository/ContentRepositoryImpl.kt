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

    override suspend fun getSubjects(grade: Int): List<String> = lessonDao.getSubjects(grade)

    override suspend fun getLessons(subject: String, grade: Int, language: String): List<Lesson> =
        lessonDao.getForSubject(subject, grade, language).map { it.toDomain() }

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
