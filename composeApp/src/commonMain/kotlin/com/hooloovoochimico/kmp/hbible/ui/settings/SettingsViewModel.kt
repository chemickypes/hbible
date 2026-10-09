package com.hooloovoochimico.kmp.hbible.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.content.ContentSyncer
import com.hooloovoochimico.kmp.hbible.data.content.UpdateCheck
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** State of the content-sync section (Settings → "Contenuti"). */
data class ContentSyncUiState(
  /** Content server base URL (developer setting, Settings → Avanzate in debug builds). */
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
  /** Time of the last successful check (epoch millis, 0 = never). */
  val lastCheckAt: Long = 0L,
  /** Packages applied / failed by the last sync. */
  val lastApplied: Int = 0,
  val lastFailed: Int = 0,
)

/** One-line summary of the content state shown in Settings (title + detail). */
data class ContentStatus(val title: String, val detail: String, val error: Boolean = false)

fun ContentSyncUiState.status(
  formatTime: (Long) -> String = { formatDate(it, "d MMM yyyy, HH:mm") },
): ContentStatus {
  val checkedAt = if (lastCheckAt > 0) "Ultimo controllo: ${formatTime(lastCheckAt)}" else ""
  return when {
    baseUrl.isBlank() -> ContentStatus("Contenuti inclusi nell'app", "Aggiornamenti online non disponibili")
    busy -> ContentStatus("Aggiornamento in corso…", progress.ifBlank { "Controllo dei contenuti" })
    lastCheck?.error != null ->
      ContentStatus("Controllo non riuscito", "Server dei contenuti non raggiungibile. Riprova più tardi.", error = true)
    lastFailed > 0 ->
      ContentStatus("Aggiornamento incompleto", "$lastFailed pacchetti non applicati. Riprova più tardi.", error = true)
    lastCheckAt == 0L -> ContentStatus("Contenuti inclusi nell'app", "Non ancora controllati online")
    lastApplied > 0 ->
      ContentStatus("Contenuti aggiornati", listOf("$lastApplied pacchetti nuovi", checkedAt).joinToString(" · "))
    else -> ContentStatus("Contenuti aggiornati", checkedAt)
  }
}

/** Names of the AI providers with a key set ("" when the assistant is not configured). */
fun AiConfig.configuredSummary(): String =
  AiCompany.entries.filter { configFor(it).apiKey.isNotBlank() }.joinToString(" · ") { it.label }

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
          lastCheckAt = settingsRepository.loadLastContentCheck(),
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
    val config = _uiState.value.aiConfig
    updateAiConfig(config.withProvider(change(config.configFor(company))))
  }

  fun moveAiProvider(company: AiCompany, delta: Int) {
    updateAiConfig(_uiState.value.aiConfig.move(company, delta))
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
    _uiState.update {
      it.copy(contentSync = it.contentSync.copy(busy = true, progress = "", lastSyncMessage = ""))
    }
    viewModelScope.launch {
      val check = contentSyncer.checkForUpdates()
      val checkedAt = recordCheck(check)
      _uiState.update {
        it.copy(contentSync = it.contentSync.copy(busy = false, lastCheck = check, lastCheckAt = checkedAt))
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
      val checkedAt = recordCheck(check)
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
              lastCheckAt = checkedAt,
              lastApplied = 0,
              lastFailed = 0,
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
            lastCheckAt = checkedAt,
            lastApplied = result.applied.size,
            lastFailed = result.failed.size,
          ),
        )
      }
    }
  }

  /** Saves the time of a successful [check]; returns the last successful check time. */
  private fun recordCheck(check: UpdateCheck): Long {
    if (check.error != null) return _uiState.value.contentSync.lastCheckAt
    val now = Clock.System.now().toEpochMilliseconds()
    settingsRepository.saveLastContentCheck(now)
    return now
  }

  /** Silent check at app open; starts the download automatically when enabled. */
  fun autoCheckOnOpen() {
    if (!contentSyncer.isConfigured()) return
    if (!settingsRepository.loadAutoUpdateCheck()) return
    if (_uiState.value.contentSync.busy) return
    downloadUpdates()
  }
}
