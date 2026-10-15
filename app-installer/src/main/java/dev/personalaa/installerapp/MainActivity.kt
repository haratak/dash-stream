package dev.personalaa.installerapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.personalaa.installer.InstallResult
import dev.personalaa.installer.SessionInstaller
import dev.personalaa.installer.ShizukuInstaller
import dev.personalaa.installer.github.GithubReleaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            var githubToken by rememberSaveable { mutableStateOf("") }
            var status by rememberSaveable { mutableStateOf("AA Tube の最新 APK を GitHub Releases から取得してインストールします。") }
            var busy by rememberSaveable { mutableStateOf(false) }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("AA Installer", style = MaterialTheme.typography.headlineMedium)
                    Text(status)
                    OutlinedTextField(
                        value = githubToken,
                        onValueChange = { githubToken = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("GitHub token (任意)") },
                        singleLine = true,
                    )
                    if (busy) {
                        CircularProgressIndicator()
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                status = "Release を確認中…"
                                val result = runInstall(
                                    token = githubToken.ifBlank { null },
                                    asPlayStore = Shizuku.pingBinder(),
                                )
                                status = when (result) {
                                    is InstallResult.Success -> "インストール完了。Android Auto で AA Tube を確認してください。"
                                    is InstallResult.Failure -> "失敗: ${result.message}"
                                }
                                busy = false
                                Toast.makeText(this@MainActivity, status, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("AA Tube をインストール / 更新")
                    }
                    Text(
                        "Shizuku が動いていれば Play ストア名義でインストールします。" +
                            " なければ通常インストール後、Android Auto の「提供元不明のアプリ」を有効にしてください。",
                    )
                }
            }
        }
    }

    private suspend fun runInstall(token: String?, asPlayStore: Boolean): InstallResult =
        withContext(Dispatchers.IO) {
            val client = GithubReleaseClient(token)
            val owner = BuildConfig.TUBE_REPO_OWNER
            val repo = BuildConfig.TUBE_REPO_NAME
            if (owner == "YOUR_GITHUB_USER" || repo == "YOUR_REPO_NAME") {
                return@withContext InstallResult.Failure(
                    "app-installer/build.gradle.kts の TUBE_REPO_OWNER / TUBE_REPO_NAME を自分の GitHub リポジトリに変更してください。",
                )
            }
            runCatching {
                val release = client.fetchLatestRelease(owner, repo)
                val asset = client.pickApkAsset(release, preferredNameContains = "tube")
                    ?: return@withContext InstallResult.Failure("Release に APK が見つかりません")
                val dest = File(cacheDir, asset.name)
                client.downloadAsset(asset, dest) { _, _ -> }
                if (asPlayStore) {
                    ShizukuInstaller(this@MainActivity).install(dest.absolutePath, asPlayStore = true)
                } else {
                    SessionInstaller(this@MainActivity).install(dest.absolutePath, asPlayStore = false)
                }
            }.getOrElse {
                InstallResult.Failure(it.message ?: "unknown error")
            }
        }
}
