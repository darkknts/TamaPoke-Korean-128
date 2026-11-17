package com.tamapoke.android

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object SoundFeedback {
    @Volatile private var tone: ToneGenerator? = null

    fun play(context: Context, kind: Int) {
        runCatching {
            val generator = tone ?: synchronized(this) {
                tone ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45).also { tone = it }
            }
            val toneType = when (kind) {
                1 -> ToneGenerator.TONE_PROP_ACK
                2 -> ToneGenerator.TONE_PROP_BEEP2
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            generator.startTone(toneType, 75)
        }
    }

    fun vibrate(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(24)
            }
        }
    }

    fun release() {
        synchronized(this) {
            tone?.release()
            tone = null
        }
    }
}
