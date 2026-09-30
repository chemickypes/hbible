package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.platform.dynamicColorScheme
import com.hooloovoochimico.kmp.hbible.platform.isDynamicColorSupported

private val DarkColorScheme = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

/** Replaces the near-black neutral surfaces with soft dark grays (accents untouched). */
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
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
      // Soften the dark palette whether it comes from Material You or the static scheme.
      darkTheme -> {
        if (dynamicColor && isDynamicColorSupported()) {
          dynamicColorScheme(true)?.softened() ?: DarkColorScheme.softened()
        } else {
          DarkColorScheme.softened()
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
