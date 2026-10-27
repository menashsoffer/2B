package com.menash.myself.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** The only spacing steps: 4, 8, 16, 24, 32. */
object Spacing {
    val s4 = 4.dp
    val s8 = 8.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp

    val xxs = s4
    val xs = s8
    /** Was 12.dp. Snapped onto the scale so the steps stay these five. */
    val sm = s8
    val md = s16
    val lg = s24
    val xl = s32
}
