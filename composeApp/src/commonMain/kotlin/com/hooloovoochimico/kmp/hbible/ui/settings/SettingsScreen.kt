package com.hooloovoochimico.kmp.hbible.ui.settings

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_NAMES
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.label
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.platform.isDynamicColorSupported
import com.hooloovoochimico.kmp.hbible.platform.openUrl
import com.hooloovoochimico.kmp.hbible.ui.common.BookSheetRow
import com.hooloovoochimico.kmp.hbible.ui.common.SectionHeader
import com.hooloovoochimico.kmp.hbible.ui.common.ScreenTitle

private val SOURCES = listOf(
  "Nuova Riveduta 2006 © Società Biblica di Ginevra" to "https://www.laparola.net/",
  "Riveduta 2020 © ADI-Media" to "https://www.laparola.net/",
  "Testo ebraico: Open Scriptures Hebrew Bible (WLC)" to "https://github.com/openscriptures/morphhb",
  "Testo greco: Nestle 1904 (pubblico dominio)" to "https://github.com/biblicalhumanities/Nestle1904",
  "Riferimenti incrociati: OpenBible.info (CC BY 4.0)" to "https://www.openbible.info/labs/cross-references/",
)

/** Full-screen settings page (opened from the bottom bar gear). */
@Composable
fun SettingsScreen(
  selectionTranslation: String,
  onSelectTranslation: (String) -> Unit,
  onOpenAiSettings: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Column(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .imePadding()
      .navigationBarsPadding(),
  ) {
    ScreenTitle("Impostazioni", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 130.dp),
    ) {
      Text(
        "Tema",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(24.dp)) {
        ThemeMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = state.themeMode == mode,
            onClick = { viewModel.setThemeMode(mode) },
            shape =
              SegmentedButtonDefaults.itemShape(
                index = index,
                count = ThemeMode.entries.size,
              ),
          ) { Text(mode.label()) }
        }
      }
      if (isDynamicColorSupported()) {
        Row(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Colori dinamici", style = MaterialTheme.typography.labelLarge)
          Switch(
            checked = state.dynamicColor,
            onCheckedChange = { viewModel.setDynamicColor(it) },
          )
        }
      }
      Text(
        "Dimensione testo",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(24.dp)) {
        ReaderFontSize.entries.forEachIndexed { index, size ->
          SegmentedButton(
            selected = state.readerFontSize == size,
            onClick = { viewModel.setReaderFontSize(size) },
            shape =
              SegmentedButtonDefaults.itemShape(
                index = index,
                count = ReaderFontSize.entries.size,
              ),
          ) { Text(size.label()) }
        }
      }
      Text(
        "Versione",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      TRANSLATION_NAMES.forEach { (code, name) ->
        BookSheetRow(
          name = name,
          abbr = code,
          selected = selectionTranslation == code,
          showReadingBadge = false,
          onClick = { onSelectTranslation(code) },
        )
      }
      SectionHeader("Ricerca AI", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
      val aiConfig = state.aiConfig
      val configured = AiCompany.entries.filter { aiConfig.configFor(it).apiKey.isNotBlank() }
      Text(
        if (configured.isEmpty()) {
          "Nessuna chiave AI impostata"
        } else {
          configured.joinToString(" · ") { company ->
            "${company.label}: chiave impostata (${aiConfig.effectiveModel(company)})"
          }
        },
        style = MaterialTheme.typography.bodySmall,
        color =
          if (configured.isEmpty()) MaterialTheme.colorScheme.error
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
      )
      BookSheetRow(
        name = "Servizi AI",
        abbr = "${configured.size}/${AiCompany.entries.size}",
        selected = false,
        showReadingBadge = false,
        onClick = onOpenAiSettings,
      )
      Text(
        "Chiavi API e priorità dei servizi AI usati per la ricerca e le traduzioni.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )

      SectionHeader("Fonti", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
      Column(Modifier.padding(horizontal = 24.dp)) {
        SOURCES.forEach { (label, url) ->
          Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier =
              Modifier
                .padding(top = 2.dp)
                .clickable { openUrl(url) },
          )
          Spacer(Modifier.height(8.dp))
        }
        Text(
          "Le versioni bibliche sono incluse per uso esclusivamente personale.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
    }
  }
}
