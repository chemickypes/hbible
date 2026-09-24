package com.hooloovoochimico.kmp.hbible.platform

import androidx.compose.material3.ColorScheme

// Dynamic color (Material You) non è disponibile su iOS: tema statico.
actual fun isDynamicColorSupported(): Boolean = false

@Suppress("UNUSED_PARAMETER")
actual fun dynamicColorScheme(dark: Boolean): ColorScheme? = null
