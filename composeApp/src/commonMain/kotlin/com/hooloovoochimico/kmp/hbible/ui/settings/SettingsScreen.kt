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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_META
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_NAMES
import com.hooloovoochimico.kmp.hbible.data.TranslationMeta
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
  "Testo ebraico: Open Scriptures Hebrew Bible (WLC)" to "https://github.com/openscriptures/morphhb",
  "Testo greco: Nestle 1904 (pubblico dominio)" to "https://github.com/biblicalhumanities/Nestle1904",
  "Riferimenti incrociati: OpenBible.info (CC BY 4.0)" to "https://www.openbible.info/labs/cross-references/",
)

/** Info block of one bundled translation (Settings → "Versioni della Bibbia"). */
@Composable
private fun TranslationMetaRow(meta: TranslationMeta) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(meta.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(
      meta.year,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (meta.personalUse) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Text(
          "Uso personale",
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
  if (meta.fullName != meta.name) {
    Text(
      meta.fullName,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  Text(
    meta.license,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  if (meta.source.isNotBlank()) {
    val isLink = meta.source.startsWith("http")
    Text(
      meta.source,
      style = MaterialTheme.typography.bodySmall,
      color =
        if (isLink) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      textDecoration = if (isLink) TextDecoration.Underline else null,
      modifier = if (isLink) Modifier.clickable { openUrl(meta.source) } else Modifier,
    )
  }
  if (meta.note.isNotBlank()) {
    Text(
      meta.note,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  Spacer(Modifier.height(12.dp))
}

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

      SectionHeader("Aggiornamenti contenuti", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
      val syncState = state.contentSync
      Column(Modifier.padding(horizontal = 24.dp)) {
        OutlinedTextField(
          value = syncState.baseUrl,
          onValueChange = { viewModel.setCmsBaseUrl(it) },
          label = { Text("URL del CMS") },
          placeholder = { Text("http://192.168.1.10:3001") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Controllo automatico all'apertura", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = syncState.autoUpdateCheck,
            onCheckedChange = { viewModel.setAutoUpdateCheck(it) },
          )
        }
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { viewModel.checkForUpdates() },
            enabled = !syncState.busy && syncState.baseUrl.isNotBlank(),
          ) {
            Text("Verifica")
          }
          Button(
            onClick = { viewModel.downloadUpdates() },
            enabled = !syncState.busy && syncState.baseUrl.isNotBlank(),
          ) {
            Text(
              when {
                syncState.busy -> "Aggiornamento…"
                else -> "Scarica aggiornamenti"
              },
            )
          }
        }
        if (syncState.busy && syncState.progress.isNotBlank()) {
          Text(
            syncState.progress,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
        val lastCheck = syncState.lastCheck
        if (lastCheck != null && !syncState.busy) {
          Text(
            when {
              lastCheck.error != null -> "Errore di verifica: ${lastCheck.error}"
              lastCheck.changed.isEmpty() -> "Contenuti aggiornati (versione ${lastCheck.version})"
              else ->
                "${lastCheck.changed.size} pacchetti aggiornati disponibili: " +
                  lastCheck.changed.take(4).joinToString() +
                  if (lastCheck.changed.size > 4) "…" else ""
            },
            style = MaterialTheme.typography.bodySmall,
            color =
              if (lastCheck.error != null) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
        if (syncState.lastSyncMessage.isNotBlank() && !syncState.busy) {
          Text(
            syncState.lastSyncMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
        Text(
          "Il CMS distribuisce bibbie, interlineare, lessico e riferimenti. " +
            "I contenuti restano memorizzati sul dispositivo e funzionano offline.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
      }

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
      SectionHeader("Versioni della Bibbia", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
      Column(Modifier.padding(horizontal = 24.dp)) {
        TRANSLATION_META.forEach { meta -> TranslationMetaRow(meta) }
        Text(
          "Le versioni contrassegnate «Uso personale» sono testi protetti inclusi solo per uso personale (repository privata, nessuna distribuzione).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
