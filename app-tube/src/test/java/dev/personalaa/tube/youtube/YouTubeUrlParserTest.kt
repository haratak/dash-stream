package dev.personalaa.tube.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeUrlParserTest {
    @Test
    fun youtuBeShortLink() {
        val url = YouTubeUrlParser.normalize("https://youtu.be/dQw4w9WgXcQ")
        assertEquals("https://m.youtube.com/watch?v=dQw4w9WgXcQ", url)
    }

    @Test
    fun desktopWatchLink() {
        val url = YouTubeUrlParser.normalize("https://www.youtube.com/watch?v=abc123&t=10")
        assertEquals("https://m.youtube.com/watch?v=abc123&t=10", url)
    }

    @Test
    fun searchUrl() {
        assertEquals(
            "https://m.youtube.com/results?search_query=android+auto",
            YouTubeUrlParser.searchUrl("android auto"),
        )
    }
}
