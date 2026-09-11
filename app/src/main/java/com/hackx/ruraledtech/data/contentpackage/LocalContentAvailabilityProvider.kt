package com.hackx.ruraledtech.data.contentpackage

import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.domain.integration.ContentAvailabilityProvider
import com.hackx.ruraledtech.domain.model.ContentPackageState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ContentAvailabilityProvider] backed by local Room database.
 */
@Singleton
class LocalContentAvailabilityProvider @Inject constructor(
    private val contentPackageDao: ContentPackageDao,
) : ContentAvailabilityProvider {

    override suspend fun isPackageAvailable(packageId: String): Boolean {
        val installed = contentPackageDao.getInstalled()
        return installed.any { it.packageId == packageId && it.state == ContentPackageState.INSTALLED.name }
    }

    override suspend fun getAvailablePackageIds(): List<String> {
        return contentPackageDao.getInstalled()
            .filter { it.state == ContentPackageState.INSTALLED.name }
            .map { it.packageId }
    }
}
