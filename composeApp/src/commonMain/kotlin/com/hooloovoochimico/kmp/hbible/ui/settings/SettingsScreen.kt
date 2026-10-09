package com.hooloovoochimico.kmp.hbible.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.APP_LICENSE
import com.hooloovoochimico.kmp.hbible.data.CREDITS
import com.hooloovoochimico.kmp.hbible.data.CreditEntry
import com.hooloovoochimico.kmp.hbible.data.FEEDBACK_URL
import com.hooloovoochimico.kmp.hbible.data.PRIVACY_POLICY_URL
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.SOFTWARE_CREDITS
import com.hooloovoochimico.kmp.hbible.data.SOURCE_CODE_URL
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_META
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_NAMES
import com.hooloovoochimico.kmp.hbible.data.ThemeMode
import com.hooloovoochimico.kmp.hbible.data.TranslationMeta
import com.hooloovoochimico.kmp.hbible.data.label
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.platform.appVersion
import com.hooloovoochimico.kmp.hbible.platform.isDebugBuild
import com.hooloovoochimico.kmp.hbible.platform.isDynamicColorSupported
import com.hooloovoochimico.kmp.hbible.platform.openUrl
import com.hooloovoochimico.kmp.hbible.platform.storePageUrl
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import com.hooloovoochimico.kmp.hbible.ui.common.ScreenTitle
import com.hooloovoochimico.kmp.hbible.ui.settings.components.SegmentedItem
import com.hooloovoochimico.kmp.hbible.ui.settings.components.SettingsGroup
import com.hooloovoochimico.kmp.hbible.ui.settings.components.SettingsItem
import com.hooloovoochimico.kmp.hbible.ui.settings.components.SettingsTrailing
import com.hooloovoochimico.kmp.hbible.ui.settings.components.SwitchItem

/** Sub-pages of Settings: full screen on phones, right pane on wide windows. */
enum class SettingsPage(val title: String) {
  VERSIONS("Versioni della Bibbia"),
  CREDITS("Crediti e licenze"),
  ADVANCED("Avanzate"),
}

/** Width of the settings list next to the sub-page pane on wide windows. */
private val ListPaneWidth = 400.dp

/** Psalm 23:1 (Bibbia Aperta) as text-size preview. */
private const val PREVIEW_VERSE = "Il Signore è il mio pastore, nulla mi mancherà."

