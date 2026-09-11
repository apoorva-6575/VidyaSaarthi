package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.data.repository.ContentRepositoryImpl
import com.hackx.ruraledtech.data.repository.LearnerRepositoryImpl
import com.hackx.ruraledtech.data.repository.ProgressRepositoryImpl
import com.hackx.ruraledtech.data.repository.QuizRepositoryImpl
import com.hackx.ruraledtech.data.repository.RecommendationRepositoryImpl
import com.hackx.ruraledtech.data.repository.SyncRepositoryImpl
import com.hackx.ruraledtech.domain.repository.ContentRepository
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.QuizRepository
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLearnerRepository(impl: LearnerRepositoryImpl): LearnerRepository

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(impl: RecommendationRepositoryImpl): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
