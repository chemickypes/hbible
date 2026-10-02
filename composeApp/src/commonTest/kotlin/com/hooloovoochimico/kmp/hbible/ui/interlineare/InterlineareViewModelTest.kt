package com.hooloovoochimico.kmp.hbible.ui.interlineare

import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.InterlinearPosition
import com.hooloovoochimico.kmp.hbible.data.LastPosition
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseRefRow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class InterlineareViewModelTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun moveTo_clampsBookChapterVerseToValidBounds() = runTest {
    val viewModel = InterlineareViewModel(FakeBibleRepository(), FakeSettingsRepository())
    // Subscribes the stateIn flows so the books list is loaded before moving.
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.moveTo(1, 60, 10)
    advanceUntilIdle()
    assertEquals(InterlinearPosition(1, 50, 1), viewModel.uiState.value.position)

    viewModel.moveTo(99, 200, 7)
    advanceUntilIdle()
    assertEquals(InterlinearPosition(66, 50, 1), viewModel.uiState.value.position)
  }
}

private class FakeBibleRepository : BibleRepository {
  override fun books(): Flow<List<BookEntity>> =
    flowOf((1..66).map { BookEntity(it, "Libro $it", "L$it", 50) })

  override fun chapter(translation: String, book: Int, chapter: Int): Flow<List<VerseEntity>> =
    flowOf(
      listOf(
        VerseEntity(translation, book, chapter, 1, null, "Nel principio Dio creò i cieli e la terra."),
      ),
    )

  override suspend fun verse(
    translation: String,
    book: Int,
    chapter: Int,
    verse: Int,
  ): VerseEntity? =
    chapter(translation, book, chapter).first().firstOrNull { it.verse == verse }

  override suspend fun verseAllTranslations(
    book: Int,
    chapter: Int,
    verse: Int,
  ): List<VerseEntity> = emptyList()

  override fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?> =
    flowOf(null)

  override fun crossReferences(book: Int, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> =
    flowOf(emptyList())

  override suspend fun lemmaOccurrences(
    lang: String,
    lemma: String,
    excludeBook: Int,
    excludeChapter: Int,
    excludeVerse: Int,
    limit: Int,
  ): List<VerseRefRow> = emptyList()

  override suspend fun lexeme(lang: String, number: String): LexemeEntity? = null

  override suspend fun saveGlossIt(lang: String, number: String, glossIt: String) {}

  override suspend fun ensureImported() {}

  override fun bookInfo(book: Int): Flow<BookInfoEntity?> = flowOf(null)

  override suspend fun saveBookInfo(info: BookInfoEntity) {}
}

private class FakeSettingsRepository : SettingsRepository {
  override fun loadThemeMode(): ThemeMode = ThemeMode.SYSTEM

  override fun saveThemeMode(mode: ThemeMode) {}

  override fun loadDynamicColor(): Boolean = true

  override fun saveDynamicColor(enabled: Boolean) {}

  override fun loadFontSize(): ReaderFontSize = ReaderFontSize.NORMAL

  override fun saveFontSize(size: ReaderFontSize) {}

  override fun loadLastPosition(): LastPosition = LastPosition("NR", 1, 1)

  override fun saveLastPosition(position: LastPosition) {}

  override fun loadInterlinearPosition(): InterlinearPosition = InterlinearPosition(1, 1, 1)

  override fun saveInterlinearPosition(position: InterlinearPosition) {}

  override fun loadCmsBaseUrl(): String = ""

  override fun saveCmsBaseUrl(url: String) {}

  override fun loadAutoUpdateCheck(): Boolean = false

  override fun saveAutoUpdateCheck(enabled: Boolean) {}

  override fun loadAiConfig(): com.hooloovoochimico.kmp.hbible.data.ai.AiConfig =
    com.hooloovoochimico.kmp.hbible.data.ai.AiConfig()

  override fun saveAiConfig(config: com.hooloovoochimico.kmp.hbible.data.ai.AiConfig) {}

  override fun isAiConfigured(): Boolean = false
}
