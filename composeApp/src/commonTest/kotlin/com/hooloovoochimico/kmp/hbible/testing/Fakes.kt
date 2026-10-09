package com.hooloovoochimico.kmp.hbible.testing

import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.CmsRepository
import com.hooloovoochimico.kmp.hbible.data.InterlinearPosition
import com.hooloovoochimico.kmp.hbible.data.LastPosition
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.local.VotdEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseRefRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fakes condivisi dai test dei ViewModel (NEXT_STEPS punto 5b): dati
 * configurabili per versetti, originali, riferimenti e info libro.
 */
internal class FakeBibleRepository(
  books: List<BookEntity> = listOf(BookEntity(1, "Genesi", "Gen", 50)),
  verses: List<VerseEntity> =
    listOf(
      VerseEntity("OTB", 1, 1, 1, null, "In principio Dio creò i cieli e la terra."),
    ),
  originals: List<OriginalVerseEntity> = emptyList(),
  crossRefs: List<CrossReferenceEntity> = emptyList(),
  bookInfos: List<BookInfoEntity> = emptyList(),
  lexemes: List<LexemeEntity> = emptyList(),
) : BibleRepository {

  val savedBookInfo = mutableListOf<BookInfoEntity>()

  private val booksData = books
  private val versesData = verses
  private val originalsData = originals
  private val crossRefsData = crossRefs
  private val bookInfosData = bookInfos
  private val lexemesData = lexemes

  override fun books(): Flow<List<BookEntity>> = flowOf(booksData)

  override fun chapter(translation: String, book: Int, chapter: Int): Flow<List<VerseEntity>> =
    flowOf(
      versesData.filter {
        it.translation == translation && it.book == book && it.chapter == chapter
      },
    )

  override suspend fun verse(translation: String, book: Int, chapter: Int, verse: Int): VerseEntity? =
    versesData.firstOrNull {
      it.translation == translation && it.book == book && it.chapter == chapter && it.verse == verse
    }

  override suspend fun verseAllTranslations(book: Int, chapter: Int, verse: Int): List<VerseEntity> =
    versesData.filter { it.book == book && it.chapter == chapter && it.verse == verse }

  override fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?> =
    flowOf(originalsData.firstOrNull { it.book == book && it.chapter == chapter && it.verse == verse })

  /** Alignments by "book:chapter:verse" → translation → indices. */
  var alignmentsData: Map<String, Map<String, List<Int>>> = emptyMap()

  override fun alignments(book: Int, chapter: Int, verse: Int): Flow<Map<String, List<Int>>> =
    flowOf(alignmentsData["$book:$chapter:$verse"].orEmpty())

  override fun crossReferences(book: Int, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> =
    flowOf(
      crossRefsData.filter { it.fromBook == book && it.fromChapter == chapter && it.fromVerse == verse },
    )

  override suspend fun lemmaOccurrences(
    lang: String,
    lemma: String,
    excludeBook: Int,
    excludeChapter: Int,
    excludeVerse: Int,
    limit: Int,
  ): List<VerseRefRow> = emptyList()

  override suspend fun lexeme(lang: String, number: String): LexemeEntity? =
    lexemesData.firstOrNull { it.lang == lang && it.number == number }

  override suspend fun saveGlossIt(lang: String, number: String, glossIt: String) {}

  override suspend fun ensureImported() {}

  override fun bookInfo(book: Int): Flow<BookInfoEntity?> =
    flowOf(bookInfosData.firstOrNull { it.book == book })

  override suspend fun saveBookInfo(info: BookInfoEntity) {
    savedBookInfo.add(info)
  }
}

internal class FakeSettingsRepository : SettingsRepository {
  override val aiConfig = MutableStateFlow(AiConfig())

  override fun loadThemeMode(): ThemeMode = ThemeMode.SYSTEM

  override fun saveThemeMode(mode: ThemeMode) {}

  override fun loadDynamicColor(): Boolean = true

  override fun saveDynamicColor(enabled: Boolean) {}

  override fun loadFontSize(): ReaderFontSize = ReaderFontSize.NORMAL

  override fun saveFontSize(size: ReaderFontSize) {}

  override fun loadLastPosition(): LastPosition = LastPosition("OTB", 1, 1)

  override fun saveLastPosition(position: LastPosition) {}

  override fun loadInterlinearPosition(): InterlinearPosition = InterlinearPosition(1, 1, 1)

  override fun saveInterlinearPosition(position: InterlinearPosition) {}

  override fun loadCmsBaseUrl(): String = ""

  override fun saveCmsBaseUrl(url: String) {}

  override fun loadAutoUpdateCheck(): Boolean = false

  override fun saveAutoUpdateCheck(enabled: Boolean) {}

  override fun loadAiConfig(): AiConfig = aiConfig.value

  override fun saveAiConfig(config: AiConfig) {
    aiConfig.value = config
  }

  override fun isAiConfigured(): Boolean = aiConfig.value.enabledChain().isNotEmpty()
}

internal class FakeCmsRepository(
  votd: VotdEntity? = null,
  posts: List<PostEntity> = emptyList(),
) : CmsRepository {
  private val votdData = votd
  private val postsData = posts

  override fun votdForToday(): Flow<VotdEntity?> = flowOf(votdData)

  override fun publishedPosts(): Flow<List<PostEntity>> = flowOf(postsData)
}
