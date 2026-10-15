package dev.personalaa.tube

import android.app.Application
import dev.personalaa.tube.media.TubeMediaController
import dev.personalaa.tube.web.YouTubeWebViewController

class TubeApplication : Application() {
    lateinit var webViewController: YouTubeWebViewController
        private set

    lateinit var mediaController: TubeMediaController
        private set

    override fun onCreate() {
        super.onCreate()
        webViewController = YouTubeWebViewController(this)
        mediaController = TubeMediaController(this, webViewController)
    }
}