/** Settings tab (R05): grouped list, sub-pages for versions, credits and developer options. */
@Composable
fun SettingsScreen(
  selectionTranslation: String,
  onSelectTranslation: (String) -> Unit,
  onOpenAiSettings: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
  var showTranslationSheet by rememberSaveable { mutableStateOf(false) }

  BoxWithConstraints(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .imePadding()
      .navigationBarsPadding(),
  ) {
    val twoPane = maxWidth >= Dimens.contentMaxWidth
    BackHandler(enabled = true) { if (page != null && !twoPane) page = null else onDismiss() }

    val list: @Composable (Modifier, SettingsPage?) -> Unit = { listModifier, selectedPage ->
      SettingsList(
        state = state,
        translationName = TRANSLATION_NAMES[selectionTranslation].orEmpty(),
        selectedPage = selectedPage,
        onOpenPage = { page = it },
        onOpenTranslations = { showTranslationSheet = true },
        onOpenAiSettings = onOpenAiSettings,
        viewModel = viewModel,
        modifier = listModifier,
      )
    }

    if (twoPane) {
      val shown = page ?: SettingsPage.VERSIONS
      Row(Modifier.fillMaxSize()) {
        list(Modifier.width(ListPaneWidth).fillMaxHeight(), shown)
        VerticalDivider()
        SettingsSubPage(shown, onBack = null, state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
      }
    } else {
      AnimatedContent(
        targetState = page,
        transitionSpec = {
          if (targetState != null) {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
          } else {
            slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
          }
        },
      ) { shown ->
        if (shown == null) {
          list(Modifier.fillMaxSize(), null)
        } else {
          SettingsSubPage(shown, onBack = { page = null }, state = state, viewModel = viewModel)
        }
      }
    }
  }

  if (showTranslationSheet) {
    TranslationSheet(
      selected = selectionTranslation,
      onSelect = {
        onSelectTranslation(it)
        showTranslationSheet = false
      },
      onDismiss = { showTranslationSheet = false },
    )
  }
}

@Composable
private fun SettingsList(
  state: SettingsUiState,
  translationName: String,
  selectedPage: SettingsPage?,
  onOpenPage: (SettingsPage) -> Unit,
  onOpenTranslations: () -> Unit,
  onOpenAiSettings: () -> Unit,
  viewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  Column(modifier.verticalScroll(rememberScrollState())) {
    ScreenTitle(
      "Impostazioni",
      Modifier.padding(horizontal = 20.dp, vertical = 12.dp).semantics { heading() },
    )

    SettingsGroup(
      "Lettura",
      items =
        buildList {
          add {
            SettingsItem(
              SettingsIcons.MenuBook,
              "Traduzione",
              subtitle = "Nel lettore",
              value = translationName,
              trailing = SettingsTrailing.CHEVRON,
              onClick = onOpenTranslations,
            )
          }
          add {
            SegmentedItem(
              SettingsIcons.FormatSize,
              "Dimensione del testo",
              options = ReaderFontSize.entries,
              selected = state.readerFontSize,
              label = { it.label() },
              onSelect = viewModel::setReaderFontSize,
            ) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
              ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                  Text(PREVIEW_VERSE, style = ScriptureTypography.reader(state.readerFontSize))
                  Text(
                    "Salmo 23:1 · Bibbia Aperta",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                  )
                }
              }
            }
          }
          add {
            SegmentedItem(
              SettingsIcons.DarkMode,
              "Tema",
              options = ThemeMode.entries,
              selected = state.themeMode,
              label = { it.label() },
              onSelect = viewModel::setThemeMode,
            )
          }
          if (isDynamicColorSupported()) {
            add {
              SwitchItem(
                SettingsIcons.Palette,
                "Colori dinamici",
                checked = state.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
                subtitle = "Usa i colori dello sfondo del telefono",
              )
            }
          }
        },
    )

    val aiSummary = state.aiConfig.configuredSummary()
    SettingsGroup(
      "Assistente AI",
      items =
        listOf {
          SettingsItem(
            SettingsIcons.AutoAwesome,
            "Assistente AI",
            subtitle = aiSummary.ifBlank { "Non configurato · serve una tua chiave API" },
            subtitleColor =
              if (aiSummary.isBlank()) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
            trailing = SettingsTrailing.CHEVRON,
            onClick = onOpenAiSettings,
          )
        },
    )

    val sync = state.contentSync
    val status = sync.status()
    SettingsGroup(
      "Contenuti",
      items =
        listOf(
          {
            SwitchItem(
              SettingsIcons.Sync,
              "Aggiornamenti automatici",
              checked = sync.autoUpdateCheck,
              onCheckedChange = viewModel::setAutoUpdateCheck,
              subtitle = "Scarica le correzioni di testo e interlineare all'apertura dell'app",
            )
          },
          {
            SettingsItem(
              if (status.error) AppIcons.Info else SettingsIcons.CheckCircle,
              status.title,
              subtitle = status.detail,
              subtitleColor =
                if (status.error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
              when {
                sync.busy -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                sync.baseUrl.isNotBlank() ->
                  FilledTonalButton(onClick = viewModel::downloadUpdates) { Text("Controlla ora") }
              }
            }
          },
        ),
    )

    SettingsGroup(
      "Informazioni",
      items =
        buildList {
          add {
            SettingsItem(
              SettingsIcons.LibraryBooks,
              SettingsPage.VERSIONS.title,
              subtitle = "${TRANSLATION_META.size} traduzioni libere o di pubblico dominio",
              trailing = SettingsTrailing.CHEVRON,
              selected = selectedPage == SettingsPage.VERSIONS,
              onClick = { onOpenPage(SettingsPage.VERSIONS) },
            )
          }
          add {
            SettingsItem(
              SettingsIcons.Description,
              SettingsPage.CREDITS.title,
              subtitle = "Testi originali, lessico, software",
              trailing = SettingsTrailing.CHEVRON,
              selected = selectedPage == SettingsPage.CREDITS,
              onClick = { onOpenPage(SettingsPage.CREDITS) },
            )
          }
          add {
            SettingsItem(
              SettingsIcons.Shield,
              "Informativa sulla privacy",
              trailing = SettingsTrailing.EXTERNAL,
              onClick = { openUrl(PRIVACY_POLICY_URL) },
            )
          }
          add {
            SettingsItem(
              SettingsIcons.Feedback,
              "Scrivi un feedback",
              subtitle = "Segnala un errore o proponi una correzione",
              trailing = SettingsTrailing.EXTERNAL,
              onClick = { openUrl(FEEDBACK_URL) },
            )
          }
          storePageUrl()?.let { url ->
            add {
              SettingsItem(
                SettingsIcons.Star,
                "Valuta l'app",
                subtitle = "Sul Play Store",
                trailing = SettingsTrailing.EXTERNAL,
                onClick = { openUrl(url) },
              )
            }
          }
          add { SettingsItem(AppIcons.Info, "Versione", subtitle = appVersion()) }
        },
    )

    if (isDebugBuild()) {
      SettingsGroup(
        "Sviluppo",
        items =
          listOf {
            SettingsItem(
              SettingsIcons.Build,
              SettingsPage.ADVANCED.title,
              subtitle = "Solo build di debug · server dei contenuti",
              trailing = SettingsTrailing.CHEVRON,
              selected = selectedPage == SettingsPage.ADVANCED,
              onClick = { onOpenPage(SettingsPage.ADVANCED) },
            )
          },
      )
    }
    Spacer(Modifier.height(LocalBottomBarClearance.current))
  }
}

