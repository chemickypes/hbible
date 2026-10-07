package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale shared by every screen. Change a value here to retune the
 * rhythm of the whole app.
 */
object Spacing {
  val xs = 4.dp
  val sm = 8.dp
  val md = 16.dp
  val lg = 24.dp
  val xl = 32.dp
}

/** App-level layout metrics: insets and structural dimensions. */
object Dimens {
  /**
   * Bottom content padding for scrollable lists: keeps the last rows visible
   * above the floating navigation bar.
   */
  val bottomBarClearance = 130.dp

  /**
   * Larghezza massima del testo biblico: oltre questa le righe diventano troppo
   * lunghe da leggere (tablet in orizzontale). Il lettore centra il testo.
   */
  val readingMaxWidth = 680.dp

  /** Larghezza massima delle schermate a colonna singola (note, ricerca, impostazioni). */
  val contentMaxWidth = 840.dp

  /** Larghezza del pannello dettaglio versetto accanto al lettore (finestre espanse). */
  val detailPaneWidth = 400.dp
}
