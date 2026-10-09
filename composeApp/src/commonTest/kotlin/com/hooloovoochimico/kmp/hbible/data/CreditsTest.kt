package com.hooloovoochimico.kmp.hbible.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ogni traduzione inclusa deve avere licenza e crediti: solo testi a licenza aperta (tasks/README.md). */
class CreditsTest {
  @Test
  fun everyBundledTranslationHasOpenLicenseMetadata() {
    assertEquals(TRANSLATION_NAMES.keys.toList(), TRANSLATION_META.map { it.abbr })
    for (meta in TRANSLATION_META) {
      assertTrue(meta.license.isNotBlank(), "${meta.abbr}: licenza mancante")
      assertTrue(!meta.license.startsWith("©"), "${meta.abbr}: licenza chiusa nell'app")
    }
  }

  @Test
  fun ccBySaTranslationsHaveAttributionAndLicenseLink() {
    for (meta in TRANSLATION_META.filter { it.license.startsWith("CC") }) {
      assertTrue(meta.attribution.isNotBlank(), "${meta.abbr}: attribuzione obbligatoria")
      assertTrue(meta.licenseUrl.startsWith("https://"), "${meta.abbr}: link alla licenza obbligatorio")
    }
  }

  @Test
  fun defaultIsTheOpenBibleAndRemovedTranslationsFallBack() {
    assertEquals("OTB", DEFAULT_TRANSLATION)
    assertEquals("OTB", TRANSLATION_NAMES.keys.first())
    assertEquals(DEFAULT_TRANSLATION, bundledTranslationOrDefault("NR"))
    assertEquals("R27", bundledTranslationOrDefault("R27"))
    assertEquals(DEFAULT_TRANSLATION, bundledTranslationOrDefault(null))
  }

  @Test
  fun sharedOpenBibleVersesCarryTheLicense() {
    assertTrue(shareAttribution("OTB").contains("CC BY-SA 4.0"))
  }
}
