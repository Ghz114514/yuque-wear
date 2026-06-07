package com.google.wear.input

import android.view.HapticFeedbackConstants

/**
 * COMPATIBILITY STUB — DO NOT REMOVE.
 *
 * Wear Compose 1.4's ScalingLazyColumn enables rotary haptics that call
 * `com.google.wear.input.WearHapticFeedbackConstants.getScroll*()` — a class that only exists
 * in the real Wear OS system image. The Xiaomi Watch 5 (HyperOS) reports as Android 14 but is
 * NOT Wear OS, so the class is absent and the app crashes with NoClassDefFoundError the moment
 * a ScalingLazyColumn composes.
 *
 * This object recreates the three static methods the library reads (verified via javap on
 * compose-foundation-1.4.1.aar). [ENABLED] is set from YuqueApp at startup; when off we return
 * an invalid feedback id (-1) so performHapticFeedback no-ops — no crown vibration. Because the
 * library caches these values once, toggling takes effect on the next app launch.
 */
object WearHapticFeedbackConstants {

    @JvmField
    var ENABLED: Boolean = true

    private const val OFF = -1

    @JvmStatic
    fun getScrollTick(): Int = if (ENABLED) HapticFeedbackConstants.CLOCK_TICK else OFF

    @JvmStatic
    fun getScrollItemFocus(): Int = if (ENABLED) HapticFeedbackConstants.CLOCK_TICK else OFF

    @JvmStatic
    fun getScrollLimit(): Int = if (ENABLED) HapticFeedbackConstants.LONG_PRESS else OFF
}
