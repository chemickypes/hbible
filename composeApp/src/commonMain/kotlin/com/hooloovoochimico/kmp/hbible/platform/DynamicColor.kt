package com.hooloovoochimico.kmp.hbible.platform

import androidx.compose.material3.ColorScheme

/** True when the platform can build a dynamic (Material You) color scheme. */
expect fun isDynamicColorSupported(): Boolean

/**
 * Dynamic color scheme from the platform wallpaper/accent, or null when
 * unsupported (the theme falls back to the static palette).
 */
expect fun dynamicColorScheme(dark: Boolean): ColorScheme?
