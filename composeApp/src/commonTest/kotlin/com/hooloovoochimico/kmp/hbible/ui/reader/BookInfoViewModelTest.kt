package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.testing.FakeBibleRepository
import com.hooloovoochimico.kmp.hbible.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Info libro (NEXT_STEPS punto 5b): con l'introduzione in cache `open(1)`
 * deve mostrare subito la cache senza occuparsi; senza cache e senza AI
 * configurata la generazione fallisce con un errore visibile.
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

  @Test
  fun open_withCachedInfo_showsItWithoutBusy() = runTest {
    val cached =
      BookInfoEntity(
        book = 1,
        contextText = "Genesi apre la Torà…",
        protagonists = "Adamo, Eva, Noè",
        christocentric = "La promessa del seme",
        model = "glm-4.6",
        updatedAt = 100L,
      )
    val viewModel =
      BookInfoViewModel(
        FakeBibleRepository(bookInfos = listOf(cached)),
        AiGateway(configProvider = { AiConfig() }),
        FakeSettingsRepository(),
      )
    viewModel.open(1)
    val state = viewModel.state.first { it.info != null }
    assertEquals(1, state.book)
    assertEquals(cached, state.info)
    assertFalse(state.busy)
  }

  @Test
  fun open_withoutCacheAndWithoutAI_setsError() = runTest {
    val viewModel =
      BookInfoViewModel(
        // Nessun provider configurato: il gateway fallisce sempre.
        FakeBibleRepository(),
        AiGateway(configProvider = { AiConfig() }),
        FakeSettingsRepository(),
      )
    viewModel.open(1)
    val state = viewModel.state.first { it.error != null }
    assertEquals(1, state.book)
    assertNotNull(state.error)
    assertFalse(state.busy)
    assertEquals(null, state.info)
  }
}
