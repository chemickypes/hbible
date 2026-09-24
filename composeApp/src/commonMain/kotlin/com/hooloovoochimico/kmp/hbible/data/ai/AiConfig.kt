package com.hooloovoochimico.kmp.hbible.data.ai

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Config AI: data class pure + [AiSettingsStore] (persistenza su
 * multiplatform-settings, file "ai_settings" come nel sorgente).
 */

/** User configuration for a single AI provider. */
@Serializable
data class AiProviderConfig(
  /** [AiCompany.name]; stored as string for serialization stability. */
  val company: String,
  val apiKey: String = "",
  /** Blank means "use the provider default model". */
  val model: String = "",
  val enabled: Boolean = false,
)

/** Full AI configuration: per-provider credentials plus the user-chosen priority order. */
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
    val orderedNames = order + AiCompany.entries.map { it.name }.filterNot { it in order }
    return orderedNames.mapNotNull { name ->
      byName[name] ?: AiProviderConfig(company = name)
    }
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
    val names = (order + AiCompany.entries.map { it.name }).distinct().toMutableList()
    val index = names.indexOf(company.name)
    if (index < 0) return this
    val target = (index + delta).coerceIn(0, names.lastIndex)
    if (target == index) return this
    names.removeAt(index)
    names.add(target, company.name)
    return copy(order = names)
  }
}

/**
 * Loads/saves [AiConfig] from settings storage. Migrates the legacy z.ai-only
 * credentials ("api_key"/"model" keys) on first access.
 */
object AiSettingsStore {
  const val PREFS = "ai_settings"
  private const val KEY_CONFIG = "config_json"
  private const val LEGACY_KEY_API = "api_key"
  private const val LEGACY_KEY_MODEL = "model"

  private val json = Json { ignoreUnknownKeys = true }

  fun load(settings: Settings): AiConfig {
    val raw = settings.getStringOrNull(KEY_CONFIG)
    val stored = raw?.let {
      try {
        json.decodeFromString<AiConfig>(it)
      } catch (e: Exception) {
        null
      }
    } ?: AiConfig()
    val config = migrateLegacy(settings, stored)
    if (config != stored) save(settings, config)
    return config.normalize()
  }

  fun save(settings: Settings, config: AiConfig) {
    settings.putString(KEY_CONFIG, json.encodeToString(config.normalize()))
  }

  /** True when at least one provider is enabled with a key set. */
  fun isConfigured(settings: Settings): Boolean = load(settings).enabledChain().isNotEmpty()

  /** Ensures every company has an entry and appears in the priority order. */
  private fun AiConfig.normalize(): AiConfig {
    val known = AiCompany.entries.map { it.name }.toSet()
    val providers = known.map { name -> this.providers.firstOrNull { it.company == name } ?: AiProviderConfig(company = name) }
    val order = (order + known).distinct()
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
