package com.hackx.ruraledtech.core.di

import android.content.Context
import com.hackx.ruraledtech.p2p.connection.NearbyConnectionManagerImpl
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.manifest.ManifestReconciler
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.mesh.LearningMeshImpl
import com.hackx.ruraledtech.p2p.mesh.MeshController
import com.hackx.ruraledtech.p2p.transfer.TransferManager
import com.hackx.ruraledtech.domain.integration.ContentInstaller
import com.hackx.ruraledtech.p2p.passport.transport.PassportManager
import com.hackx.ruraledtech.p2p.integration.LearnerDataExporter
import com.hackx.ruraledtech.p2p.integration.LearnerDataImporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object P2PModule {

    @Provides
    @Singleton
    fun provideConnectionManager(@ApplicationContext context: Context): P2PConnectionManager {
        return NearbyConnectionManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideTransferManager(
        connectionManager: P2PConnectionManager,
        contentInstaller: ContentInstaller
    ): TransferManager {
        return TransferManager(connectionManager, contentInstaller)
    }

    @Provides
    @Singleton
    fun providePassportManager(
        exporter: LearnerDataExporter,
        importer: LearnerDataImporter,
        connectionManager: P2PConnectionManager
    ): PassportManager {
        return PassportManager(exporter, importer, connectionManager)
    }

    @Provides
    @Singleton
    fun provideMeshController(
        connectionManager: P2PConnectionManager,
        reconciler: ManifestReconciler,
        transferManager: TransferManager,
        passportManager: PassportManager
    ): MeshController {
        val controller = MeshController(connectionManager, reconciler, transferManager)
        connectionManager.setListener(controller)
        controller.setPassportManager(passportManager)
        return controller
    }

    @Provides
    @Singleton
    fun provideLearningMesh(
        connectionManager: P2PConnectionManager,
        meshController: MeshController
    ): LearningMesh {
        return LearningMeshImpl(connectionManager, meshController)
    }
}
