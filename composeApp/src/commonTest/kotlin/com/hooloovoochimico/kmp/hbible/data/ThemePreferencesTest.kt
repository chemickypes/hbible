package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.testing.InMemorySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemePreferencesTest {

  @Test
  fun `la label di BLUE e Blu`() {
    assertEquals("Blu", ThemeMode.BLUE.label())
  }

  @Test
  fun `BLUE si salva e si rilegge`() {
    val settings = InMemorySettings()
    ThemePreferences.save(settings, ThemeMode.BLUE)
    assertEquals(ThemeMode.BLUE, ThemePreferences.load(settings))
  }

  @Test
  fun `un valore sconosciuto torna a SYSTEM`() {
    val settings = InMemorySettings()
    ThemePreferences.save(settings, ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, ThemePreferences.load(settings))
    settings.putString("theme_mode", "VIOLA")
    assertEquals(ThemeMode.SYSTEM, ThemePreferences.load(settings))
  }

  @Test
  fun `senza valore salvato si parte da SYSTEM`() {
    assertEquals(ThemeMode.SYSTEM, ThemePreferences.load(InMemorySettings()))
  }

  @Test
  fun `le funzioni IA sono spente di default e si accendono e spengono`() {
    val settings = InMemorySettings()
    assertFalse(ThemePreferences.loadAiFeaturesEnabled(settings))
    ThemePreferences.saveAiFeaturesEnabled(settings, true)
    assertTrue(ThemePreferences.loadAiFeaturesEnabled(settings))
    ThemePreferences.saveAiFeaturesEnabled(settings, false)
    assertFalse(ThemePreferences.loadAiFeaturesEnabled(settings))
  }
}
