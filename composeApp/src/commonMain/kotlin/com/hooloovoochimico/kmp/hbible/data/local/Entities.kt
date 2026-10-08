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
  /** true = the verse opens a paragraph (package field `p`). Last column: see migration 15→16. */
  @ColumnInfo(name = "paragraph", defaultValue = "0") val paragraph: Boolean = false,
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
  /** Tab-separated contextual interlinear gloss (English) per word, "" = none. */
  @ColumnInfo(name = "glosses") val glosses: String = "",
  /** Tab-separated contextual interlinear gloss (Italian) per word, "" = none. */
  @ColumnInfo(name = "glosses_it") val glossesIt: String = "",
)

/**
 * Word alignment of an original verse against one translation: for each
 * original word (same order as [OriginalVerseEntity.text]) the 0-based index
 * of the translation token (verse text split on whitespace), -1 = unaligned.
 * One row per (verse, translation): a translation without alignment has no row.
 */
@Entity(
  tableName = "original_alignments",
  primaryKeys = ["book", "chapter", "verse", "translation"],
  indices = [Index(value = ["translation", "book", "chapter"])],
)
data class OriginalAlignmentEntity(
  val book: Int,
  val chapter: Int,
  val verse: Int,
  val translation: String,
  /** Comma-separated token indices, e.g. "1,3,-1,5". */
  val indices: String,
)

/** Parses the comma-separated indices of an alignment row. */
fun parseAlignment(indices: String): List<Int> =
  if (indices.isBlank()) emptyList() else indices.split(",").map { it.trim().toIntOrNull() ?: -1 }

/** Tokens of a translation verse as indexed by alignments (whitespace split, newlines included). */
fun alignmentTokens(text: String): List<String> = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

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

/**
 * Sync state of one content package (manifest path → last applied hash), used
 * by [com.hooloovoochimico.kmp.hbible.data.content.ContentSyncer] to decide
 * which packages changed and must be re-downloaded from the CMS.
 */
@Entity(tableName = "content_state")
data class ContentStateEntity(
  /** Manifest package path, e.g. "bible/NR.json" or "originals/01.json". */
  @PrimaryKey @ColumnInfo(name = "package_id") val packageId: String,
  val hash: String,
  /** Manifest version the hash was taken from. */
  val version: String,
  @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

/** Metadata of an installed Bible translation (from the CMS package's `meta`). */
@Entity(tableName = "translation_meta")
data class TranslationMetaEntity(
  @PrimaryKey val abbr: String,
  val name: String,
  val description: String = "",
  val publisher: String = "",
  val year: String = "",
  val copyright: String = "",
  /** Short license, e.g. "CC BY-SA 4.0" (CMS `meta.license`). */
  @ColumnInfo(defaultValue = "''") val license: String = "",
  @ColumnInfo(name = "license_url", defaultValue = "''") val licenseUrl: String = "",
  @ColumnInfo(name = "source_url", defaultValue = "''") val sourceUrl: String = "",
  /** Credits text ready to show (CMS `meta.attribution`). */
  @ColumnInfo(defaultValue = "''") val attribution: String = "",
)

/** Verse of the day from the CMS: date=null → rotation pool, date set → calendar override. */
@Entity(tableName = "votd_entries")
data class VotdEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  /** 'YYYY-MM-DD' fixed date, null = rotation pool. */
  val date: String? = null,
  val book: Int,
  val chapter: Int,
  val verse: Int,
  /** Preferred translation (fallback: reader's current one). */
  val translation: String? = null,
  /** Optional curated note (markdown). */
  val note: String? = null,
  /** Order inside the rotation pool. */
  @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
)

/** Curated feed item ("condivisioni") published by the CMS. */
@Entity(tableName = "posts")
data class PostEntity(
  @PrimaryKey val slug: String,
  val title: String,
  /** Markdown body. */
  val body: String,
  /** Optional linked verse (book 0 = no link). */
  val book: Int = 0,
  val chapter: Int = 0,
  val verse: Int = 0,
  @ColumnInfo(name = "published_at") val publishedAt: String? = null,
)
