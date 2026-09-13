package com.hackx.ruraledtech.domain.usecase.content

import com.hackx.ruraledtech.domain.model.ContentLookupResult
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.Lesson
import com.hackx.ruraledtech.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubjectsUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(grade: Int, learnerId: String): List<String> = repository.getSubjects(grade, learnerId)
}

class GetLessonsUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(subject: String, grade: Int, language: String, learnerId: String): List<Lesson> =
        repository.getLessons(subject, grade, language, learnerId)
}

class GetLessonUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(lessonId: String): ContentLookupResult = repository.getLesson(lessonId)
}

class ObserveInstalledPackagesUseCase @Inject constructor(private val repository: ContentRepository) {
    operator fun invoke(learnerId: String): Flow<List<ContentPackage>> = repository.observeInstalledPackages(learnerId)
}

class RemoveContentPackageUseCase @Inject constructor(private val repository: ContentRepository) {
    suspend operator fun invoke(packageId: String) = repository.removePackage(packageId)
}
