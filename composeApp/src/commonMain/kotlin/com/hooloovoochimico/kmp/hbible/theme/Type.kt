package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Defaults = Typography()

/** Serif per titoli e intestazioni: richiama il testo biblico (ScriptureTypography). */
private fun TextStyle.serif(weight: FontWeight = FontWeight.Medium) =
  copy(fontFamily = FontFamily.Serif, fontWeight = weight)

/**
 * Display e headline in serif (titoli delle pagine, card in evidenza), il resto in
 * sans per controlli e testo d'interfaccia.
 */
val Typography =
  Typography(
    displayLarge = Defaults.displayLarge.serif(FontWeight.Normal),
    displayMedium = Defaults.displayMedium.serif(FontWeight.Normal),
    displaySmall = Defaults.displaySmall.serif(FontWeight.Normal),
    headlineLarge = Defaults.headlineLarge.serif(),
    headlineMedium = Defaults.headlineMedium.serif(),
    headlineSmall = Defaults.headlineSmall.serif(),
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
  )
