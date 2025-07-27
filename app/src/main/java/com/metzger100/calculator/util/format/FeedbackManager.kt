package com.metzger100.calculator.util.format

import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class FeedbackManager private constructor(
    private val audioManager: AudioManager,
    val prefs: FeedbackPrefs
) {
    // Non-composable access to preferences
    suspend fun toggleHaptic() {
        prefs.setHapticEnabled(!prefs.isHapticEnabled())
    }

    suspend fun toggleSound() {
        prefs.setSoundEnabled(!prefs.isSoundEnabled())
    }

    fun provideFeedback(view: View) {
        if (prefs.isHapticEnabled()) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (prefs.isSoundEnabled()) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK,1.0f)
        }
    }

    companion object {
        @Composable
        fun rememberFeedbackManager(): FeedbackManager {
            val context = LocalContext.current
            val audioManager = remember {
                context.getSystemService(AudioManager::class.java)
            }
            val prefs = remember { FeedbackPrefs(context) }
            return remember { FeedbackManager(audioManager, prefs) }
        }
    }
}