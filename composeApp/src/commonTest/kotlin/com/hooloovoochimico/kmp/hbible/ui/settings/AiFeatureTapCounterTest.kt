package com.hooloovoochimico.kmp.hbible.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/** Sblocco nascosto delle funzioni IA: 7 tocchi ravvicinati sulla riga "Versione". */
class AiFeatureTapCounterTest {

  @Test
  fun `il settimo tocco ravvicinato accende il cambio, i precedenti danno solo il conto alla rovescia`() {
    val counter = AiFeatureTapCounter()
    val results = (0 until AI_TAPS_TO_TOGGLE).map { counter.tap(now = 1_000L + it * 100L) }
    assertEquals(
      listOf(
        AiTapResult.None,
        AiTapResult.None,
        AiTapResult.None,
        AiTapResult.Hint(3),
        AiTapResult.Hint(2),
        AiTapResult.Hint(1),
        AiTapResult.Toggle,
      ),
      results,
    )
  }

  @Test
  fun `tocchi distanziati ricominciano il conteggio`() {
    val counter = AiFeatureTapCounter()
    repeat(5) { counter.tap(now = 1_000L + it * 100L) }
    // Pausa lunga: il conto riparte da uno, quindi serve un altro giro completo.
    assertEquals(AiTapResult.None, counter.tap(now = 60_000L))
    assertEquals(AiTapResult.None, counter.tap(now = 60_100L))
  }

  @Test
  fun `dopo il cambio il conteggio riparte da zero`() {
    val counter = AiFeatureTapCounter()
    repeat(AI_TAPS_TO_TOGGLE) { counter.tap(now = 1_000L + it * 100L) }
    assertEquals(AiTapResult.None, counter.tap(now = 1_800L))
  }
}
