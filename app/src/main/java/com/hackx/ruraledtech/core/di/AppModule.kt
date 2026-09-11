package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.DefaultDispatcherProvider
import com.hackx.ruraledtech.core.common.DispatcherProvider
import com.hackx.ruraledtech.core.common.SystemAppClock
import com.hackx.ruraledtech.core.connectivity.AndroidConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindAppClock(impl: SystemAppClock): AppClock

    companion object {
        @Provides
        @Singleton
        fun provideApplicationScope(dispatcherProvider: DispatcherProvider): CoroutineScope =
            CoroutineScope(SupervisorJob() + dispatcherProvider.default)
    }
}
