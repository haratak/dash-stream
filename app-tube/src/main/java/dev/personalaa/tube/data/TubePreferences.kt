package dev.personalaa.tube.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tube_prefs")

class TubePreferences(private val context: Context) {
    val zoomScale: Flow<Float> = context.dataStore.data.map { it[KEY_ZOOM] ?: 1.1f }
    val pendingUrl: Flow<String?> = context.dataStore.data.map { it[KEY_PENDING_URL] }

    suspend fun setZoom(scale: Float) {
        context.dataStore.edit { it[KEY_ZOOM] = scale.coerceIn(0.8f, 1.6f) }
    }

    suspend fun setPendingUrl(url: String?) {
        context.dataStore.edit {
            if (url.isNullOrBlank()) {
                it.remove(KEY_PENDING_URL)
            } else {
                it[KEY_PENDING_URL] = url
            }
        }
    }

    companion object {
        private val KEY_ZOOM = floatPreferencesKey("zoom_scale")
        private val KEY_PENDING_URL = stringPreferencesKey("pending_url")
    }
}
