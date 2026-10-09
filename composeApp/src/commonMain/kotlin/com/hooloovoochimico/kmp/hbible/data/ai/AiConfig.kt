package com.hooloovoochimico.kmp.hbible.data.ai

import com.hooloovoochimico.kmp.hbible.appLog
import com.hooloovoochimico.kmp.hbible.platform.SecretStore
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Config AI: data class pure + [AiSettingsStore] (persistenza cifrata: il JSON
 * in `config_json` vive nel [SecretStore] — Android Keystore / iOS Keychain —,
 * nel file "ai_settings" restano solo le chiavi legacy in migrazione).
 */

/** User configuration for a single AI provider. */
@Serializable
data class AiProviderConfig(
  /** [AiCompany.name]. */
  val company: String,
  val apiKey: String = "",
  /** Blank means "use the provider default model". */
  val model: String = "",
  val enabled: Boolean = false,
)

/**
 * Full AI configuration: per-provider credentials (the user's own keys only) plus the
 * user-chosen priority order. Up to the 2026-10 test builds the CMS also pushed a default
 * service with its key ("CMS" entry + `cms` field): [AiSettingsStore] drops it on load.
 */
@Serializable
data class AiConfig(
  val providers: List<AiProviderConfig> = emptyList(),
  /** [AiCompany.name]s ordered by priority (first = used first). */
  val order: List<String> = emptyList(),
) {
  /** Providers enabled and holding a key, sorted by priority. */
  fun enabledChain(): List<AiProviderConfig> =
    ordered().filter { it.enabled && it.apiKey.isNotBlank() }

  /** All providers sorted by the priority order (missing companies appended last). */
  fun ordered(): List<AiProviderConfig> {
    val byName = providers.associateBy { it.company }
    val names = buildList {
      addAll(order.filter { name -> AiCompany.entries.any { it.name == name } })
      addAll(AiCompany.entries.map { it.name }.filterNot { it in this })
    }
    return names.map { name -> byName[name] ?: AiProviderConfig(company = name) }
  }

  fun configFor(company: AiCompany): AiProviderConfig =
    providers.firstOrNull { it.company == company.name } ?: AiProviderConfig(company = company.name)

  /** The effective model: stored value, or the provider default when blank. */
  fun effectiveModel(company: AiCompany): String =
    configFor(company).model.trim().ifBlank { company.defaultModel }

  /** Returns a copy with [company] replaced by [config] (inserted if missing). */
  fun withProvider(config: AiProviderConfig): AiConfig {
    val others = providers.filterNot { it.company == config.company }
    return copy(providers = others + config)
  }

  /** Returns a copy with the priority order updated so that [company] moves by [delta]. */
  fun move(company: AiCompany, delta: Int): AiConfig {
    val name = company.name
    val names = ordered().map { it.company }.toMutableList()
    val index = names.indexOf(name)
    if (index < 0) return this
    val target = (index + delta).coerceIn(0, names.lastIndex)
    if (target == index) return this
    names.removeAt(index)
    names.add(target, name)
    return copy(order = names)
  }
}

/**
 * Loads/saves [AiConfig] from encrypted storage. `config_json` lives in the
 * [SecretStore]; [settings] ("ai_settings") keeps only the plaintext legacy
 * keys, migrated on first access:
 * - `config_json` written in the clear by previous versions → moved to the
 *   [SecretStore] and removed from [settings];
 * - z.ai-only credentials (`api_key`/`model`) → folded into the config.
 */
object AiSettingsStore {
  const val PREFS = "ai_settings"
  private const val KEY_CONFIG = "config_json"
  private const val LEGACY_KEY_API = "api_key"
  private const val LEGACY_KEY_MODEL = "model"

  private val json = Json { ignoreUnknownKeys = true }

  fun load(secretStore: SecretStore, settings: Settings): AiConfig {
    val raw = readRaw(secretStore, settings)
    val stored = raw?.let { decode(it) } ?: AiConfig()
    val config = migrateLegacy(settings, stored)
    if (config != stored || (raw != null && hasObsoleteEntries(raw, stored))) save(secretStore, config)
    return config.normalize()
  }

  /** True when [raw] still holds the CMS default of old test builds (its key included). */
  private fun hasObsoleteEntries(raw: String, stored: AiConfig): Boolean {
    val known = AiCompany.entries.map { it.name }
    if (stored.providers.any { it.company !in known } || stored.order.any { it !in known }) return true
    return try {
      "cms" in json.parseToJsonElement(raw).jsonObject
    } catch (e: Exception) {
      false
    }
  }

  fun save(secretStore: SecretStore, config: AiConfig) {
    secretStore.put(KEY_CONFIG, json.encodeToString(config.normalize()))
  }

  /** True when at least one provider is enabled with a key set. */
  fun isConfigured(secretStore: SecretStore, settings: Settings): Boolean =
    load(secretStore, settings).enabledChain().isNotEmpty()

  /** Stored JSON, migrating a plaintext `config_json` into [secretStore] if found. */
  private fun readRaw(secretStore: SecretStore, settings: Settings): String? {
    secretStore.get(KEY_CONFIG)?.let { return it }
    settings.getStringOrNull(KEY_CONFIG)?.let { plaintext ->
      secretStore.put(KEY_CONFIG, plaintext)
      settings.remove(KEY_CONFIG)
      return plaintext
    }
    return null
  }

  private fun decode(raw: String): AiConfig? =
    try {
      json.decodeFromString<AiConfig>(raw)
    } catch (e: Exception) {
      appLog.w(e) { "Configurazione AI salvata non leggibile: ignorata" }
      null
    }

  /** Ensures every company has an entry and appears in the priority order; drops unknown
   *  entries (the "CMS" default of old test builds, with the key it carried). */
  internal fun AiConfig.normalize(): AiConfig {
    val known = AiCompany.entries.map { it.name }
    val providers = known.map { name -> this.providers.firstOrNull { it.company == name } ?: AiProviderConfig(company = name) }
    val order = (order.filter { it in known } + known).distinct()
    return copy(providers = providers, order = order)
  }

  private fun migrateLegacy(settings: Settings, config: AiConfig): AiConfig {
    val legacyKey = settings.getStringOrNull(LEGACY_KEY_API).orEmpty()
    if (legacyKey.isBlank()) return config
    val zai = config.configFor(AiCompany.Z_AI)
    val migrated =
      if (zai.apiKey.isBlank()) {
        val legacyModel = settings.getStringOrNull(LEGACY_KEY_MODEL).orEmpty()
        zai.copy(
          apiKey = legacyKey,
          model = legacyModel,
          enabled = true,
        )
      } else {
        zai
      }
    settings.remove(LEGACY_KEY_API)
    settings.remove(LEGACY_KEY_MODEL)
    return config.withProvider(migrated)
  }
}
