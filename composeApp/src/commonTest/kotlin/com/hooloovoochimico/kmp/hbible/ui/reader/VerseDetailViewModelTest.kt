package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.CmsRepository
import com.hooloovoochimico.kmp.hbible.data.InterlinearPosition
import com.hooloovoochimico.kmp.hbible.data.LastPosition
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.local.VotdEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseRefRow
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Copre il comportamento reattivo di [VerseDetailViewModel.aiConfigured]
 * (NEXT_STEPS punto 3b): il blocco "Chiedi all'AI" deve comparire anche nel
 * dettaglio già aperto dopo aver configurato un provider in Impostazioni,
 * senza attendere un ridisegno provocato da altro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerseDetailViewModelTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun aiConfigured_falseWithEmptyConfig() = runTest {
    val viewModel = buildViewModel()
    assertEquals(false, viewModel.aiConfigured.value)
  }

  @Test
  fun aiConfigured_trueWhenAProviderIsEnabledWithKey() = runTest {
    val settings = FakeSettingsRepository()
    val viewModel = buildViewModel(settings)
    assertEquals(false, viewModel.aiConfigured.value)

    settings.aiConfig.value = AiConfig(
      providers = listOf(
        AiProviderConfig(company = AiCompany.Z_AI.name, apiKey = "chiave-finta", enabled = true),
      ),
      order = listOf(AiCompany.Z_AI.name),
    )
    assertEquals(true, viewModel.aiConfigured.value)
  }

  @Test
  fun aiConfigured_falseWhenProviderIsEnabledWithoutKey() = runTest {
    val settings = FakeSettingsRepository()
    val viewModel = buildViewModel(settings)
    settings.aiConfig.value = AiConfig(
      providers = listOf(
        AiProviderConfig(company = AiCompany.Z_AI.name, apiKey = "", enabled = true),
      ),
      order = listOf(AiCompany.Z_AI.name),
    )
    assertEquals(false, viewModel.aiConfigured.value)
  }

  @Test
  fun openAndClose_updateVerseDetail() = runTest {
    val viewModel = buildViewModel()
    assertNull(viewModel.verseDetail.value)
    viewModel.open(listOf(VerseRef(1, 1, 1)))
    val detail = viewModel.verseDetail.filterNotNull().first()
    assertEquals(VerseRef(1, 1, 1), detail.ref)
    viewModel.close()
    assertEquals(null, viewModel.verseDetail.value)
  }

  private fun buildViewModel(
    settings: FakeSettingsRepository = FakeSettingsRepository(),
  ): VerseDetailViewModel =
    VerseDetailViewModel(
      FakeBibleRepository(),
      // Nessun provider all'avvio: il gateway fallisce sempre, per questo test
      // non serve una chiamata vera.
      AiGateway(configProvider = { settings.aiConfig.value }),
      settings,
    )

  private class FakeBibleRepository : BibleRepository {
    override fun books(): Flow<List<BookEntity>> =
      flowOf(listOf(BookEntity(1, "Genesi", "Gen", 50)))

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
    override val aiConfig = MutableStateFlow(AiConfig())

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

    override fun loadAiConfig(): AiConfig = aiConfig.value

    override fun saveAiConfig(config: AiConfig) {
      aiConfig.value = config
    }

    override fun isAiConfigured(): Boolean = aiConfig.value.enabledChain().isNotEmpty()
  }

  private class FakeCmsRepository : CmsRepository {
    override fun votdForToday(): Flow<VotdEntity?> = flowOf(null)

    override fun publishedPosts(): Flow<List<PostEntity>> = flowOf(emptyList())
  }
}
