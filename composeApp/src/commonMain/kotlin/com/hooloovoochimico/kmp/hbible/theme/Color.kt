package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Palette "Pergamena e oro": fondi avorio come la carta, testo color inchiostro,
 * oro antico come colore primario, terracotta (secondario) e salvia (terziario)
 * come accenti. Al buio i fondi restano caldi (bruno quasi nero, mai nero puro)
 * e l'oro si schiarisce per mantenere il contrasto.
 */

// Oro antico
private val Gold30 = Color(0xFF684400)
private val Gold40 = Color(0xFF8A5A12)
private val Gold80 = Color(0xFFE8B96A)
private val Gold90 = Color(0xFFFFDDB0)
private val Gold20 = Color(0xFF472A00)

// Terracotta
private val Terracotta30 = Color(0xFF7A3219)
private val Terracotta40 = Color(0xFF9A4A2E)
private val Terracotta80 = Color(0xFFFFB59C)
private val Terracotta90 = Color(0xFFF7DCC7)
private val Terracotta20 = Color(0xFF5C1A05)

// Salvia
private val Sage30 = Color(0xFF274E39)
private val Sage40 = Color(0xFF3F6650)
private val Sage80 = Color(0xFFA5D0B4)
private val Sage90 = Color(0xFFC1ECCF)
private val Sage20 = Color(0xFF0E3723)

// Neutri caldi (pergamena / inchiostro)
private val Ink = Color(0xFF1F1A14)
private val InkVariant = Color(0xFF4E4539)
private val Parchment = Color(0xFFFBF7EF)
private val ParchmentText = Color(0xFFEDE4D6)

val LightColorScheme =
  lightColorScheme(
    primary = Gold40,
    onPrimary = Color.White,
    primaryContainer = Gold90,
    onPrimaryContainer = Gold30,
    inversePrimary = Gold80,
    secondary = Terracotta40,
    onSecondary = Color.White,
    secondaryContainer = Terracotta90,
    onSecondaryContainer = Terracotta30,
    tertiary = Sage40,
    onTertiary = Color.White,
    tertiaryContainer = Sage90,
    onTertiaryContainer = Sage30,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE1CF),
    onSurfaceVariant = InkVariant,
    surfaceTint = Gold40,
    inverseSurface = Color(0xFF353028),
    inverseOnSurface = Color(0xFFF8EFE3),
    outline = Color(0xFF807567),
    outlineVariant = Color(0xFFD2C4B1),
    scrim = Color.Black,
    surfaceBright = Parchment,
    surfaceDim = Color(0xFFE2D9CB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F1E6),
    surfaceContainer = Color(0xFFF3ECDF),
    surfaceContainerHigh = Color(0xFFEDE5D6),
    surfaceContainerHighest = Color(0xFFE7DFCF),
  )

val DarkColorScheme =
  darkColorScheme(
    primary = Gold80,
    onPrimary = Gold20,
    primaryContainer = Gold30,
    onPrimaryContainer = Gold90,
    inversePrimary = Gold40,
    secondary = Terracotta80,
    onSecondary = Terracotta20,
    secondaryContainer = Terracotta30,
    onSecondaryContainer = Terracotta90,
    tertiary = Sage80,
    onTertiary = Sage20,
    tertiaryContainer = Sage30,
    onTertiaryContainer = Sage90,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF16130F),
    onBackground = ParchmentText,
    surface = Color(0xFF16130F),
    onSurface = ParchmentText,
    surfaceVariant = InkVariant,
    onSurfaceVariant = Color(0xFFD2C4B1),
    surfaceTint = Gold80,
    inverseSurface = ParchmentText,
    inverseOnSurface = Color(0xFF353028),
    outline = Color(0xFF9B8F7F),
    outlineVariant = InkVariant,
    scrim = Color.Black,
    surfaceBright = Color(0xFF3D372F),
    surfaceDim = Color(0xFF16130F),
    surfaceContainerLowest = Color(0xFF110E0A),
    surfaceContainerLow = Color(0xFF1F1B16),
    surfaceContainer = Color(0xFF231F1A),
    surfaceContainerHigh = Color(0xFF2E2924),
    surfaceContainerHighest = Color(0xFF39332D),
  )

/**
 * Soft dark grays: dark but not pitch black, used to lift the neutral surfaces of
 * the dynamic (Material You) dark scheme.
 */
val DarkBackground = Color(0xFF1A1B1D)
val DarkSurface = Color(0xFF1A1B1D)
val DarkSurfaceDim = Color(0xFF141517)
val DarkSurfaceBright = Color(0xFF3B3C3F)
val DarkSurfaceContainerLowest = Color(0xFF121314)
val DarkSurfaceContainerLow = Color(0xFF232426)
val DarkSurfaceContainer = Color(0xFF27282B)
val DarkSurfaceContainerHigh = Color(0xFF323336)
val DarkSurfaceContainerHighest = Color(0xFF3D3E41)
val DarkSurfaceVariant = Color(0xFF2C2D30)
