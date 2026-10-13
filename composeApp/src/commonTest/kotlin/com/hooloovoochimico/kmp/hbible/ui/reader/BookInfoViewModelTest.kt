package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.content.PackageFetcher
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.testing.FakeBibleRepository
import com.hooloovoochimico.kmp.hbible.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Info libro dal CMS: con la scheda già scaricata `open()` la mostra con il
 * versetto chiave risolto nella traduzione del lettore, senza scaricare nulla;
 * senza scheda prova il download del pacchetto e, se non arriva, la pagina è
 * "non disponibile".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookInfoViewModelTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(repo: FakeBibleRepository, fetched: MutableList<String>) =
    BookInfoViewModel(
      repo,
      AiGateway(configProvider = { AiConfig() }),
      FakeSettingsRepository(),
      PackageFetcher { path -> fetched += path; false },
    )

  @Test
  fun open_withDownloadedInfo_showsItWithKeyVerseText() = runTest {
    val info = BookInfoEntity(book = 1, theme = "Le origini", contextText = "Genesi apre la Torà…", keyVerse = "Gen 1:1")
    val fetched = mutableListOf<String>()
    val vm = viewModel(FakeBibleRepository(bookInfos = listOf(info)), fetched)
    vm.open(1, "OTB")
    val state = vm.state.first { it.info != null }
    assertEquals(info, state.info)
    assertEquals("In principio Dio creò i cieli e la terra.", state.keyVerse?.text)
    assertFalse(state.loading)
    assertTrue(fetched.isEmpty())
  }

  @Test
  fun open_withoutInfo_triesDownloadThenUnavailable() = runTest {
    val fetched = mutableListOf<String>()
    val vm = viewModel(FakeBibleRepository(), fetched)
    vm.open(1, "OTB")
    val state = vm.state.first { it.unavailable }
    assertEquals(listOf(BOOK_INFO_PACKAGE), fetched)
    assertFalse(state.loading)
    assertEquals(null, state.info)
  }
}
