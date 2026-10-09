package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.ui.reader.ScriptureSegment.Kind
import kotlin.test.Test
import kotlin.test.assertEquals

class ScriptureSegmentsTest {
  @Test
  fun plainVerseIsOneNormalSegment() {
    assertEquals(listOf(ScriptureSegment("In principio Dio creò.", Kind.NORMAL)), scriptureSegments("In principio Dio creò."))
  }

  @Test
  fun selahLineIsSeparated() {
    val text = "Il Signore ti risponda\nnel giorno dell'angoscia.\n[Pausa]"
    assertEquals(
      listOf(
        ScriptureSegment("Il Signore ti risponda\nnel giorno dell'angoscia.\n", Kind.NORMAL),
        ScriptureSegment("[Pausa]", Kind.SELAH),
      ),
      scriptureSegments(text),
    )
  }

  @Test
  fun bracketedVerseIsVariant() {
    val text = "[Questo tipo si può scacciare solo con la preghiera e il digiuno.]"
    assertEquals(listOf(ScriptureSegment(text, Kind.VARIANT)), scriptureSegments(text))
  }

  @Test
  fun innerBracketsAreNotAVariant() {
    val text = "[Nota] testo [altro]"
    assertEquals(Kind.NORMAL, scriptureSegments(text).single().kind)
  }
}
