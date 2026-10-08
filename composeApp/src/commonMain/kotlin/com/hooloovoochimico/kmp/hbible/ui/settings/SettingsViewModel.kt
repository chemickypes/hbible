package com.hooloovoochimico.kmp.hbible.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.ai.CMS_COMPANY
import com.hooloovoochimico.kmp.hbible.data.content.ContentSyncer
import com.hooloovoochimico.kmp.hbible.data.content.UpdateCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the content-sync section (Settings → "Aggiornamenti contenuti"). */
data class ContentSyncUiState(
  /** CMS base URL as typed by the user. */
  val baseUrl: String = "",
  val autoUpdateCheck: Boolean = true,
  val busy: Boolean = false,
  /** Progress message during a sync run. */
  val progress: String = "",
  /** Last completed check (updates available / up to date / error). */
  val lastCheck: UpdateCheck? = null,
  /** Result message of the last completed sync. */
  val lastSyncMessage: String = "",
  /** Manifest version currently applied locally ("" = never synced). */
  val localVersion: String = "",
)

data class SettingsUiState(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val dynamicColor: Boolean = true,
  val readerFontSize: ReaderFontSize = ReaderFontSize.NORMAL,
  val aiConfig: AiConfig = AiConfig(),
  val contentSync: ContentSyncUiState = ContentSyncUiState(),
)

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val contentSyncer: ContentSyncer,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      SettingsUiState(
        themeMode = settingsRepository.loadThemeMode(),
        dynamicColor = settingsRepository.loadDynamicColor(),
        readerFontSize = settingsRepository.loadFontSize(),
        aiConfig = settingsRepository.loadAiConfig(),
        contentSync = ContentSyncUiState(
          baseUrl = settingsRepository.loadCmsBaseUrl(),
          autoUpdateCheck = settingsRepository.loadAutoUpdateCheck(),
        ),
      ),
    )
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  fun setThemeMode(mode: ThemeMode) {
    settingsRepository.saveThemeMode(mode)
    _uiState.update { it.copy(themeMode = mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    settingsRepository.saveDynamicColor(enabled)
    _uiState.update { it.copy(dynamicColor = enabled) }
  }

  fun setReaderFontSize(size: ReaderFontSize) {
    settingsRepository.saveFontSize(size)
    _uiState.update { it.copy(readerFontSize = size) }
  }

  // --- AI configuration ---

  fun updateAiConfig(config: AiConfig) {
    settingsRepository.saveAiConfig(config)
    _uiState.update { it.copy(aiConfig = config) }
  }

  fun updateAiProvider(company: AiCompany, change: (AiProviderConfig) -> AiProviderConfig) {
    updateAiProviderNamed(company.name, change)
  }

  /** [updateAiProvider] for any provider name, including the CMS default ([CMS_COMPANY]). */
  fun updateAiProviderNamed(name: String, change: (AiProviderConfig) -> AiProviderConfig) {
    val config = _uiState.value.aiConfig
    val current =
      config.providers.firstOrNull { it.company == name } ?: AiProviderConfig(company = name)
    updateAiConfig(config.withProvider(change(current)))
  }

  fun moveAiProvider(company: AiCompany, delta: Int) {
    updateAiConfig(_uiState.value.aiConfig.move(company, delta))
  }

  fun moveAiProviderNamed(name: String, delta: Int) {
    updateAiConfig(_uiState.value.aiConfig.moveNamed(name, delta))
  }

  /**
   * Fetches the CMS-published default AI and merges it into the config as the
   * [CMS_COMPANY] entry (enable flag and position are kept across refreshes).
   * No-op when the CMS is unreachable; drops the entry when the CMS no longer
   * publishes a default.
   */
  fun refreshCmsAiSettings() {
    viewModelScope.launch {
      val fetch = contentSyncer.fetchAiSettings()
      val config = _uiState.value.aiConfig
      if (!fetch.reachable) return@launch
      if (!fetch.available || fetch.settings == null) {
        if (config.hasCmsEntry() || config.cms != null) {
          updateAiConfig(
            config.copy(
              providers = config.providers.filterNot { it.company == CMS_COMPANY },
              order = config.order.filterNot { it == CMS_COMPANY },
              cms = null,
            ),
          )
        }
        return@launch
      }
      val cms = fetch.settings
      val existing = config.cmsEntry() ?: AiProviderConfig(company = CMS_COMPANY)
      updateAiConfig(
        config
          .copy(cms = cms)
          .withProvider(
            existing.copy(apiKey = cms.apiKey, model = cms.model),
          ),
      )
    }
  }

  // --- Content sync (CMS) ---

  fun setCmsBaseUrl(url: String) {
    settingsRepository.saveCmsBaseUrl(url)
    _uiState.update { it.copy(contentSync = it.contentSync.copy(baseUrl = url)) }
  }

  fun setAutoUpdateCheck(enabled: Boolean) {
    settingsRepository.saveAutoUpdateCheck(enabled)
    _uiState.update { it.copy(contentSync = it.contentSync.copy(autoUpdateCheck = enabled)) }
  }

  fun checkForUpdates() {
    if (!contentSyncer.isConfigured() || _uiState.value.contentSync.busy) return
    refreshCmsAiSettings()
    _uiState.update {
      it.copy(contentSync = it.contentSync.copy(busy = true, progress = "", lastSyncMessage = ""))
    }
    viewModelScope.launch {
      val check = contentSyncer.checkForUpdates()
      _uiState.update {
        it.copy(contentSync = it.contentSync.copy(busy = false, lastCheck = check))
      }
    }
  }

  /** Checks for updates and, when available, downloads and applies them. */
  fun downloadUpdates() {
    if (!contentSyncer.isConfigured() || _uiState.value.contentSync.busy) return
    _uiState.update {
      it.copy(contentSync = it.contentSync.copy(busy = true, progress = "Verifica in corso…", lastSyncMessage = ""))
    }
    viewModelScope.launch {
      val check = contentSyncer.checkForUpdates()
      if (check.error != null) {
        _uiState.update {
          it.copy(
            contentSync = it.contentSync.copy(
              busy = false,
              lastCheck = check,
              lastSyncMessage = "Errore: ${check.error}",
            ),
          )
        }
        return@launch
      }
      if (check.changed.isEmpty()) {
        _uiState.update {
          it.copy(
            contentSync = it.contentSync.copy(
              busy = false,
              lastCheck = check,
              lastSyncMessage = "Contenuti già aggiornati",
            ),
          )
        }
        return@launch
      }
      val result = contentSyncer.sync(check.changed) { progress ->
        _uiState.update { it.copy(contentSync = it.contentSync.copy(progress = progress)) }
      }
      val message =
        buildString {
          append("Applicati ${result.applied.size} pacchetti")
          if (result.failed.isNotEmpty()) {
            append(", ${result.failed.size} non riusciti: ${result.failed.keys.take(3).joinToString()}")
          }
        }
      _uiState.update {
        it.copy(
          contentSync = it.contentSync.copy(
            busy = false,
            progress = "",
            lastCheck = check,
            lastSyncMessage = message,
            localVersion = result.version,
          ),
        )
      }
    }
  }

  /** Silent check at app open; starts the download automatically when enabled. */
  fun autoCheckOnOpen() {
    if (!contentSyncer.isConfigured()) return
    refreshCmsAiSettings()
    if (!settingsRepository.loadAutoUpdateCheck()) return
    if (_uiState.value.contentSync.busy) return
    downloadUpdates()
  }
}
