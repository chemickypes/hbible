package com.hooloovoochimico.kmp.hbible.ui.notes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichTextTest {

  private val ranges = listOf(StyleRange(0, 5, NoteStyleType.BOLD))

  @Test
  fun toggleStyle_addsBoldToSelection() {
    val result = toggleStyle(emptyList(), 2, 7, NoteStyleType.BOLD)
    assertEquals(listOf(StyleRange(2, 7, NoteStyleType.BOLD)), result)
  }

  @Test
  fun toggleStyle_removesWhenFullyStyled() {
    val result = toggleStyle(ranges, 0, 5, NoteStyleType.BOLD)
    assertTrue(result.isEmpty())
  }

  @Test
  fun toggleStyle_removesOnlySelectedPortion() {
    val result = toggleStyle(ranges, 2, 4, NoteStyleType.BOLD)
    assertEquals(
      listOf(StyleRange(0, 2, NoteStyleType.BOLD), StyleRange(4, 5, NoteStyleType.BOLD)),
      result,
    )
  }

  @Test
  fun toggleStyle_extendsExistingRange() {
    val result = toggleStyle(ranges, 5, 9, NoteStyleType.BOLD)
    assertEquals(listOf(StyleRange(0, 9, NoteStyleType.BOLD)), result)
  }

  @Test
  fun toggleStyle_mergesAdjacentRanges() {
    val result =
      toggleStyle(
        listOf(StyleRange(0, 3, NoteStyleType.ITALIC), StyleRange(6, 9, NoteStyleType.ITALIC)),
        3,
        6,
        NoteStyleType.ITALIC,
      )
    assertEquals(listOf(StyleRange(0, 9, NoteStyleType.ITALIC)), result)
  }

  @Test
  fun toggleStyle_keepsOtherTypesIntact() {
    val mixed = listOf(StyleRange(0, 5, NoteStyleType.BOLD), StyleRange(1, 3, NoteStyleType.ITALIC))
    val result = toggleStyle(mixed, 0, 5, NoteStyleType.BOLD)
    assertEquals(listOf(StyleRange(1, 3, NoteStyleType.ITALIC)), result)
  }

  @Test
  fun isStyled_detectsFullCoverage() {
    val two = listOf(StyleRange(0, 3, "B"), StyleRange(3, 6, "B"))
    assertTrue(isStyled(two, 1, 5, "B"))
    assertFalse(isStyled(two, 2, 7, "B"))
    assertFalse(isStyled(emptyList(), 0, 2, "B"))
  }

  @Test
  fun adjustRanges_shiftsAfterInsertion() {
    // Insert 3 chars at position 0: the bold range moves right.
    val result = adjustRanges("abcdef", "XYZabcdef", listOf(StyleRange(0, 5, "B")))
    assertEquals(listOf(StyleRange(3, 8, "B")), result)
  }

  @Test
  fun adjustRanges_growsRangeWhenInsertedInside() {
    val result = adjustRanges("hello", "hello world", listOf(StyleRange(0, 5, "B")))
    assertEquals(listOf(StyleRange(0, 11, "B")), result)
  }

  @Test
  fun adjustRanges_shrinksOnDeletion() {
    // Delete chars 2..4 ("llo" → "he"): range clips.
    val result = adjustRanges("hello", "he", listOf(StyleRange(1, 4, "B")))
    assertEquals(listOf(StyleRange(1, 2, "B")), result)
  }

  @Test
  fun adjustRanges_dropsEmptyRanges() {
    val result = adjustRanges("abc", "a", listOf(StyleRange(1, 2, "B")))
    assertTrue(result.isEmpty())
  }

  @Test
  fun adjustRanges_replacementInTheMiddle() {
    // Replace "word" (2..6) with "longer" inside a styled range covering 0..8.
    val result = adjustRanges("a word end", "a longer end", listOf(StyleRange(0, 8, "B")))
    assertEquals(listOf(StyleRange(0, 10, "B")), result)
  }

  @Test
  fun styles_roundTripThroughJson() {
    val encoded = NoteStyles.encode(listOf(StyleRange(0, 4, "I"), StyleRange(6, 9, "U")))
    assertEquals(listOf(StyleRange(0, 4, "I"), StyleRange(6, 9, "U")), NoteStyles.decode(encoded))
  }

  @Test
  fun styles_decodeOfGarbageReturnsEmpty() {
    assertTrue(NoteStyles.decode("not json").isEmpty())
  }
}
