package dev.personalaa.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Standard PackageInstaller session. Does not spoof Play Store installer name.
 */
class SessionInstaller(
    private val context: Context,
) : AppInstaller {
    override suspend fun install(apkPath: String, asPlayStore: Boolean): InstallResult {
        if (asPlayStore) {
            return InstallResult.Failure(
                "Play Store installer name requires Shizuku or adb. Use ShizukuInstaller or: adb install -i com.android.vending",
            )
        }
        val file = File(apkPath)
        if (!file.exists()) {
            return InstallResult.Failure("APK not found: $apkPath")
        }
        return suspendCancellableCoroutine { cont ->
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = runCatching { installer.createSession(params) }
                .getOrElse {
                    cont.resume(InstallResult.Failure(it.message ?: "createSession failed"))
                    return@suspendCancellableCoroutine
                }
            runCatching {
                installer.openSession(sessionId).use { session ->
                    file.inputStream().use { input ->
                        session.openWrite("base.apk", 0, file.length()).use { out ->
                            input.copyTo(out)
                            session.fsync(out)
                        }
                    }
                    val intent = Intent(ACTION_INSTALL_COMMIT)
                    val pending = PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                    session.commit(pending.intentSender)
                }
                cont.resume(InstallResult.Success)
            }.onFailure {
                cont.resume(InstallResult.Failure(it.message ?: "install failed"))
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_COMMIT = "dev.personalaa.installer.INSTALL_COMMIT"
    }
}
