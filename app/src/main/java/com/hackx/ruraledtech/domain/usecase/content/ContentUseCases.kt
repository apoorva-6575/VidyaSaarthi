package com.hackx.ruraledtech.domain.usecase.content

import com.hackx.ruraledtech.domain.model.ContentLookupResult
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.Lesson
import com.hackx.ruraledtech.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubjectsUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(grade: Int): List<String> = repository.getSubjects(grade)
}

class GetLessonsUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(subject: String, grade: Int, language: String): List<Lesson> =
        repository.getLessons(subject, grade, language)
}

class GetLessonUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(lessonId: String): ContentLookupResult = repository.getLesson(lessonId)
}

class ObserveInstalledPackagesUseCase @Inject constructor(private val repository: ContentRepository) {
    operator fun invoke(): Flow<List<ContentPackage>> = repository.observeInstalledPackages()
}

class RemoveContentPackageUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(packageId: String) = repository.removePackage(packageId)
}
