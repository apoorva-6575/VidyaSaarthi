package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.data.contentpackage.ContentInstallerImpl
import com.hackx.ruraledtech.data.contentpackage.LocalContentAvailabilityProvider
import com.hackx.ruraledtech.data.engine.AdaptiveLearningEngineImpl
import com.hackx.ruraledtech.data.engine.ConceptGraphResolverImpl
import com.hackx.ruraledtech.data.engine.DefaultAnswerEvaluator
import com.hackx.ruraledtech.data.engine.DefaultMasteryCalculator
import com.hackx.ruraledtech.data.engine.DefaultRecommendationEngine
import com.hackx.ruraledtech.data.mock.MockOfflineSpeechRecognizer
import com.hackx.ruraledtech.data.passport.LearnerDataExporterImpl
import com.hackx.ruraledtech.data.passport.LearnerDataImporterImpl
import com.hackx.ruraledtech.data.voice.AndroidTextToSpeechEngine
import com.hackx.ruraledtech.data.voice.DefaultVoiceIntentParser
import com.hackx.ruraledtech.domain.engine.AnswerEvaluator
import com.hackx.ruraledtech.domain.engine.ConceptGraphResolver
import com.hackx.ruraledtech.domain.engine.MasteryCalculator
import com.hackx.ruraledtech.domain.engine.RecommendationEngine
import com.hackx.ruraledtech.domain.integration.ContentAvailabilityProvider
import com.hackx.ruraledtech.domain.integration.ContentInstaller
import com.hackx.ruraledtech.domain.integration.LearnerDataExporter
import com.hackx.ruraledtech.domain.integration.LearnerDataImporter
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.integration.OfflineSpeechRecognizer
import com.hackx.ruraledtech.domain.multilingual.DefaultLanguageResolver
import com.hackx.ruraledtech.domain.multilingual.LanguageResolver
import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine
import com.hackx.ruraledtech.domain.voice.VoiceIntentParser
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
    abstract fun bindAnswerEvaluator(impl: DefaultAnswerEvaluator): AnswerEvaluator

    @Binds
    @Singleton
    abstract fun bindMasteryCalculator(impl: DefaultMasteryCalculator): MasteryCalculator

    @Binds
    @Singleton
    abstract fun bindConceptGraphResolver(impl: ConceptGraphResolverImpl): ConceptGraphResolver

    @Binds
    @Singleton
    abstract fun bindRecommendationEngine(impl: DefaultRecommendationEngine): RecommendationEngine

    @Binds
    @Singleton
    abstract fun bindLearningEngine(impl: AdaptiveLearningEngineImpl): LearningEngine

    @Binds
    @Singleton
    abstract fun bindContentAvailabilityProvider(impl: LocalContentAvailabilityProvider): ContentAvailabilityProvider

    @Binds
    @Singleton
    abstract fun bindLanguageResolver(impl: DefaultLanguageResolver): LanguageResolver

    @Binds
    @Singleton
    abstract fun bindVoiceIntentParser(impl: DefaultVoiceIntentParser): VoiceIntentParser

    @Binds
    @Singleton
    abstract fun bindTextToSpeechEngine(impl: AndroidTextToSpeechEngine): TextToSpeechEngine

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
