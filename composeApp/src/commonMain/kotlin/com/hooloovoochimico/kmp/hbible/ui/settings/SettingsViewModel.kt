package com.hooloovoochimico.kmp.hbible.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val dynamicColor: Boolean = true,
  val readerFontSize: ReaderFontSize = ReaderFontSize.NORMAL,
  val aiConfig: AiConfig = AiConfig(),
)

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      SettingsUiState(
        themeMode = settingsRepository.loadThemeMode(),
        dynamicColor = settingsRepository.loadDynamicColor(),
        readerFontSize = settingsRepository.loadFontSize(),
        aiConfig = settingsRepository.loadAiConfig(),
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
}
