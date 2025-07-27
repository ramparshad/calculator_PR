package com.metzger100.calculator.util

import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class FeedbackManager private constructor(
    private val context: Context
) {

    fun provideFeedback(view: View, haptic: Boolean = true, sound: Boolean = true) {
        if (haptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (sound && areTouchSoundsEnabled()) view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    private fun areTouchSoundsEnabled(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SOUND_EFFECTS_ENABLED,
            /* default */ 1
        ) == 1
    }

    companion object {
        @Composable
        fun rememberFeedbackManager(): FeedbackManager {
            val context = LocalContext.current
            return remember { FeedbackManager(context) }
        }
    }
}