package com.hooloovoochimico.kmp.hbible.ui.common

import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.ui.explore.AiVerseMatch
import kotlinx.coroutines.flow.first

/** Longest passage shown in full; longer ones end with "…" (the reader has the rest). */
private const val MAX_PASSAGE_VERSES = 12

/**
 * Text of a resolved reference ("Fil 4:6-7", "Is 52:13-53:12") in [translation],
 * verses joined by a space; null when the translation lacks the passage.
 */
internal suspend fun BibleRepository.passageText(translation: String, match: AiVerseMatch): String? {
  val ref = match.ref
  val start = ref.verse ?: 1
  val endChapter = match.endChapter ?: ref.chapter
  val parts = mutableListOf<String>()
  for (c in ref.chapter..endChapter) {
    val from = if (c == ref.chapter) start else 1
    val to =
      if (c < endChapter || ref.verse == null) Int.MAX_VALUE else match.verseEnd ?: start
    chapter(translation, ref.book, c).first()
      .filter { it.verse in from..to }
      .forEach { parts += it.text.replace("\n", " ").trim() }
    if (parts.size > MAX_PASSAGE_VERSES) break
  }
  if (parts.isEmpty()) return null
  val shown = parts.take(MAX_PASSAGE_VERSES).joinToString(" ")
  return if (parts.size > MAX_PASSAGE_VERSES) "$shown …" else shown
}
