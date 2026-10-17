package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/** Contrasto WCAG fra ogni coppia X/onX dei due schemi Blu: almeno 4.5:1 (testo normale). */
class BlueThemeContrastTest {

  private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
  }

  private fun pairs(s: ColorScheme): List<Triple<String, Color, Color>> =
    listOf(
      Triple("primary", s.primary, s.onPrimary),
      Triple("primaryContainer", s.primaryContainer, s.onPrimaryContainer),
      Triple("secondary", s.secondary, s.onSecondary),
      Triple("secondaryContainer", s.secondaryContainer, s.onSecondaryContainer),
      Triple("tertiary", s.tertiary, s.onTertiary),
      Triple("tertiaryContainer", s.tertiaryContainer, s.onTertiaryContainer),
      Triple("error", s.error, s.onError),
      Triple("errorContainer", s.errorContainer, s.onErrorContainer),
      Triple("surface", s.surface, s.onSurface),
      Triple("background", s.background, s.onBackground),
      Triple("surfaceVariant", s.surfaceVariant, s.onSurfaceVariant),
      Triple("inverseSurface", s.inverseSurface, s.inverseOnSurface),
    )

  private fun check(name: String, scheme: ColorScheme) {
    val failures =
      pairs(scheme).filter { (_, bg, fg) -> contrast(bg, fg) < 4.5f }.map { (n, bg, fg) ->
        "$name/$n: ${contrast(bg, fg)}"
      }
    assertTrue(failures.isEmpty(), "Contrasto insufficiente: $failures")
  }

  @Test
  fun `Blu chiaro rispetta il contrasto 4,5`() = check("BlueLight", BlueLightColorScheme)

  @Test
  fun `Blu scuro rispetta il contrasto 4,5`() = check("BlueDark", BlueDarkColorScheme)

  @Test
  fun `il testo secondario resta leggibile sulle schede`() {
    for (s in listOf(BlueLightColorScheme, BlueDarkColorScheme)) {
      assertTrue(contrast(s.surfaceContainerLow, s.onSurfaceVariant) >= 4.5f)
    }
  }
}
