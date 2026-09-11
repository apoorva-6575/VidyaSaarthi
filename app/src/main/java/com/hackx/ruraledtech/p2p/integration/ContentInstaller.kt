package com.hackx.ruraledtech.p2p.integration

interface ContentInstaller {
    suspend fun install(packagePath: String): Boolean
}
