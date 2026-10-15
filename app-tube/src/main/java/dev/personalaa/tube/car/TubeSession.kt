package dev.personalaa.tube.car

import android.content.Intent
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.personalaa.tube.TubeApplication
import dev.personalaa.tube.data.TubePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TubeSession(
    private val app: TubeApplication,
) : Session() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = TubePreferences(app)

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    app.mediaController.ensureSession()
                    scope.launch {
                        preferences.pendingUrl.first()?.let { url ->
                            app.webViewController.loadUrl(url)
                            preferences.setPendingUrl(null)
                        }
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    scope.cancel()
                }
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        TubeIntentHandler.handle(app, intent)
    }

    override fun onCreateScreen(intent: Intent) = TubeNavigationScreen(carContext, app)
}
