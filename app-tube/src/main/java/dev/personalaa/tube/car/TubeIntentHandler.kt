package dev.personalaa.tube.car

import android.content.Intent
import dev.personalaa.tube.TubeApplication
import dev.personalaa.tube.youtube.YouTubeUrlParser

object TubeIntentHandler {
    fun handle(app: TubeApplication, intent: Intent?) {
        val url = extractUrl(intent) ?: return
        app.webViewController.loadUrl(url)
    }

    fun extractUrl(intent: Intent?): String? {
        if (intent == null) return null
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
                return YouTubeUrlParser.normalize(text)
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()?.let { return YouTubeUrlParser.normalize(it) }
            }
            TubeNavigationScreen.ACTION_SEARCH -> {
                val query = intent.getStringExtra(TubeNavigationScreen.EXTRA_QUERY) ?: return null
                return YouTubeUrlParser.searchUrl(query)
            }
        }
        return null
    }
}
