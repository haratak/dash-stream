package dev.personalaa.installer.github

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class GithubReleaseClient(
    private val token: String? = null,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun fetchLatestRelease(owner: String, repo: String): GithubRelease {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .apply {
                token?.let { header("Authorization", "Bearer $it") }
            }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub API ${response.code}: ${response.message}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response")
            return json.decodeFromString(GithubRelease.serializer(), body)
        }
    }

    fun pickApkAsset(release: GithubRelease, preferredNameContains: String = "tube"): GithubAsset? {
        return release.assets.firstOrNull { it.name.contains(preferredNameContains, ignoreCase = true) && it.name.endsWith(".apk") }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
    }

    fun downloadAsset(asset: GithubAsset, destination: File, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        val request = Request.Builder()
            .url(asset.browserDownloadUrl)
            .apply {
                token?.let { header("Authorization", "Bearer $it") }
            }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty body")
            val total = body.contentLength().coerceAtLeast(asset.size)
            destination.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        }
    }
}
