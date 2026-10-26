package com.hooloovoochimico.kmp.hbible.ui.settings

/** Tocchi sulla riga "Versione" che accendono o spengono le funzioni IA (come le opzioni sviluppatore). */
const val AI_TAPS_TO_TOGGLE = 7

/** Dal quarto tocco in poi si mostra quanti ne mancano. */
private const val AI_TAPS_HINT_FROM = 4

/** Tocchi più distanziati di questo ricominciano da capo. */
private const val AI_TAPS_WINDOW_MS = 3_000L

sealed interface AiTapResult {
  /** Nessun messaggio. */
  data object None : AiTapResult

  /** Mancano [remaining] tocchi al cambio di stato. */
  data class Hint(val remaining: Int) : AiTapResult

  /** Settimo tocco: lo stato delle funzioni IA va invertito. */
  data object Toggle : AiTapResult
}

/** Conta i tocchi ravvicinati sulla riga "Versione" e dice cosa mostrare a ogni tocco. */
class AiFeatureTapCounter {
  private var taps = 0
  private var lastTapAt = 0L

  /** Registra un tocco avvenuto all'istante [now] (epoch millis). */
  fun tap(now: Long): AiTapResult {
    taps = if (now - lastTapAt > AI_TAPS_WINDOW_MS) 1 else taps + 1
    lastTapAt = now
    return when {
      taps >= AI_TAPS_TO_TOGGLE -> {
        taps = 0
        AiTapResult.Toggle
      }
      taps >= AI_TAPS_HINT_FROM -> AiTapResult.Hint(AI_TAPS_TO_TOGGLE - taps)
      else -> AiTapResult.None
    }
  }
}
