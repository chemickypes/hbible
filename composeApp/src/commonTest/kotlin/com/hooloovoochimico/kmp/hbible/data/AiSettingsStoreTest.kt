package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiSettingsStore
import com.hooloovoochimico.kmp.hbible.platform.SecretStore
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiSettingsStoreTest {

  private class FakeSecretStore : SecretStore {
    val map = mutableMapOf<String, String>()
    val removed = mutableListOf<String>()
    override fun get(key: String): String? = map[key]
    override fun put(key: String, value: String) {
      map[key] = value
    }

    override fun remove(key: String) {
      map.remove(key)
      removed.add(key)
    }
  }

  /** In-memory Settings (MapSettings non è nell'artefatto principale). */
  private class FakeSettings : Settings {
    private val map = mutableMapOf<String, Any>()
    override val keys: Set<String> get() = map.keys.toSet()
    override val size: Int get() = map.size
    override fun clear() = map.clear()
    override fun remove(key: String) {
      map.remove(key)
    }

    override fun hasKey(key: String): Boolean = map.containsKey(key)
    override fun putInt(key: String, value: Int) {
      map[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int = (map[key] as? Int) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = map[key] as? Int
    override fun putLong(key: String, value: Long) {
      map[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long = (map[key] as? Long) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = map[key] as? Long
    override fun putString(key: String, value: String) {
      map[key] = value
    }

    override fun getString(key: String, defaultValue: String): String = (map[key] as? String) ?: defaultValue
    override fun getStringOrNull(key: String): String? = map[key] as? String
    override fun putFloat(key: String, value: Float) {
      map[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float = (map[key] as? Float) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = map[key] as? Float
    override fun putDouble(key: String, value: Double) {
      map[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double = (map[key] as? Double) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
    override fun putBoolean(key: String, value: Boolean) {
      map[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (map[key] as? Boolean) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
  }

  @Test
  fun `save then load returns the same config`() {
    val store = FakeSecretStore()
    val settings = FakeSettings()
    val config =
      AiConfig(
        providers =
          listOf(
            AiProviderConfig(AiCompany.Z_AI.name, apiKey = "k1", model = "m1", enabled = true),
            AiProviderConfig(AiCompany.GEMINI.name, apiKey = "k2", enabled = false),
          ),
        order = listOf(AiCompany.Z_AI.name, AiCompany.GEMINI.name),
      )
    AiSettingsStore.save(store, config)
    val loaded = AiSettingsStore.load(store, settings)
    assertEquals(with(AiSettingsStore) { config.normalize() }, loaded)
    assertEquals(
      listOf(AiCompany.Z_AI.name, AiCompany.GEMINI.name, AiCompany.OPENAI.name, AiCompany.ANTHROPIC.name),
      loaded.order,
    )
    assertEquals("k1", loaded.configFor(AiCompany.Z_AI).apiKey)
    assertTrue(AiSettingsStore.isConfigured(store, settings))
  }

  @Test
  fun `plaintext config_json is moved into the secret store and removed from settings`() {
    val store = FakeSecretStore()
    val settings = FakeSettings()
    val plaintext =
      """{"providers":[{"company":"Z_AI","apiKey":"legacy","enabled":true}],"order":["Z_AI"]}"""
    settings.putString("config_json", plaintext)
    assertTrue(store.map.isEmpty())

    val loaded = AiSettingsStore.load(store, settings)

    assertEquals("legacy", loaded.configFor(AiCompany.Z_AI).apiKey)
    assertTrue(loaded.configFor(AiCompany.Z_AI).enabled)
    assertEquals(plaintext, store.map["config_json"])
    assertFalse(settings.hasKey("config_json"))
    // Ricaricando non trova più nulla in Settings e non sposta nulla.
    val reloaded = AiSettingsStore.load(store, settings)
    assertEquals("legacy", reloaded.configFor(AiCompany.Z_AI).apiKey)
  }

  @Test
  fun `legacy api_key and model are folded into the encrypted config`() {
    val store = FakeSecretStore()
    val settings = FakeSettings()
    settings.putString("api_key", "old-zai-key")
    settings.putString("model", "glm-4")

    val loaded = AiSettingsStore.load(store, settings)

    val zai = loaded.configFor(AiCompany.Z_AI)
    assertEquals("old-zai-key", zai.apiKey)
    assertEquals("glm-4", zai.model)
    assertTrue(zai.enabled)
    assertFalse(settings.hasKey("api_key"))
    assertFalse(settings.hasKey("model"))
    assertTrue(store.map.containsKey("config_json"))
  }

  @Test
  fun `cms default of old test builds is dropped with its key`() {
    val store = FakeSecretStore()
    val settings = FakeSettings()
    store.map["config_json"] =
      """{"providers":[{"company":"Z_AI","apiKey":"mine","enabled":true},""" +
        """{"company":"CMS","apiKey":"cms-secret","enabled":true}],""" +
        """"order":["CMS","Z_AI"],"cms":{"provider":"GEMINI","apiKey":"cms-secret"}}"""

    val loaded = AiSettingsStore.load(store, settings)

    assertTrue(loaded.providers.none { it.company == "CMS" })
    assertFalse("CMS" in loaded.order)
    assertEquals(listOf(AiCompany.Z_AI.name), loaded.enabledChain().map { it.company })
    assertFalse(store.map.getValue("config_json").contains("cms-secret"))
  }

  @Test
  fun `empty storage loads default config`() {
    val store = FakeSecretStore()
    val settings = FakeSettings()
    val loaded = AiSettingsStore.load(store, settings)
    assertEquals(with(AiSettingsStore) { AiConfig().normalize() }, loaded)
    assertFalse(AiSettingsStore.isConfigured(store, settings))
    assertNull(store.map["config_json"])
  }
}
