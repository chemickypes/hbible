package com.hooloovoochimico.kmp.hbible.platform

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

actual fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual fun dynamicColorScheme(dark: Boolean): ColorScheme? {
    if (!isDynamicColorSupported()) return null
    val context = AndroidAppContext.appContext
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
