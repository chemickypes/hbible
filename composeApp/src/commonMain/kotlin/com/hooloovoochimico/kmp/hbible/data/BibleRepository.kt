package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseRefRow
import com.hooloovoochimico.kmp.hbible.data.local.withTransaction
import com.hooloovoochimico.kmp.hbible.resources.Res
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/** Display names of the bundled translations, keyed by their database code. */
val TRANSLATION_NAMES = mapOf("NR" to "Nuova Riveduta", "R2" to "Riveduta 2020", "R27" to "Riveduta 1927")

/** Scripture data: books, chapters, verses, original text and cached book info. */
interface BibleRepository {
  fun books(): Flow<List<BookEntity>>

  fun chapter(translation: String, book: Int, chapter: Int): Flow<List<VerseEntity>>

  /** Loads a single verse, used to preview AI search matches. */
  suspend fun verse(translation: String, book: Int, chapter: Int, verse: Int): VerseEntity?

  /** Original-language text (Hebrew/Greek) for a verse, if available. */
  fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?>

  /** Cross-references pointing to other verses. */
  fun crossReferences(book: Int, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>>

  /** Verses (max [limit]) whose original text contains the given Strong's lemma. */
  suspend fun lemmaOccurrences(
    lang: String,
    lemma: String,
    excludeBook: Int = -1,
    excludeChapter: Int = -1,
    excludeVerse: Int = -1,
    limit: Int = 40,
  ): List<VerseRefRow>

  /** Strong's dictionary entry, if present. */
  suspend fun lexeme(lang: String, number: String): LexemeEntity?

  /** Caches the Italian translation of a dictionary gloss. */
  suspend fun saveGlossIt(lang: String, number: String, glossIt: String)

  /** Imports the bundled translations into the database on first launch. */
  suspend fun ensureImported()

  // --- Book info ---

  fun bookInfo(book: Int): Flow<BookInfoEntity?>

  suspend fun saveBookInfo(info: BookInfoEntity)
}

class DefaultBibleRepository(
  private val db: BibleDatabase,
) : BibleRepository {

  override fun books(): Flow<List<BookEntity>> = db.bibleDao().books()

  override fun chapter(translation: String, book: Int, chapter: Int): Flow<List<VerseEntity>> =
    db.bibleDao().chapter(translation, book, chapter)

  override suspend fun verse(
    translation: String,
    book: Int,
    chapter: Int,
    verse: Int,
  ): VerseEntity? = db.bibleDao().verse(translation, book, chapter, verse)

  override fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?> =
    db.bibleDao().originalVerse(book, chapter, verse)

  override fun crossReferences(book: Int, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> =
    db.bibleDao().crossReferences(book, chapter, verse)

  override suspend fun lemmaOccurrences(
    lang: String,
    lemma: String,
    excludeBook: Int,
    excludeChapter: Int,
    excludeVerse: Int,
    limit: Int,
  ): List<VerseRefRow> =
    db.bibleDao().lemmaOccurrences(lang, "% $lemma %", excludeBook, excludeChapter, excludeVerse, limit)

  override suspend fun lexeme(lang: String, number: String): LexemeEntity? =
    db.bibleDao().lexeme(lang, number)

  override suspend fun saveGlossIt(lang: String, number: String, glossIt: String) {
    db.bibleDao().updateGlossIt(lang, number, glossIt)
  }

  override fun bookInfo(book: Int): Flow<BookInfoEntity?> = db.bibleDao().bookInfo(book)

  override suspend fun saveBookInfo(info: BookInfoEntity) {
    db.bibleDao().insertBookInfo(info)
  }

  override suspend fun ensureImported() {
    val dao = db.bibleDao()
    // Per-translation gate: a translation ships with any app update, so it
    // must import on upgrade too, not only on first launch.
    for (asset in ASSETS) {
      val doc =
        json.decodeFromString<BibleDoc>(Res.readBytes("files/$asset").decodeToString())
      if (dao.translationVerseCount(doc.meta.abbr) > 0) continue
      db.withTransaction {
        // The books table is shared across translations: insert once, from the
        // first imported asset, so names/abbreviations stay stable.
        if (dao.bookCount() == 0) {
          dao.insertBooks(doc.books.map { BookEntity(it.n, it.name, it.abbr, it.chapters) })
        }
        doc.verses.chunked(2000).forEach { chunk ->
          dao.insertVerses(
            chunk.map {
              VerseEntity(
                translation = doc.meta.abbr,
                book = it.b,
                chapter = it.c,
                verse = it.v,
                title = it.t,
                text = it.x,
              )
            },
          )
        }
      }
    }
    if (dao.originalVerseCount() == 0) {
      val doc =
        json.decodeFromString<OriginalDoc>(Res.readBytes("files/$ORIGINALS_ASSET").decodeToString())
      db.withTransaction {
        doc.verses.chunked(3000).forEach { chunk ->
          dao.insertOriginalVerses(
            chunk.map {
              OriginalVerseEntity(
                book = it.b,
                chapter = it.c,
                verse = it.v,
                lang = it.lang,
                text = it.text,
                transliteration = it.tr,
                lemmas = it.lm,
                italianNr = it.anr.joinToString(","),
                italianR2 = it.ar2.joinToString(","),
                italianR27 = it.ar27.joinToString(","),
              )
            },
          )
        }
      }
    }
    if (dao.crossReferenceCount() == 0) {
      val doc =
        json.decodeFromString<CrossRefDoc>(Res.readBytes("files/$CROSSREFS_ASSET").decodeToString())
      db.withTransaction {
        doc.refs.chunked(4000).forEach { chunk ->
          dao.insertCrossReferences(
            chunk.map {
              CrossReferenceEntity(
                fromBook = it[0],
                fromChapter = it[1],
                fromVerse = it[2],
                toBook = it[3],
                toChapter = it[4],
                toVerse = it[5],
              )
            },
          )
        }
      }
    }
    if (dao.lexemeCount() == 0) {
      val doc =
        json.decodeFromString<LexiconDoc>(Res.readBytes("files/$LEXICON_ASSET").decodeToString())
      db.withTransaction {
        doc.he.entries.chunked(2000).forEach { chunk ->
          dao.insertLexemes(
            chunk.map { (number, l) -> LexemeEntity("he", number, l.tr, l.g) },
          )
        }
        doc.el.entries.chunked(2000).forEach { chunk ->
          dao.insertLexemes(
            chunk.map { (number, l) -> LexemeEntity("el", number, l.tr, l.g) },
          )
        }
      }
    }
  }

  companion object {
    private val json = Json { ignoreUnknownKeys = true }
    private val ASSETS = listOf("nuova_riveduta.json", "riveduta_2020.json", "riveduta_1927.json")
    private const val ORIGINALS_ASSET = "originals.json"
    private const val CROSSREFS_ASSET = "crossrefs.json"
    private const val LEXICON_ASSET = "lexicon.json"
  }
}
