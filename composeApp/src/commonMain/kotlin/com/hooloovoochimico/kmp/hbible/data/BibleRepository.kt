package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseRefRow
import com.hooloovoochimico.kmp.hbible.data.local.copyFromBundled
import com.hooloovoochimico.kmp.hbible.data.local.parseAlignment
import com.hooloovoochimico.kmp.hbible.data.local.refreshFromBundledIfChanged
import com.hooloovoochimico.kmp.hbible.platform.withBundledDatabaseFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Display names of the bundled translations, keyed by their database code, in
 * display order. Must match TRANSLATION_ASSETS in tools/build_bible_db.py (the
 * bundled database). Only openly licensed texts: the closed ones are kept in
 * the CMS "cassetto" and never shipped (see tasks/README.md).
 */
val TRANSLATION_NAMES = mapOf(
  "OTB" to "Bibbia Aperta",
  "R27" to "Riveduta 1927",
  "DIO" to "Diodati",
  "MAR" to "Martini",
)

/** Default translation everywhere (reader, interlinear, search, AI): the Bibbia Aperta. */
const val DEFAULT_TRANSLATION = "OTB"

/** [code] if it is a bundled translation, else [DEFAULT_TRANSLATION] (e.g. a removed one saved in settings). */
fun bundledTranslationOrDefault(code: String?): String =
  code?.takeIf { it in TRANSLATION_NAMES } ?: DEFAULT_TRANSLATION

/** Scripture data: books, chapters, verses, original text and cached book info. */
interface BibleRepository {
  fun books(): Flow<List<BookEntity>>

  fun chapter(translation: String, book: Int, chapter: Int): Flow<List<VerseEntity>>

  /** Loads a single verse, used to preview AI search matches. */
  suspend fun verse(translation: String, book: Int, chapter: Int, verse: Int): VerseEntity?

  /** The same verse in every bundled translation that has it (interlinear "versions" list). */
  suspend fun verseAllTranslations(book: Int, chapter: Int, verse: Int): List<VerseEntity>

  /** Original-language text (Hebrew/Greek) for a verse, if available. */
  fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?>

  /** Word alignments of the original verse, by translation code (0-based token indices, -1 = none). */
  fun alignments(book: Int, chapter: Int, verse: Int): Flow<Map<String, List<Int>>>

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

  /** Restores bundled content missing from the user database (new translations, emptied tables). */
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

  override suspend fun verseAllTranslations(
    book: Int,
    chapter: Int,
    verse: Int,
  ): List<VerseEntity> = db.bibleDao().verseAllTranslations(book, chapter, verse)

  override fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?> =
    db.bibleDao().originalVerse(book, chapter, verse)

  override fun crossReferences(book: Int, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> =
    db.bibleDao().crossReferences(book, chapter, verse)

  override fun alignments(book: Int, chapter: Int, verse: Int): Flow<Map<String, List<Int>>> =
    db.bibleDao().alignments(book, chapter, verse).map { rows ->
      rows.associate { it.translation to parseAlignment(it.indices) }
    }

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

  override suspend fun ensureImported() = withContext(Dispatchers.Default) {
    val dao = db.bibleDao()
    // Al primo avvio il driver ha già installato il DB pre-costruito
    // (PrepackagedSQLiteDriver): qui restano solo pochi COUNT. Su un DB
    // esistente le righe mancanti — una traduzione nuova arrivata con un
    // aggiornamento, una tabella svuotata da una migrazione — si ricopiano dal
    // DB incluso con ATTACH, senza più leggere JSON.
    // APK con contenuti nuovi (impronta diversa): ricopia tutti i contenuti inclusi.
    if (withBundledDatabaseFile { path -> db.refreshFromBundledIfChanged(path, TRANSLATION_NAMES.keys.toList()) }) {
      return@withContext
    }
    val translations = TRANSLATION_NAMES.keys.filter { dao.translationVerseCount(it) == 0 }
    val tables =
      buildList {
        if (dao.bookCount() == 0) add("books")
        if (dao.originalVerseCount() == 0) add("original_verses")
        if (dao.alignmentCount() == 0) add("original_alignments")
        if (dao.crossReferenceCount() == 0) add("cross_references")
        if (dao.lexemeCount() == 0) add("lexemes")
      }
    if (translations.isEmpty() && tables.isEmpty()) return@withContext
    withBundledDatabaseFile { path -> db.copyFromBundled(path, translations, tables) }
  }
}
