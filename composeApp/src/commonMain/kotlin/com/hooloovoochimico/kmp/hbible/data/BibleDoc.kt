package com.hooloovoochimico.kmp.hbible.data

import kotlinx.serialization.Serializable

@Serializable
data class BibleDoc(
  val meta: BibleMeta,
  val books: List<BookDto>,
  val verses: List<VerseDto>,
)

@Serializable
data class BibleMeta(
  val name: String,
  val abbr: String,
  val description: String = "",
  val publisher: String = "",
  val year: String = "",
  val copyright: String = "",
  /** Short license, e.g. "CC BY-SA 4.0". */
  val license: String = "",
  val license_url: String = "",
  val source_url: String = "",
  /** Credits text ready to show in the app. */
  val attribution: String = "",
)

@Serializable
data class BookDto(
  val n: Int,
  val name: String,
  val abbr: String,
  val chapters: Int,
  val verses: Int,
)

@Serializable
data class VerseDto(
  val b: Int,
  val c: Int,
  val v: Int,
  val t: String? = null,
  val x: String,
  /** 1 = the verse opens a paragraph. */
  val p: Int = 0,
)

/** Bundle of original-language verses (Hebrew OT / Greek NT). */
@Serializable
data class OriginalDoc(
  val verses: List<OriginalVerseDto>,
)

@Serializable
data class OriginalVerseDto(
  val b: Int,
  val c: Int,
  val v: Int,
  val lang: String,
  val text: String,
  val tr: String,
  val lm: String = "",
  /**
   * Word alignment per translation (format 2 of the CMS packages): abbr → for
   * each original word the 0-based token index in that translation's verse,
   * -1 = unaligned. A translation without alignment is simply absent.
   */
  val al: Map<String, List<Int>> = emptyMap(),
  /** Contextual interlinear gloss (English) per word, "" = none. */
  val ge: List<String> = emptyList(),
  /** Contextual interlinear gloss (Italian, where curated) per word, "" = none. */
  val gi: List<String> = emptyList(),
)

/** Flat list of cross-reference links [fromBook, fromChapter, fromVerse, toBook, toChapter, toVerse]. */
@Serializable
data class CrossRefDoc(
  val refs: List<List<Int>>,
)

/** Strong's lexicon: {"he": {num: {tr, g}}, "el": {...}}. */
@Serializable
data class LexiconDoc(
  val he: Map<String, LexemeDto> = emptyMap(),
  val el: Map<String, LexemeDto> = emptyMap(),
)

@Serializable
data class LexemeDto(
  val tr: String = "",
  val g: String = "",
  /** Italian gloss pre-generated offline (batch AI); empty = translate on demand. */
  val gi: String = "",
)
