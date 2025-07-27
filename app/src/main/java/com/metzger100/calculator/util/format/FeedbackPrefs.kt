package com.metzger100.calculator.util.format

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "feedback_settings")

class FeedbackPrefs(private val context: Context) {
    companion object {
        private val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    // Flow for Compose UI
    val hapticEnabled: Flow<Boolean> = context.feedbackDataStore.data
        .map { prefs -> prefs[HAPTIC_ENABLED] ?: true }

    val soundEnabled: Flow<Boolean> = context.feedbackDataStore.data
        .map { prefs -> prefs[SOUND_ENABLED] ?: true }

    // Blocking calls for non-composable contexts
    fun isHapticEnabled(): Boolean = runBlocking {
        hapticEnabled.first()
    }

    fun isSoundEnabled(): Boolean = runBlocking {
        soundEnabled.first()
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.feedbackDataStore.edit { prefs ->
            prefs[HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.feedbackDataStore.edit { prefs ->
            prefs[SOUND_ENABLED] = enabled
        }
    }
}