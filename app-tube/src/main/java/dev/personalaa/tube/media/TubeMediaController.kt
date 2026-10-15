package dev.personalaa.tube.media

import android.content.Context
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.personalaa.tube.web.YouTubeWebViewController

@UnstableApi
class TubeMediaController(
    context: Context,
    webViewController: YouTubeWebViewController,
) {
    private val appContext = context.applicationContext
    private val player = WebBridgePlayer(appContext.mainLooper, webViewController)
    private var session: MediaSession? = null

    fun ensureSession(): MediaSession {
        return session ?: MediaSession.Builder(appContext, player)
            .setId("aa-tube-session")
            .build()
            .also { session = it }
    }

    fun release() {
        session?.release()
        session = null
        player.release()
    }

    @UnstableApi
    private class WebBridgePlayer(
        looper: Looper,
        private val web: YouTubeWebViewController,
    ) : SimpleBasePlayer(looper) {
        private var playWhenReady = true

        override fun getState(): State {
            return State.Builder()
                .setAvailableCommands(
                    Player.Commands.Builder()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_STOP)
                        .build(),
                )
                .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaybackState(STATE_READY)
                .build()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            this.playWhenReady = playWhenReady
            if (playWhenReady) {
                web.play()
            } else {
                web.pause()
            }
            return Futures.immediateVoidFuture()
        }

        override fun handleStop(): ListenableFuture<*> {
            playWhenReady = false
            web.pause()
            return Futures.immediateVoidFuture()
        }
    }
}