/** Sub-page with its own top bar; [onBack] null = shown in the right pane (no back arrow). */
@Composable
private fun SettingsSubPage(
  page: SettingsPage,
  onBack: (() -> Unit)?,
  state: SettingsUiState,
  viewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    Row(
      Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (onBack != null) {
        IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, contentDescription = "Indietro") }
      } else {
        Spacer(Modifier.width(16.dp))
      }
      Text(page.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
    Column(
      Modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      when (page) {
        SettingsPage.VERSIONS -> VersionsPage()
        SettingsPage.CREDITS -> CreditsPage()
        SettingsPage.ADVANCED -> AdvancedPage(state.contentSync, viewModel)
      }
      Spacer(Modifier.height(LocalBottomBarClearance.current))
    }
  }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
  }
}

@Composable
private fun PageSectionTitle(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() },
  )
}

@Composable
private fun LicenseBadge(license: String, url: String) {
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = if (url.isNotBlank()) Modifier.clickable(role = Role.Button) { openUrl(url) } else Modifier,
  ) {
    Text(license, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
  }
}

@Composable
private fun LinkText(text: String, url: String) {
  Text(
    text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.clickable(role = Role.Button) { openUrl(url) }.padding(vertical = 6.dp),
  )
}

@Composable
private fun VersionsPage() {
  Text(
    "HBible include solo testi con licenza aperta o di pubblico dominio.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 4.dp),
  )
  TRANSLATION_META.forEach { meta -> InfoCard { TranslationMetaBlock(meta) } }
}

/** Info block of one bundled translation. */
@Composable
private fun TranslationMetaBlock(meta: TranslationMeta) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(meta.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(meta.year, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LicenseBadge(meta.license, meta.licenseUrl)
  }
  val secondary = MaterialTheme.colorScheme.onSurfaceVariant
  if (meta.fullName != meta.name) Text(meta.fullName, style = MaterialTheme.typography.bodyMedium, color = secondary)
  if (meta.note.isNotBlank()) Text(meta.note, style = MaterialTheme.typography.bodyMedium, color = secondary)
  if (meta.attribution.isNotBlank()) {
    Text(meta.attribution, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = secondary)
  }
  if (meta.source.startsWith("http")) {
    LinkText(meta.source, meta.source)
  } else if (meta.source.isNotBlank()) {
    Text(meta.source, style = MaterialTheme.typography.bodyMedium, color = secondary)
  }
}

