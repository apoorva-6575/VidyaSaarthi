package com.hackx.ruraledtech.core.device

import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device id is distinct from learner id (PS section 31) — one physical device hosts many
 * learners, and Groups 3/4 key P2P peers and sync events off this rather than any learner.
 */
@Singleton
class DeviceIdProvider @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val idGenerator: IdGenerator,
) {
    private val mutex = Mutex()
    private var cached: String? = null

    suspend fun get(): String {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: preferencesManager.getOrCreateDeviceId { idGenerator.deviceId() }.also { cached = it }
        }
    }
}
