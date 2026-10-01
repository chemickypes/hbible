package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hooloovoochimico.kmp.hbible.data.BookRef

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey override val n: Int,
  override val name: String,
  override val abbr: String,
  override val chapters: Int,
) : BookRef

@Entity(
  tableName = "verses",
  primaryKeys = ["translation", "book", "chapter", "verse"],
  indices = [Index(value = ["translation", "book", "chapter"])],
)
data class VerseEntity(
  @ColumnInfo(name = "translation") val translation: String,
  @ColumnInfo(name = "book") val book: Int,
  @ColumnInfo(name = "chapter") val chapter: Int,
  @ColumnInfo(name = "verse") val verse: Int,
  val title: String?,
  val text: String,
)

/** Verse in its original language ("he" = Hebrew OT, "el" = Greek NT). */
@Entity(tableName = "original_verses", primaryKeys = ["book", "chapter", "verse"])
data class OriginalVerseEntity(
  val book: Int,
  val chapter: Int,
  val verse: Int,
  val lang: String,
  val text: String,
  val transliteration: String,
  /** Space-padded Strong's numbers per word, aligned with `text` tokens. */
  @ColumnInfo(name = "lemmas") val lemmas: String = "",
  /** Comma-separated Italian token indices (Nuova Riveduta), -1 = unaligned. */
  @ColumnInfo(name = "it_nr") val italianNr: String = "",
  /** Comma-separated Italian token indices (Riveduta 2020). */
  @ColumnInfo(name = "it_r2") val italianR2: String = "",
  /** Comma-separated Italian token indices (Riveduta 1927). */
  @ColumnInfo(name = "it_r27") val italianR27: String = "",
  /** Comma-separated Italian token indices (Diodati). */
  @ColumnInfo(name = "it_dio") val italianDio: String = "",
  /** Comma-separated Italian token indices (Nuova Diodati). */
  @ColumnInfo(name = "it_nd") val italianNd: String = "",
  /** Comma-separated Italian token indices (CEI 1974). */
  @ColumnInfo(name = "it_cei") val italianCei: String = "",
  /** Comma-separated Italian token indices (Ricciotti). */
  @ColumnInfo(name = "it_ric") val italianRic: String = "",
  /** Comma-separated Italian token indices (Martini). */
  @ColumnInfo(name = "it_mar") val italianMar: String = "",
  /** Tab-separated contextual interlinear gloss (English) per word, "" = none. */
  @ColumnInfo(name = "glosses") val glosses: String = "",
  /** Tab-separated contextual interlinear gloss (Italian) per word, "" = none. */
  @ColumnInfo(name = "glosses_it") val glossesIt: String = "",
)

/**
 * Italian token indices of the verse (translation-dependent alignment channel
 * `ar_<version>`), -1 = unaligned. Shared by the verse detail page and the
 * interlinear tab so both resolve Italian words the same way.
 */
fun OriginalVerseEntity?.alignedItalianIndices(translation: String): List<Int> =
  when (translation) {
    "R2" -> this?.italianR2
    "R27" -> this?.italianR27
    "DIO" -> this?.italianDio
    "ND" -> this?.italianNd
    "CEI" -> this?.italianCei
    "RIC" -> this?.italianRic
    "MAR" -> this?.italianMar
    else -> this?.italianNr
  }?.split(",")?.map { it.toIntOrNull() ?: -1 } ?: emptyList()

/** A cross-reference link between two verses. */
@Entity(
  tableName = "cross_references",
  primaryKeys = ["fromBook", "fromChapter", "fromVerse", "toBook", "toChapter", "toVerse"],
  indices = [Index(value = ["fromBook", "fromChapter", "fromVerse"], name = "index_cross_references_from")],
)
data class CrossReferenceEntity(
  @ColumnInfo(name = "fromBook") val fromBook: Int,
  @ColumnInfo(name = "fromChapter") val fromChapter: Int,
  @ColumnInfo(name = "fromVerse") val fromVerse: Int,
  @ColumnInfo(name = "toBook") val toBook: Int,
  @ColumnInfo(name = "toChapter") val toChapter: Int,
  @ColumnInfo(name = "toVerse") val toVerse: Int,
)

/** Lightweight verse reference returned by lemma occurrence queries. */
data class VerseRefRow(
  val book: Int,
  val chapter: Int,
  val verse: Int,
)

/** Strong's dictionary entry (public domain) with an optional Italian gloss. */
@Entity(tableName = "lexemes", primaryKeys = ["lang", "number"])
data class LexemeEntity(
  /** "he" | "el" */
  val lang: String,
  /** Strong's number, e.g. "7971". */
  val number: String,
  /** Romanized lemma from the dictionary. */
  val romanized: String,
  /** English gloss (public domain Strong's). */
  val gloss: String,
  /** Italian gloss, filled on demand via AI translation. */
  @ColumnInfo(name = "gloss_it") val glossIt: String = "",
)

/** A personal note: rich-text spans are stored as JSON in [styles]. */
@Entity(tableName = "notes", indices = [Index(value = ["updatedAt"])])
data class NoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  /** Plain text of the note (styles live in [styles], verse refs are detected live). */
  val content: String,
  /** JSON array of [com.hooloovoochimico.kmp.hbible.ui.notes.StyleRange]. */
  val styles: String = "[]",
  /** Optional linked verse (book 0 = no link). */
  val book: Int = 0,
  val chapter: Int = 0,
  val verse: Int = 0,
  val createdAt: Long,
  val updatedAt: Long,
)

/** AI-generated introduction of a Bible book, cached locally. */
@Entity(tableName = "book_info")
data class BookInfoEntity(
  /** Canonical book number (1-66). */
  @PrimaryKey val book: Int,
  @ColumnInfo(name = "context_text") val contextText: String,
  val protagonists: String,
  @ColumnInfo(name = "christocentric") val christocentric: String,
  /** Model that produced the text, for reference. */
  val model: String = "",
  val updatedAt: Long,
)
