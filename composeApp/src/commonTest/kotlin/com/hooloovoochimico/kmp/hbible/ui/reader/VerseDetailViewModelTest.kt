package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
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

  @Test
  fun verseDetail_versionsFollowTranslationOrderAndSkipMissingText() = runTest {
    val viewModel =
      buildViewModel(
        repository =
          FakeBibleRepository(
            verses =
              listOf(
                // MAR (Martini) manca per questo versetto: deve essere saltata.
                VerseEntity("DIO", 1, 1, 1, null, "Diodati"),
                VerseEntity("NTA", 1, 1, 1, null, "Nuova"),
                VerseEntity("R27", 1, 1, 1, null, "Riveduta"),
              ),
          ),
      )
    viewModel.open(listOf(VerseRef(1, 1, 1)))
    val detail = viewModel.verseDetail.filterNotNull().first()
    // Ordine di TRANSLATION_META (NTA, R27, DIO, MAR); la traduzione corrente resta
    // nell'elenco, lo schermo la filtra.
    assertEquals(listOf("NTA", "R27", "DIO"), detail.versions.map { it.translation })
    assertEquals(listOf("Nuova", "Riveduta", "Diodati"), detail.versions.map { it.text })
  }

  private fun buildViewModel(
    settings: FakeSettingsRepository = FakeSettingsRepository(),
    repository: FakeBibleRepository = FakeBibleRepository(),
  ): VerseDetailViewModel =
    VerseDetailViewModel(
      repository,
      // Nessun provider all'avvio: il gateway fallisce sempre, per questo test
      // non serve una chiamata vera.
      AiGateway(configProvider = { settings.aiConfig.value }),
      settings,
    )
}
