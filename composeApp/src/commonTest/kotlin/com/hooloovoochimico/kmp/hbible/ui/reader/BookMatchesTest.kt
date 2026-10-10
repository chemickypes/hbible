package com.hooloovoochimico.kmp.hbible.ui.reader

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookMatchesTest {

  @Test
  fun matchesNameOrAbbreviationIgnoringCaseAccentsAndSpaces() {
    assertTrue(bookMatches("1 Corinzi", "1Cor", "cor"))
    assertTrue(bookMatches("1 Corinzi", "1Cor", "1 cor"))
    assertTrue(bookMatches("Giosuè", "Gs", "giosue"))
    assertTrue(bookMatches("Giobbe", "Gb", "GB"))
    assertTrue(bookMatches("Romani", "Rm", ""))
  }

  @Test
  fun rejectsOtherBooks() {
    assertFalse(bookMatches("Romani", "Rm", "cor"))
    assertFalse(bookMatches("Giovanni", "Gv", "giob"))
  }
}
