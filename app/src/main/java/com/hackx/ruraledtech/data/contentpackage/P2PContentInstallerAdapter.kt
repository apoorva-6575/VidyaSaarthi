package com.hackx.ruraledtech.data.contentpackage

import com.hackx.ruraledtech.domain.integration.InstallResult
import javax.inject.Inject

/**
 * Bridges Group 3's simpler [com.hackx.ruraledtech.p2p.integration.ContentInstaller] (used
 * by [com.hackx.ruraledtech.core.work.ContentUpdateWorker] and
 * [com.hackx.ruraledtech.p2p.transfer.TransferManager]) onto Group 1's real
 * [com.hackx.ruraledtech.domain.integration.ContentInstaller] (backed by
 * [ContentInstallerImpl], which does the actual checksum verification and Room writes).
 *
 * Two interfaces exist for the same job because they were defined independently by two
 * groups before either side saw the other's work — see INTEGRATION.md. This adapter is the
 * minimal fix to keep everyone's code compiling against whichever one they already wrote
 * against; the two should probably be reconciled into a single interface when there's time.
 */
class P2PContentInstallerAdapter @Inject constructor(
    private val delegate: com.hackx.ruraledtech.domain.integration.ContentInstaller,
) : com.hackx.ruraledtech.p2p.integration.ContentInstaller {

    override suspend fun install(packagePath: String): Boolean =
        delegate.install(packagePath) is InstallResult.Success
}
