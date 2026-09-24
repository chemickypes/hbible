package com.hooloovoochimico.kmp.hbible.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibleReferenceParserTest {

  private val books =
    listOf(
      FakeBookRef(1, "Genesi", "Gen", 50),
      FakeBookRef(6, "Giosuè", "Gs", 24),
      FakeBookRef(9, "1 Samuele", "1Sam", 31),
      FakeBookRef(10, "2 Samuele", "2Sam", 24),
      FakeBookRef(11, "1 Re", "1Re", 22),
      FakeBookRef(12, "2 Re", "2Re", 25),
      FakeBookRef(18, "Giobbe", "Gio", 42),
      FakeBookRef(19, "Salmi", "Sal", 150),
      FakeBookRef(22, "Cantico dei Cantici", "Ct", 8),
      FakeBookRef(32, "Giona", "Gio", 4),
      FakeBookRef(43, "Giovanni", "Gv", 21),
      FakeBookRef(46, "1 Corinzi", "1Cor", 16),
      FakeBookRef(62, "1 Giovanni", "1Gv", 5),
      FakeBookRef(64, "3 Giovanni", "3Gv", 1),
      FakeBookRef(66, "Apocalisse", "Ap", 22),
    )

  @Test
  fun leadingNumberWithAbbreviationAndReference() {
    assertEquals(BibleRef(62, 1, 3), BibleReferenceParser.parse("1 GV 1:3", books))
  }

  @Test
  fun commaAsChapterVerseSeparator() {
    assertEquals(BibleRef(43, 3, 16), BibleReferenceParser.parse("Gv 3,16", books))
  }

  @Test
  fun bookOnlyDefaultsToChapterOne() {
    assertEquals(BibleRef(1, 1, null), BibleReferenceParser.parse("genesi", books))
  }

  @Test
  fun psalmsAbbreviation() {
    assertEquals(BibleRef(19, 23, null), BibleReferenceParser.parse("Sal 23", books))
  }

  @Test
  fun multiWordBookName() {
    assertEquals(BibleRef(22, 3, 1), BibleReferenceParser.parse("cantico dei cantici 3:1", books))
  }

  @Test
  fun gluedTokens() {
    assertEquals(BibleRef(46, 13, null), BibleReferenceParser.parse("1cor13", books))
    assertEquals(BibleRef(12, 1, null), BibleReferenceParser.parse("2re1", books))
  }

  @Test
  fun verseRanges() {
    assertEquals(BibleRef(12, 23, 4, 20), BibleReferenceParser.parse("2Re 23:4-20", books))
    assertEquals(BibleRef(19, 23, 1, 6), BibleReferenceParser.parse("sal 23:1-6", books))
    // incollato senza spazio: "2re23:4-20"
    assertEquals(BibleRef(12, 23, 4, 20), BibleReferenceParser.parse("2re23:4-20", books))
    // separatori italiani
    assertEquals(BibleRef(12, 23, 4, 20), BibleReferenceParser.parse("2Re 23.4-20", books))
  }

  @Test
  fun chapterRangesPointToFirstChapter() {
    assertEquals(BibleRef(12, 23, null), BibleReferenceParser.parse("2Re 23-25", books))
  }

  @Test
  fun newlineInsideReferenceDoesNotBreakParsing() {
    assertEquals(BibleRef(12, 23, 21, 23), BibleReferenceParser.parse("2Re\n23:21-23", books))
  }

  @Test
  fun ambiguousGioResolvesToGiona() {
    assertEquals(BibleRef(32, 1, null), BibleReferenceParser.parse("gio 1", books))
  }

  @Test
  fun jobByGb() {
    assertEquals(BibleRef(18, 42, null), BibleReferenceParser.parse("gb 42", books))
  }

  @Test
  fun numberedKings() {
    assertEquals(BibleRef(11, 3, null), BibleReferenceParser.parse("1re 3", books))
    assertEquals(BibleRef(12, 2, 5), BibleReferenceParser.parse("2 Re 2:5", books))
  }

  @Test
  fun shortNumberedAliases() {
    assertEquals(BibleRef(62, 4, null), BibleReferenceParser.parse("1 g 4", books))
    assertEquals(BibleRef(64, 1, null), BibleReferenceParser.parse("3g 1", books))
  }

  @Test
  fun accentedNameWithoutAccent() {
    assertEquals(BibleRef(6, 2, null), BibleReferenceParser.parse("giosue 2", books))
  }

  @Test
  fun spacelessPrefixAndChapter() {
    assertEquals(BibleRef(9, 5, null), BibleReferenceParser.parse("1 sam 5", books))
  }

  @Test
  fun fullNumberedName() {
    assertEquals(BibleRef(62, 2, null), BibleReferenceParser.parse("1 giovanni 2", books))
  }

  @Test
  fun revelationLastVerse() {
    assertEquals(BibleRef(66, 22, 21), BibleReferenceParser.parse("Ap 22:21", books))
  }

  @Test
  fun dotsAsSeparator() {
    assertEquals(BibleRef(43, 2, 1), BibleReferenceParser.parse("gv.2.1", books))
  }

  @Test
  fun chapterOutOfRangeFails() {
    assertNull(BibleReferenceParser.parse("gv 999", books))
  }

  @Test
  fun ambiguousBareNameFails() {
    assertNull(BibleReferenceParser.parse("samuele", books))
  }

  @Test
  fun unknownBookFails() {
    assertNull(BibleReferenceParser.parse("xyz 3", books))
  }

  @Test
  fun partialNameWithPrefixNumberResolves() {
    // "1gio" → 1 Giovanni via prefix matching (key contains letters).
    assertEquals(BibleRef(62, 1, null), BibleReferenceParser.parse("1gio", books))
  }

  @Test
  fun bareNumberNeverResolves() {
    // "3" must not fuzzy-match "3 Giovanni" (regression: "3." in notes).
    assertNull(BibleReferenceParser.parse("3", books))
    assertNull(BibleReferenceParser.parse("1", books))
  }
}
