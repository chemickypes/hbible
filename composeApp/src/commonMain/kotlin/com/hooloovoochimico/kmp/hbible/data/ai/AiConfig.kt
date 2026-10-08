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
  /** [AiCompany.name], or [CMS_COMPANY] for the CMS-provided default. */
  val company: String,
  val apiKey: String = "",
  /** Blank means "use the provider default model". */
  val model: String = "",
  val enabled: Boolean = false,
)

/**
 * Pseudo provider name for the default AI service pushed by the CMS
 * ("casa madre" + token + modello configurati sul portale). The real
 * provider/model live in [AiConfig.cms]; the matching entry in [AiConfig.providers]
 * only carries the user's enable flag and position in the priority order.
 */
const val CMS_COMPANY = "CMS"

/** Default AI service published by the CMS (see GET /api/ai/settings). */
@Serializable
data class CmsAiSettings(
  /** [AiCompany.name] of the underlying provider. */
  val provider: String,
  val apiKey: String = "",
  /** Blank means "use the provider default model". */
  val model: String = "",
)

/** Full AI configuration: per-provider credentials plus the user-chosen priority order. */
@Serializable
data class AiConfig(
  val providers: List<AiProviderConfig> = emptyList(),
  /** [AiCompany.name]s (plus [CMS_COMPANY]) ordered by priority (first = used first). */
  val order: List<String> = emptyList(),
  /** Default service offered by the CMS, null when the CMS offers none. */
  val cms: CmsAiSettings? = null,
) {
  /** True when a CMS default entry exists among the providers. */
  fun hasCmsEntry(): Boolean = providers.any { it.company == CMS_COMPANY }

  /** The CMS default entry as configured by the user (enable flag), if present. */
  fun cmsEntry(): AiProviderConfig? = providers.firstOrNull { it.company == CMS_COMPANY }
  /** Providers enabled and holding a key, sorted by priority. */
  fun enabledChain(): List<AiProviderConfig> =
    ordered().filter { it.enabled && it.apiKey.isNotBlank() }

  /** All providers sorted by the priority order (missing companies appended last). */
  fun ordered(): List<AiProviderConfig> {
    val byName = providers.associateBy { it.company }
    val names = buildList {
      addAll(order.filter { it != CMS_COMPANY || byName.containsKey(CMS_COMPANY) })
      addAll(AiCompany.entries.map { it.name }.filterNot { it in this })
      if (byName.containsKey(CMS_COMPANY) && CMS_COMPANY !in this) add(CMS_COMPANY)
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
  fun move(company: AiCompany, delta: Int): AiConfig = moveNamed(company.name, delta)

  /** [move] for any provider name, including [CMS_COMPANY]. */
  fun moveNamed(name: String, delta: Int): AiConfig {
    val pool =
      buildList {
        addAll(order)
        addAll(AiCompany.entries.map { it.name })
        if (hasCmsEntry()) add(CMS_COMPANY)
      }.distinct()
    val names = pool.toMutableList()
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

  /** Ensures every company has an entry and appears in the priority order (the
   *  CMS default entry is kept only while it exists — it is managed by the CMS). */
  private fun AiConfig.normalize(): AiConfig {
    val known =
      AiCompany.entries.map { it.name }.toSet() +
        (if (cms != null || hasCmsEntry()) setOf(CMS_COMPANY) else emptySet())
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
