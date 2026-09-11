package com.hackx.ruraledtech.core.di

import com.hackx.ruraledtech.data.mock.MockLearningMesh
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bound to [MockLearningMesh] so Group 1's UI can integrate against [LearningMesh] before
 * Group 3's real Nearby Connections mesh exists. Swap the binding target when that lands —
 * no UI call site should need to change, matching the pattern in [IntegrationModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class P2PModule {

    @Binds
    @Singleton
    abstract fun bindLearningMesh(impl: MockLearningMesh): LearningMesh
}
