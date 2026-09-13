package com.hackx.ruraledtech.domain.usecase.mesh

import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.MaterialRequestDao
import com.hackx.ruraledtech.data.local.entities.MaterialRequestEntity
import com.hackx.ruraledtech.p2p.mesh.MeshController
import com.hackx.ruraledtech.p2p.protocol.P2PMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class ClassroomMeshCoordinator @Inject constructor(
    private val meshController: MeshController,
    private val contentPackageDao: ContentPackageDao,
    private val materialRequestDao: MaterialRequestDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _incomingOffers = MutableSharedFlow<P2PMessage.ClassMaterialOffer>()
    val incomingOffers = _incomingOffers.asSharedFlow()

    init {
        scope.launch {
            meshController.incomingMessages.collect { message ->
                when (message) {
                    is P2PMessage.ClassMaterialOffer -> scope.launch { handleMaterialOffer(message) }
                    is P2PMessage.ClassMaterialRequest -> scope.launch { handleMaterialRequest(message) }
                    is P2PMessage.MaterialRequestDecision -> scope.launch { handleRequestDecision(message) }
                    else -> {} // Handled by other systems
                }
            }
        }
    }

    private suspend fun handleMaterialOffer(offer: P2PMessage.ClassMaterialOffer) {
        val hasContent = contentPackageDao.getLatest(offer.packageId) != null
        if (!hasContent) {
            _incomingOffers.emit(offer)
        }
    }

    private suspend fun handleMaterialRequest(request: P2PMessage.ClassMaterialRequest) {
        val hasContent = contentPackageDao.getLatest(request.packageId) != null // simplistic version check
        if (hasContent) {
            val entity = MaterialRequestEntity(
                requestId = request.requestId,
                packageId = request.packageId,
                requesterId = request.requesterId,
                providerId = null,
                status = "PENDING",
                isIncoming = true,
                lastUpdated = System.currentTimeMillis()
            )
            materialRequestDao.insert(entity)
        }
    }

    private suspend fun handleRequestDecision(decision: P2PMessage.MaterialRequestDecision) {
        val request = materialRequestDao.getById(decision.requestId)
        if (request != null && !request.isIncoming) {
            materialRequestDao.updateStatus(
                decision.requestId, 
                if (decision.approved) "APPROVED" else "REJECTED"
            )
            if (decision.approved) {
                meshController.broadcastRequest(request.packageId, 1) // simplisitic version
            }
        }
    }
    
    suspend fun acceptOffer(offer: P2PMessage.ClassMaterialOffer) {
        meshController.broadcastRequest(offer.packageId, offer.version)
    }

    fun broadcastOffer(packageId: String, version: Int, subject: String, sizeBytes: Long) {
        val message = P2PMessage.ClassMaterialOffer(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = meshController.localEndpointId,
            timestamp = System.currentTimeMillis(),
            packageId = packageId,
            version = version,
            subject = subject,
            sizeBytes = sizeBytes
        )
        meshController.broadcastMessage(message)
    }

    suspend fun requestFromPeers(packageId: String, requesterId: String) {
        val requestId = UUID.randomUUID().toString()
        val requestEntity = MaterialRequestEntity(
            requestId = requestId,
            packageId = packageId,
            requesterId = requesterId,
            providerId = null,
            status = "PENDING",
            isIncoming = false,
            lastUpdated = System.currentTimeMillis()
        )
        materialRequestDao.insert(requestEntity)
        
        val message = P2PMessage.ClassMaterialRequest(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = meshController.localEndpointId,
            timestamp = System.currentTimeMillis(),
            requestId = requestId,
            packageId = packageId,
            requesterId = requesterId
        )
        meshController.broadcastMessage(message)
    }

    suspend fun approveRequest(requestId: String) {
        val request = materialRequestDao.getById(requestId) ?: return
        materialRequestDao.updateStatus(requestId, "APPROVED")
        val message = P2PMessage.MaterialRequestDecision(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = meshController.localEndpointId,
            timestamp = System.currentTimeMillis(),
            requestId = requestId,
            approved = true
        )
        meshController.broadcastMessage(message)
    }
    
    suspend fun declineRequest(requestId: String) {
        val request = materialRequestDao.getById(requestId) ?: return
        materialRequestDao.updateStatus(requestId, "REJECTED")
        val message = P2PMessage.MaterialRequestDecision(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = meshController.localEndpointId,
            timestamp = System.currentTimeMillis(),
            requestId = requestId,
            approved = false
        )
        meshController.broadcastMessage(message)
    }
}
