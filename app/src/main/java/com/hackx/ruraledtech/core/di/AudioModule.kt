package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.core.audio.AndroidAudioManager
import com.hackx.ruraledtech.core.audio.AudioManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindAudioManager(impl: AndroidAudioManager): AudioManager
}
