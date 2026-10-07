package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.hooloovoochimico.kmp.hbible.theme.Dimens

/**
 * Spazio in fondo alle liste per non finire sotto la navigazione: alto con la
 * bottom bar flottante (telefono), solo inset di sistema + respiro con la
 * navigation rail (finestre larghe). Fornito dal guscio in App.kt.
 */
val LocalBottomBarClearance = staticCompositionLocalOf { Dimens.bottomBarClearance }

/**
 * Margine laterale che centra un contenuto largo al massimo [maxContentWidth]
 * dentro [availableWidth]; zero quando lo spazio non basta (telefono). Usato
 * come contentPadding delle liste, così lo scroll resta attivo su tutta la
 * larghezza e non solo nella colonna centrale.
 */
fun centeringPadding(availableWidth: Dp, maxContentWidth: Dp): Dp =
  max((availableWidth - maxContentWidth) / 2, Dp(0f))

/**
 * Colonna centrata larga al massimo [Dimens.contentMaxWidth] per le schermate a
 * colonna singola: su telefono non cambia nulla, su tablet evita righe e campi
 * stirati per tutta la larghezza.
 */
@Composable
fun ContentWidth(
  modifier: Modifier = Modifier,
  maxWidth: Dp = Dimens.contentMaxWidth,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Box(Modifier.widthIn(max = maxWidth).fillMaxSize(), content = content)
  }
}
