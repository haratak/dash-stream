package dev.personalaa.tube

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.personalaa.tube.car.TubeIntentHandler
import dev.personalaa.tube.data.TubePreferences
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TubeApplication
        val url = TubeIntentHandler.extractUrl(intent)
        if (url == null) {
            Toast.makeText(this, "YouTube URL を認識できませんでした", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        app.webViewController.loadUrl(url)
        lifecycleScope.launch {
            TubePreferences(this@ShareReceiverActivity).setPendingUrl(url)
        }
        Toast.makeText(this, "車載画面で開きます: $url", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}
