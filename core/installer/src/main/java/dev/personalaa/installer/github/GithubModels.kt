package dev.personalaa.installer.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
)

data class CatalogEntry(
    val id: String,
    val displayName: String,
    val repoOwner: String,
    val repoName: String,
    val apkNamePattern: String = ".apk",
)
