package dev.personalaa.installer

sealed class InstallResult {
    data object Success : InstallResult()
    data class Failure(val message: String) : InstallResult()
}

interface AppInstaller {
    suspend fun install(apkPath: String, asPlayStore: Boolean): InstallResult
}
