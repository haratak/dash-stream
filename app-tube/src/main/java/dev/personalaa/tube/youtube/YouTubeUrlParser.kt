package dev.personalaa.tube.youtube

import java.net.URI

object YouTubeUrlParser {
    private const val MOBILE_HOME = "https://m.youtube.com/"

    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return MOBILE_HOME
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> toMobile(trimmed)
            trimmed.startsWith("www.") -> toMobile("https://$trimmed")
            else -> MOBILE_HOME
        }
    }

    fun toMobile(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return MOBILE_HOME
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()

        return when {
            host == "youtu.be" -> {
                val id = path.trim('/').substringBefore('/')
                if (id.isNotEmpty()) "https://m.youtube.com/watch?v=$id" else MOBILE_HOME
            }
            host.contains("youtube.com") -> {
                when {
                    path.startsWith("/watch") -> "https://m.youtube.com$path$query"
                    path.startsWith("/shorts/") -> {
                        val id = path.removePrefix("/shorts/").substringBefore('/')
                        "https://m.youtube.com/watch?v=$id"
                    }
                    else -> "https://m.youtube.com$path$query".ifBlank { MOBILE_HOME }
                }
            }
            else -> url
        }
    }

    fun searchUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        return "https://m.youtube.com/results?search_query=$encoded"
    }
}
