package com.btx.tvlauncher

import android.graphics.drawable.GradientDrawable

/** Vordefinierte Hintergrundverläufe. */
object Backgrounds {
    private val presets = listOf(
        intArrayOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt()),
        intArrayOf(0xFF141E30.toInt(), 0xFF243B55.toInt()),
        intArrayOf(0xFF2B0A3D.toInt(), 0xFF5B1A6B.toInt(), 0xFF1B1B3A.toInt()),
        intArrayOf(0xFF0B3D2E.toInt(), 0xFF145A32.toInt()),
        intArrayOf(0xFF3A0D0D.toInt(), 0xFF6B1F1F.toInt(), 0xFF1A1A1A.toInt()),
        intArrayOf(0xFF000000.toInt(), 0xFF1C1C1C.toInt())
    )

    val count: Int get() = presets.size

    fun drawable(index: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, presets[index.mod(presets.size)])
}
