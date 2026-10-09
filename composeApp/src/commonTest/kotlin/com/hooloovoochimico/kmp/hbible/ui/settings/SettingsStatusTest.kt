package com.hooloovoochimico.kmp.hbible.ui.settings

import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.content.UpdateCheck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsStatusTest {
  private val time: (Long) -> String = { "T$it" }
  private val configured = ContentSyncUiState(baseUrl = "https://example.org")

  @Test
  fun noServer_saysContentIsBundled() {
    val status = ContentSyncUiState().status(time)
    assertEquals("Contenuti inclusi nell'app", status.title)
    assertFalse(status.error)
  }

  @Test
  fun neverChecked_saysNotCheckedYet() {
    assertEquals("Non ancora controllati online", configured.status(time).detail)
  }

  @Test
  fun busy_showsProgress() {
    val status = configured.copy(busy = true, progress = "lessico.json").status(time)
    assertEquals("Aggiornamento in corso…", status.title)
    assertEquals("lessico.json", status.detail)
  }

  @Test
  fun failedCheck_isAnError() {
    val status = configured.copy(lastCheckAt = 5, lastCheck = UpdateCheck(null, emptyList(), "timeout")).status(time)
    assertTrue(status.error)
    assertEquals("Controllo non riuscito", status.title)
  }

  @Test
  fun failedPackages_areAnError() {
    assertTrue(configured.copy(lastCheckAt = 5, lastFailed = 2).status(time).error)
  }

  @Test
  fun appliedPackages_showCountAndTime() {
    val status = configured.copy(lastCheckAt = 5, lastApplied = 3).status(time)
    assertEquals("Contenuti aggiornati", status.title)
    assertEquals("3 pacchetti nuovi · Ultimo controllo: T5", status.detail)
  }

  @Test
  fun upToDate_showsTime() {
    assertEquals("Ultimo controllo: T5", configured.copy(lastCheckAt = 5).status(time).detail)
  }

  @Test
  fun aiSummary_listsProvidersWithKey() {
    assertEquals("", AiConfig().configuredSummary())
    val company = AiCompany.entries.first()
    val config = AiConfig().withProvider(AiProviderConfig(company = company.name, apiKey = "k"))
    assertEquals(company.label, config.configuredSummary())
  }
}
