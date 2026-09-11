package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.data.contentpackage.ContentInstallerImpl
import com.hackx.ruraledtech.data.mock.MockLearningEngine
import com.hackx.ruraledtech.data.mock.MockOfflineSpeechRecognizer
import com.hackx.ruraledtech.data.passport.LearnerDataExporterImpl
import com.hackx.ruraledtech.data.passport.LearnerDataImporterImpl
import com.hackx.ruraledtech.domain.integration.ContentInstaller
import com.hackx.ruraledtech.domain.integration.LearnerDataExporter
import com.hackx.ruraledtech.domain.integration.LearnerDataImporter
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.integration.OfflineSpeechRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Every binding in this file is the seam described in INTEGRATION.md. Swapping a Mock* for
 * a Group 2/3 real implementation is a one-line change here — no other file in the app
 * should ever need to change as a result.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class IntegrationModule {

    @Binds
    @Singleton
    abstract fun bindLearningEngine(impl: MockLearningEngine): LearningEngine

    @Binds
    @Singleton
    abstract fun bindOfflineSpeechRecognizer(impl: MockOfflineSpeechRecognizer): OfflineSpeechRecognizer

    @Binds
    @Singleton
    abstract fun bindContentInstaller(impl: ContentInstallerImpl): ContentInstaller

    @Binds
    @Singleton
    abstract fun bindLearnerDataExporter(impl: LearnerDataExporterImpl): LearnerDataExporter

    @Binds
    @Singleton
    abstract fun bindLearnerDataImporter(impl: LearnerDataImporterImpl): LearnerDataImporter
}
