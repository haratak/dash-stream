package dev.personalaa.installer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

/**
 * Installs via shell `pm install -i com.android.vending` when Shizuku is available.
 */
class ShizukuInstaller(
    @Suppress("unused") private val context: Context,
) : AppInstaller {
    override suspend fun install(apkPath: String, asPlayStore: Boolean): InstallResult =
        withContext(Dispatchers.IO) {
            if (!Shizuku.pingBinder()) {
                return@withContext InstallResult.Failure("Shizuku is not running")
            }
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@withContext InstallResult.Failure("Shizuku permission not granted")
            }
            val file = File(apkPath)
            if (!file.exists()) {
                return@withContext InstallResult.Failure("APK not found: $apkPath")
            }
            val cmd = buildList {
                add("pm")
                add("install")
                add("-r")
                if (asPlayStore) {
                    add("-i")
                    add("com.android.vending")
                }
                add(file.absolutePath)
            }.toTypedArray()
            runCatching {
                val process = shizukuNewProcess(cmd)
                val code = process.waitFor()
                val err = process.errorStream.bufferedReader().readText()
                val out = process.inputStream.bufferedReader().readText()
                if (code == 0) {
                    InstallResult.Success
                } else {
                    InstallResult.Failure((err + out).ifBlank { "pm install exited with $code" })
                }
            }.getOrElse {
                InstallResult.Failure(it.message ?: "Shizuku install failed")
            }
        }

    fun isAvailable(): Boolean = Shizuku.pingBinder()

    private fun shizukuNewProcess(cmd: Array<String>): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, cmd, null, null) as Process
    }
}
