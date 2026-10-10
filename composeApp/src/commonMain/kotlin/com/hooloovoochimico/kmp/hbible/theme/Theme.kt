package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.platform.dynamicColorScheme
import com.hooloovoochimico.kmp.hbible.platform.isDynamicColorSupported

/** Replaces the near-black neutral surfaces of Material You with soft dark grays (accents untouched). */
private fun ColorScheme.softened(): ColorScheme =
  copy(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceVariant = DarkSurfaceVariant,
  )

@Composable
fun HBibleTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  // Dynamic color is available on Android 12+; off by default so the app keeps
  // its own palette (Color.kt).
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }
  val colorScheme =
    when {
      // Material You dark schemes are near-black: soften them. The app palette
      // already has warm, lifted dark surfaces.
      darkTheme -> {
        if (dynamicColor && isDynamicColorSupported()) {
          dynamicColorScheme(true)?.softened() ?: DarkColorScheme
        } else {
          DarkColorScheme
        }
      }
      dynamicColor && isDynamicColorSupported() -> dynamicColorScheme(false) ?: LightColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = ExpressiveShapes,
  ) {
    // Le schermate disegnano i propri sfondi (.background) invece di appoggiarsi a
    // Surface/Scaffold: senza un provider esplicito i Text senza colore ereditano il
    // default della libreria (Color.Black), illeggibile sul tema scuro.
    CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface, content = content)
  }
}
