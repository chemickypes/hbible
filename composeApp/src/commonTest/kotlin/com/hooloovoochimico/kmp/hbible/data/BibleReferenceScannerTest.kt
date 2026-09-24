package com.hooloovoochimico.kmp.hbible.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleReferenceScannerTest {

  // Minimal book table: the entries used by the tests plus their collisions.
  private val books =
    listOf(
      FakeBookRef(9, "1 Samuele", "1Sam", 31),
      FakeBookRef(12, "2 Re", "2Re", 25),
      FakeBookRef(18, "Giobbe", "Gb", 42),
      FakeBookRef(19, "Salmi", "Sal", 150),
      FakeBookRef(43, "Giovanni", "Gv", 21),
      FakeBookRef(44, "Atti", "At", 28),
      FakeBookRef(46, "1 Corinzi", "1Cor", 16),
      FakeBookRef(62, "1 Giovanni", "1Gv", 5),
      FakeBookRef(66, "Apocalisse", "Ap", 22),
    )

  @Test
  fun findsSimpleReference() {
    val matches = BibleReferenceParser.findReferences("vedi Gv 3:16 oggi", books)
    assertEquals(1, matches.size)
    assertEquals(43, matches[0].ref.book)
    assertEquals(3, matches[0].ref.chapter)
    assertEquals(16, matches[0].ref.verse)
    assertEquals("Gv 3:16", matches[0].text)
  }

  @Test
  fun findsReferenceWithPrefixAndNoSpace() {
    // "1 gv1" resolves to 1 Giovanni, chapter 1 (no verse).
    val matches = BibleReferenceParser.findReferences("come dice 1 gv1", books)
    assertEquals(1, matches.size)
    assertEquals(62, matches[0].ref.book)
    assertEquals(1, matches[0].ref.chapter)
    assertEquals(null, matches[0].ref.verse)
  }

  @Test
  fun findsReferenceWithCommaSeparator() {
    val matches = BibleReferenceParser.findReferences("Sal 23,1 è famoso", books)
    assertEquals(1, matches.size)
    assertEquals(19, matches[0].ref.book)
    assertEquals(23, matches[0].ref.chapter)
    assertEquals(1, matches[0].ref.verse)
  }

  @Test
  fun findsMultipleReferences() {
    val matches = BibleReferenceParser.findReferences("tra Gv 1:1 e Sal 23", books)
    assertEquals(2, matches.size)
    assertEquals(43, matches[0].ref.book)
    assertEquals(19, matches[1].ref.book)
  }

  @Test
  fun trimsTrailingPunctuation() {
    val matches = BibleReferenceParser.findReferences("(Gv 3:16), dice", books)
    assertEquals(1, matches.size)
    assertEquals("Gv 3:16", matches[0].text)
    assertEquals(16, matches[0].ref.verse)
  }

  @Test
  fun ignoresBareBookNamesWithoutChapter() {
    // "at" and "am" are book abbreviations, but without a digit they must not match.
    val matches = BibleReferenceParser.findReferences("at that time am I", books)
    assertTrue(matches.isEmpty())
  }

  @Test
  fun ignoresUnknownWords() {
    val matches = BibleReferenceParser.findReferences("questa nota non cita nulla", books)
    assertTrue(matches.isEmpty())
  }

  @Test
  fun rejectsOutOfRangeChapters() {
    val matches = BibleReferenceParser.findReferences("Sal 999 non esiste", books)
    assertTrue(matches.isEmpty())
  }

  @Test
  fun findsVerseRangeInParentheses() {
    val matches = BibleReferenceParser.findReferences("(2Re 23:4-20).", books)
    assertEquals(1, matches.size)
    assertEquals(12, matches[0].ref.book)
    assertEquals(23, matches[0].ref.chapter)
    assertEquals(4, matches[0].ref.verse)
    assertEquals(20, matches[0].ref.verseEnd)
    assertEquals("2Re 23:4-20", matches[0].text)
  }

  @Test
  fun findsRangeWrappedAcrossLines() {
    val text = "centralizzando il culto a Gerusalemme (2Re\n23:21-23)."
    val matches = BibleReferenceParser.findReferences(text, books)
    assertEquals(1, matches.size)
    assertEquals(12, matches[0].ref.book)
    assertEquals(23, matches[0].ref.chapter)
    assertEquals(21, matches[0].ref.verse)
    assertEquals(23, matches[0].ref.verseEnd)
    assertEquals("2Re 23:21-23", matches[0].text)
  }

  @Test
  fun keepsOnlyValidRanges() {
    // end < start: the range is dropped but the starting verse stays valid.
    val matches = BibleReferenceParser.findReferences("vedi Gv 3:16-4", books)
    assertEquals(1, matches.size)
    assertEquals(16, matches[0].ref.verse)
    assertEquals(null, matches[0].ref.verseEnd)
  }

  @Test
  fun findsChapterRangeStart() {
    val matches = BibleReferenceParser.findReferences("il ciclo di 2Re 23-25", books)
    assertEquals(1, matches.size)
    assertEquals(12, matches[0].ref.book)
    assertEquals(23, matches[0].ref.chapter)
    assertEquals(null, matches[0].ref.verse)
  }

  @Test
  fun ignoresBareNumbersLikeListMarkers() {
    // "3." is a list marker, not "3 Giovanni 1": candidates must contain letters too.
    val matches = BibleReferenceParser.findReferences("3. **Risposta a un superiore umano:**", books)
    assertTrue(matches.isEmpty())
  }

  @Test
  fun ignoresLoneNumbers() {
    val matches = BibleReferenceParser.findReferences("vedi il punto 3 e il numero 16", books)
    assertTrue(matches.isEmpty())
  }

  @Test
  fun findsReferenceAtStringStart() {
    val matches = BibleReferenceParser.findReferences("1Cor13 è l'inno alla carità", books)
    assertEquals(1, matches.size)
    assertEquals(46, matches[0].ref.book)
    assertEquals(13, matches[0].ref.chapter)
  }

  @Test
  fun spansAreCorrectForHighlighting() {
    val text = "vedi Gv 3:16"
    val matches = BibleReferenceParser.findReferences(text, books)
    assertEquals(1, matches.size)
    assertEquals(text.indexOf("Gv"), matches[0].start)
    assertEquals(text.length, matches[0].end)
    assertEquals("Gv 3:16", text.substring(matches[0].start, matches[0].end))
  }
}
