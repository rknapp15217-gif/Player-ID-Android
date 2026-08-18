package com.playerid.app.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

fun performStrongMicPressHaptic(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createWaveform(longArrayOf(0L, 55L, 35L, 70L), -1)
    } else {
        @Suppress("DEPRECATION")
        VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    vibrator.vibrate(effect)
}

fun performRecordButtonPressHaptic(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
    } else {
        @Suppress("DEPRECATION")
        VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    vibrator.vibrate(effect)
}

fun performRecordingCapturedDoubleHaptic(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createWaveform(longArrayOf(0L, 38L, 60L, 38L), -1)
    } else {
        @Suppress("DEPRECATION")
        VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    vibrator.vibrate(effect)
}