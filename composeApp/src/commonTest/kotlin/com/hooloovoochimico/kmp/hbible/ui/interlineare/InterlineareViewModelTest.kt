package com.hooloovoochimico.kmp.hbible.ui.interlineare

import com.hooloovoochimico.kmp.hbible.data.InterlinearPosition
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseRefRow
import com.hooloovoochimico.kmp.hbible.testing.FakeBibleRepository
import com.hooloovoochimico.kmp.hbible.testing.FakeSettingsRepository
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val viewModel =
      InterlineareViewModel(
        FakeBibleRepository(books = (1..66).map { BookEntity(it, "Libro $it", "L$it", 50) }),
        FakeSettingsRepository(),
        aiGateway(),
      )
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

  @Test
  fun selectWord_loadsLexiconEntryAndLemmaOccurrences() = runTest {
    val viewModel = viewModelWithVerse()
    subscribe(viewModel)

    viewModel.selectWord(0)
    advanceUntilIdle()

    val selected = viewModel.selectedWord.value
    assertEquals(0, selected?.index)
    assertEquals("he", selected?.lang)
    val info = selected!!.info
    assertEquals("בְּרֵאשִׁית", info.word)
    assertEquals("bereshit", info.transliteration)
    assertEquals("in principio", info.gloss)
    assertEquals("principio", info.italianWord)
    assertEquals("7225", info.lemma)
    assertEquals("principio", info.lexeme?.glossIt)
    assertEquals(listOf(VerseRef(1, 1, 5)), info.occurrences)
    assertEquals(false, info.occurrencesLoading)
  }

  @Test
  fun selectWord_withoutLemmaHasNoLexiconAndNoOccurrences() = runTest {
    val viewModel = viewModelWithVerse()
    subscribe(viewModel)

    viewModel.selectWord(1)
    advanceUntilIdle()

    val info = viewModel.selectedWord.value!!.info
    assertNull(info.lemma)
    assertNull(info.lexeme)
    assertEquals(emptyList(), info.occurrences)
    assertEquals(false, info.occurrencesLoading)
  }

  @Test
  fun clearWord_andMovingToAnotherVerse_closeTheCard() = runTest {
    val viewModel = viewModelWithVerse()
    subscribe(viewModel)

    viewModel.selectWord(0)
    advanceUntilIdle()
    assertEquals(0, viewModel.selectedWord.value?.index)

    viewModel.clearWord()
    advanceUntilIdle()
    assertNull(viewModel.selectedWord.value)

    viewModel.selectWord(0)
    advanceUntilIdle()
    viewModel.moveTo(1, 1, 2)
    advanceUntilIdle()
    assertNull(viewModel.selectedWord.value)
  }

  /** Subscribes the flows used by the tests (stateIn needs a collector to start). */
  private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: InterlineareViewModel) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.selectedWord.collect {} }
    advanceUntilIdle()
  }

  /** Verse 1:1 (Hebrew, two words, the second without lemma) and verse 1:2 in NTA. */
  private fun kotlinx.coroutines.test.TestScope.viewModelWithVerse(): InterlineareViewModel {
    val original =
      OriginalVerseEntity(
        book = 1,
        chapter = 1,
        verse = 1,
        lang = "he",
        text = "בְּרֵאשִׁית -",
        transliteration = "bereshit -",
        lemmas = "7225 -",
        glosses = "in the beginning\tcreated",
        glossesIt = "in principio\t",
      )
    val repository =
      FakeBibleRepository(
        books = listOf(BookEntity(1, "Genesi", "Gen", 50)),
        verses =
          listOf(
            VerseEntity("NTA", 1, 1, 1, null, "In principio Dio creò i cieli e la terra."),
            VerseEntity("NTA", 1, 1, 2, null, "La terra era informe."),
          ),
        originals = listOf(original),
        lexemes = listOf(LexemeEntity("he", "7225", "reshit", "beginning", glossIt = "principio")),
      ).apply {
        lemmaOccurrencesData = listOf(VerseRefRow(1, 1, 5))
        alignmentsData = mapOf("1:1:1" to mapOf("NTA" to listOf(1, 3)))
      }
    return InterlineareViewModel(repository, FakeSettingsRepository(), aiGateway())
  }

  private fun aiGateway() = AiGateway(configProvider = { AiConfig() })
}