@Composable
private fun CreditRow(credit: CreditEntry, showScope: Boolean = true) {
  Row(
    Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button) { openUrl(credit.url) }
      .heightIn(min = 56.dp)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(credit.title, style = MaterialTheme.typography.bodyLarge)
      Text(
        if (showScope) "${credit.scope} · ${credit.license}" else credit.license,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      SettingsIcons.OpenInNew,
      contentDescription = "Apre il browser",
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
private fun CreditsPage() {
  PageSectionTitle("Testi biblici")
  val attributed = TRANSLATION_META.filter { it.attribution.isNotBlank() }
  val publicDomain = TRANSLATION_META.filter { it.attribution.isBlank() }
  InfoCard {
    attributed.forEach { meta ->
      Text(meta.attribution, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
      if (meta.licenseUrl.isNotBlank()) LinkText("Licenza ${meta.license}", meta.licenseUrl)
    }
    if (publicDomain.isNotEmpty()) {
      Text(
        publicDomain.joinToString(", ") { it.name } + ": " + publicDomain.first().license.lowercase() + ".",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
  PageSectionTitle("Testi originali e strumenti")
  InfoCard { CREDITS.forEach { CreditRow(it) } }
  PageSectionTitle("Software")
  InfoCard { SOFTWARE_CREDITS.forEach { CreditRow(it, showScope = false) } }
  PageSectionTitle("HBible")
  InfoCard {
    Text("Codice sorgente con licenza $APP_LICENSE.", style = MaterialTheme.typography.bodyMedium)
    LinkText(SOURCE_CODE_URL.removePrefix("https://"), SOURCE_CODE_URL)
  }
}

/** Developer options (debug builds only): content server URL and manual sync. */
@Composable
private fun AdvancedPage(sync: ContentSyncUiState, viewModel: SettingsViewModel) {
  OutlinedTextField(
    value = sync.baseUrl,
    onValueChange = viewModel::setCmsBaseUrl,
    label = { Text("Indirizzo del server dei contenuti") },
    placeholder = { Text("http://192.168.1.10:3001") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
  )
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = viewModel::checkForUpdates, enabled = !sync.busy && sync.baseUrl.isNotBlank()) {
      Text("Verifica")
    }
    Button(onClick = viewModel::downloadUpdates, enabled = !sync.busy && sync.baseUrl.isNotBlank()) {
      Text(if (sync.busy) "Aggiornamento…" else "Scarica aggiornamenti")
    }
  }
  val secondary = MaterialTheme.colorScheme.onSurfaceVariant
  if (sync.busy && sync.progress.isNotBlank()) {
    Text(sync.progress, style = MaterialTheme.typography.bodySmall, color = secondary)
  }
  val lastCheck = sync.lastCheck
  if (lastCheck != null && !sync.busy) {
    Text(
      when {
        lastCheck.error != null -> "Errore di verifica: ${lastCheck.error}"
        lastCheck.changed.isEmpty() -> "Contenuti aggiornati (versione ${lastCheck.version})"
        else ->
          "${lastCheck.changed.size} pacchetti aggiornati disponibili: " +
            lastCheck.changed.take(4).joinToString() + if (lastCheck.changed.size > 4) "…" else ""
      },
      style = MaterialTheme.typography.bodySmall,
      color = if (lastCheck.error != null) MaterialTheme.colorScheme.error else secondary,
    )
  }
  if (sync.lastSyncMessage.isNotBlank() && !sync.busy) {
    Text(sync.lastSyncMessage, style = MaterialTheme.typography.bodySmall, color = secondary)
  }
  if (sync.localVersion.isNotBlank()) {
    Text("Versione dei contenuti: ${sync.localVersion}", style = MaterialTheme.typography.bodySmall, color = secondary)
  }
  Text(
    "Visibile solo nelle build di debug. In release l'app usa l'indirizzo pubblico dei contenuti.",
    style = MaterialTheme.typography.bodySmall,
    color = secondary,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Text(
      "Traduzione",
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
    )
    Column(Modifier.padding(bottom = 24.dp)) {
      TRANSLATION_META.forEach { meta ->
        val isSelected = meta.abbr == selected
        Row(
          Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(meta.abbr) }
            .heightIn(min = 64.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          RadioButton(selected = isSelected, onClick = null)
          Column(Modifier.weight(1f)) {
            Text(meta.name, style = MaterialTheme.typography.bodyLarge)
            Text(
              meta.fullName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Box { LicenseBadge(meta.license, url = "") }
        }
      }
    }
  }
}
