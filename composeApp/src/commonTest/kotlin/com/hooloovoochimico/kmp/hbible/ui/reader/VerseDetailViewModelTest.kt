package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
}
