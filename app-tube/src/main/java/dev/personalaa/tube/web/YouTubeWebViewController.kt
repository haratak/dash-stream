package dev.personalaa.tube.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.personalaa.tube.youtube.YouTubeUrlParser

class YouTubeWebViewController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var zoomScale = 1.1f

    @SuppressLint("SetJavaScriptEnabled")
    fun obtainView(): WebView {
        webView?.let { return it }
        CookieManager.getInstance().setAcceptCookie(true)
        return WebView(appContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = MOBILE_USER_AGENT
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = WebViewClient()
            webView = this
            loadUrl(YouTubeUrlParser.normalize(null))
        }
    }

    fun setZoom(scale: Float) {
        zoomScale = scale
        mainHandler.post {
            webView?.settings?.textZoom = (scale * 100).toInt()
        }
    }

    fun loadUrl(url: String) {
        val normalized = YouTubeUrlParser.normalize(url)
        mainHandler.post {
            obtainView().loadUrl(normalized)
        }
    }

    fun goBack() {
        mainHandler.post {
            webView?.takeIf { it.canGoBack() }?.goBack()
        }
    }

    fun goHome() {
        loadUrl(YouTubeUrlParser.normalize(null))
    }

    fun togglePlayPause() {
        mainHandler.post {
            webView?.evaluateJavascript(
                """
                (function() {
                  var v = document.querySelector('video');
                  if (!v) return 'no-video';
                  if (v.paused) { v.play(); return 'play'; }
                  v.pause(); return 'pause';
                })();
                """.trimIndent(),
                null,
            )
        }
    }

    fun play() = mainHandler.post {
        webView?.evaluateJavascript("document.querySelector('video')?.play();", null)
    }

    fun pause() = mainHandler.post {
        webView?.evaluateJavascript("document.querySelector('video')?.pause();", null)
    }

    companion object {
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
