package dev.personalaa.tube

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.personalaa.tube.data.TubePreferences
import dev.personalaa.tube.youtube.YouTubeUrlParser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferences: TubePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TubeApplication
        preferences = TubePreferences(this)

        setContent {
            val zoom by preferences.zoomScale.collectAsState(initial = 1.1f)
            var slider by mutableFloatStateOf(zoom)
            val scope = rememberCoroutineScope()

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = getString(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
                    Text(text = getString(R.string.settings_subtitle))
                    Text(text = getString(R.string.share_hint))
                    Text(text = "ズーム: ${"%.1f".format(slider)}x")
                    Slider(
                        value = slider,
                        onValueChange = { slider = it },
                        onValueChangeFinished = {
                            scope.launch {
                                preferences.setZoom(slider)
                                app.webViewController.setZoom(slider)
                            }
                        },
                        valueRange = 0.8f..1.6f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(YouTubeUrlParser.normalize(null))),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(getString(R.string.open_youtube_phone))
                    }
                }
            }
        }
    }
}
