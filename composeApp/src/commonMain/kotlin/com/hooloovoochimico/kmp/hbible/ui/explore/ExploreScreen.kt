package com.hooloovoochimico.kmp.hbible.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.BibleReferenceParser
import com.hooloovoochimico.kmp.hbible.data.SearchHistoryEntry
import com.hooloovoochimico.kmp.hbible.data.SearchScope
import com.hooloovoochimico.kmp.hbible.data.aiReportUrl
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import com.hooloovoochimico.kmp.hbible.platform.openUrl
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyState
import com.hooloovoochimico.kmp.hbible.ui.common.GroupHeader
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import com.hooloovoochimico.kmp.hbible.ui.common.ScreenTitle
import com.hooloovoochimico.kmp.hbible.ui.common.TonalIcon
import com.hooloovoochimico.kmp.hbible.ui.common.groupedShape
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsIcons

/** Shared duration of the AI-mode transitions (bar morph + filter chips fade). */
private const val SEARCH_TRANSITION_MS = 500

/** Horizontal margin of the screen content. */
private val ScreenPadding = 16.dp

/** Recent searches shown before "Mostra tutte". */
private const val RECENT_COLLAPSED = 4

/** Example query with the kind shown on its card. */
private data class SearchExample(val kind: String, val text: String)

private val SEARCH_EXAMPLES =
  listOf(
    SearchExample("Riferimento", "1 Gv 1:3"),
    SearchExample("Capitolo", "Sal 23"),
    SearchExample("Libro", "cantico dei cantici 2"),
    SearchExample("Parola", "amore"),
    SearchExample("Frase", "non temere"),
  )

/** Tematiche del pronto soccorso: versetti e riflessioni per i momenti difficili. */
private val FIRST_AID_THEMES =
  listOf(
    "Ansia", "Paura", "Burnout", "Stress", "Dolore", "Morte", "Perdita e lutto",
    "Sconfitta", "Solitudine", "Sconforto", "Malattia", "Tentazione", "Perdono", "Rabbia",
  )

private val AI_EXAMPLES =
  listOf(
    "cerco il versetto che dice che l'amore è paziente",
    "Gesù calma la tempesta sul mare",
    "il buon pastore che dà la vita per le pecore",
    "non temere, io sono con te",
  )

private val SEARCH_SCOPES =
  listOf(
    SearchScope.ALL,
    SearchScope.AT,
    SearchScope.NT,
    SearchScope.VANGELI,
    SearchScope.LETTERE,
    SearchScope.STORICI,
    SearchScope.PROFETI,
    SearchScope.SALMI,
    SearchScope.PENTATEUCO,
  )

/** Four-point sparkle used as the AI search toggle. */
private val AiIcon: ImageVector =
  ImageVector.Builder(
    name = "AiSpark",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  )
    .path(fill = SolidColor(Color.Black)) {
      moveTo(12f, 2f)
      lineTo(14.4f, 9.6f)
      lineTo(22f, 12f)
      lineTo(14.4f, 14.4f)
      lineTo(12f, 22f)
      lineTo(9.6f, 14.4f)
      lineTo(2f, 12f)
      lineTo(9.6f, 9.6f)
      close()
    }
    .build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
  translation: String,
  translationName: String,
  onDismiss: () -> Unit,
  onGoToVerse: (Int, Int, Int) -> Unit,
  onOpenNote: (Long) -> Unit,
  onSaveReflectionToNote: (String, String) -> Unit,
  focusSearchRequest: Int = 0,
  modifier: Modifier = Modifier,
  viewModel: ExploreViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val books by viewModel.books.collectAsStateWithLifecycle()
  val aiResults by viewModel.aiResults.collectAsStateWithLifecycle()

  // Verse previews are loaded in the translation the reader is showing.
  LaunchedEffect(translation) { viewModel.setTranslation(translation) }

  val keyboard = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }
  val query = state.query
  val aiMode = state.aiMode
  val bookNames = remember(books) { books.associate { it.n to it.name } }

  // Only pop the keyboard for a fresh search; a restored one shows its results.
  LaunchedEffect(Unit) { if (query.isEmpty()) focusRequester.requestFocus() }

  // Ctrl+F dal guscio (6e): ogni incremento della richiesta porta il focus
  // sul campo di ricerca.
  LaunchedEffect(focusSearchRequest) {
    if (focusSearchRequest > 0) focusRequester.requestFocus()
  }

  Box(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      // Consume taps on empty areas so they never reach the reader screen below.
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) {}
      .statusBarsPadding()
      .imePadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(Modifier.fillMaxHeight().widthIn(max = Dimens.contentMaxWidth)) {
      ScreenTitle(
        "Esplora",
        Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).semantics { heading() },
      )
      Text(
        if (aiMode) {
          "Descrivi con parole tue il versetto che cerchi: l'AI trova i riferimenti più " +
            "probabili (Tutta la Bibbia, $translationName)."
        } else {
          "Cerca nel testo, vai a un passo o chiedi all'AI"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp),
      )
      SearchBar(
        query = query,
        aiMode = aiMode,
        noteMode = state.noteMode,
        aiSearching = state.aiSearching,
        focusRequester = focusRequester,
        onQueryChange = viewModel::onQueryChange,
        onToggleAiMode = viewModel::onToggleAiMode,
        onSearch = {
          if (aiMode) {
            viewModel.runAiSearch()
          } else {
            viewModel.recordQuery(query)
            keyboard?.hide()
          }
        },
        onRunAiSearch = viewModel::runAiSearch,
      )
      AnimatedVisibility(
        visible = !aiMode,
        enter =
          fadeIn(tween(SEARCH_TRANSITION_MS)) +
            expandVertically(expandFrom = Alignment.Top, animationSpec = tween(SEARCH_TRANSITION_MS)),
        exit =
          fadeOut(tween(SEARCH_TRANSITION_MS)) +
            shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(SEARCH_TRANSITION_MS)),
      ) {
        ScopeChips(
          scope = state.scope,
          noteMode = state.noteMode,
          onScopeChange = viewModel::onScopeChange,
          onToggleNoteMode = viewModel::onToggleNoteMode,
        )
      }
      Spacer(Modifier.height(4.dp))

      val listModifier = Modifier.weight(1f).fillMaxWidth()
      val listPadding = PaddingValues(bottom = LocalBottomBarClearance.current)
      when {
        query.trim().isEmpty() ->
          LazyColumn(listModifier, contentPadding = listPadding) {
            exploreHome(
              aiMode = aiMode,
              aiSearching = state.aiSearching,
              history = state.history,
              onFirstAid = viewModel::onFirstAid,
              onRestoreHistory = {
                keyboard?.hide()
                viewModel.restoreHistory(it)
              },
              onClearHistory = viewModel::clearHistory,
              onExample = { example ->
                viewModel.onQueryChange(example)
                if (aiMode) viewModel.runAiSearch() else viewModel.recordQuery(example)
              },
            )
          }
        aiMode -> {
          val savedAi = state.ai
          val aiActive = savedAi != null && savedAi.query == query.trim()
          val activeAiError = savedAi?.takeIf { it.query == query.trim() }?.error
          val r = aiResults
          when {
            state.aiSearching ->
              Column(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                CircularProgressIndicator()
                Text(
                  "L'AI sta cercando i versetti...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 16.dp),
                )
              }
            activeAiError != null ->
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 8.dp),
              ) {
                Text(
                  activeAiError,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(16.dp),
                )
              }
            !aiActive ->
              Text(
                "Premi il tasto di invio per far cercare l'AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
              )
            r == null -> Unit
            r.perfect.isEmpty() && r.similar.isEmpty() ->
              EmptyState(AiIcon, "Nessun risultato", hint = "Prova a descrivere il passo con altre parole.")
            else ->
              LazyColumn(listModifier.navigationBarsPadding(), contentPadding = listPadding) {
                val reflection = savedAi?.matches?.riflessione.orEmpty()
                if (reflection.isNotBlank()) {
                  item(key = "first-aid-card") {
                    val themeLabel = (savedAi?.query.orEmpty()).replaceFirstChar { it.uppercase() }
                    FirstAidCard(
                      theme = themeLabel,
                      reflection = reflection,
                      onSaveToNote = { onSaveReflectionToNote(themeLabel, reflection) },
                    )
                  }
                }
                aiSection("perfect", "Corrispondenze perfette", r.perfect, bookNames, onGoToVerse)
                aiSection("similar", "Simili", r.similar, bookNames, onGoToVerse)
              }
          }
        }
        state.noteMode -> {
          ResultsCount("${state.noteResults.size} note trovate")
          when {
            state.searching -> SearchProgress()
            state.noteResults.isEmpty() ->
              EmptyState(AppIcons.Edit, "Nessuna nota trovata", hint = "Nessuna nota corrisponde alla ricerca.")
            else ->
              LazyColumn(
                listModifier.navigationBarsPadding(),
                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = LocalBottomBarClearance.current),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(state.noteResults, key = { "note-${it.id}" }) { note ->
                  NoteResultCard(note, query) {
                    keyboard?.hide()
                    onOpenNote(note.id)
                  }
                }
              }
          }
        }
        else -> {
          val ref = remember(query, books) { BibleReferenceParser.parse(query, books) }
          ref?.let { r ->
            GoToPassageCard(
              label = "${bookNames[r.book]} ${r.chapter}${r.verse?.let { ":$it" } ?: ""}",
              onClick = {
                keyboard?.hide()
                viewModel.recordQuery(query)
                onGoToVerse(r.book, r.chapter, r.verse ?: 1)
              },
            )
          }
          if (state.searching) {
            SearchProgress()
          } else {
            if (state.results.isEmpty()) {
              // A reference ("Gv 3:16") rarely matches the text: the passage card is enough.
              if (ref == null) {
                EmptyState(
                  AppIcons.Search,
                  "Nessun risultato",
                  hint = "Prova con meno parole, cambia ambito o chiedi all'AI.",
                )
              }
            } else {
              val limit = if (state.results.size == SEARCH_LIMIT) "+" else ""
              ResultsCount("${state.results.size}$limit risultati · $translationName")
              LazyColumn(listModifier.navigationBarsPadding(), contentPadding = listPadding) {
                itemsIndexed(
                  state.results,
                  key = { _, it -> "${it.translation}-${it.book}-${it.chapter}-${it.verse}" },
                ) { index, verse ->
                  if (index > 0) ResultDivider()
                  VerseResultRow(
                    reference = "${bookNames[verse.book]} ${verse.chapter}:${verse.verse}",
                    text = verse.text.replace("\n", " "),
                    query = query,
                    onClick = {
                      keyboard?.hide()
                      viewModel.recordQuery(query)
                      onGoToVerse(verse.book, verse.chapter, verse.verse)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private const val SEARCH_LIMIT = 300

/** Search field that morphs between a pill (text search) and a prompt box (AI). */
@Composable
private fun SearchBar(
  query: String,
  aiMode: Boolean,
  noteMode: Boolean,
  aiSearching: Boolean,
  focusRequester: FocusRequester,
  onQueryChange: (String) -> Unit,
  onToggleAiMode: () -> Unit,
  onSearch: () -> Unit,
  onRunAiSearch: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  // Shared pieces of the trailing controls; in AI mode they are drawn as overlays
  // instead of icon slots so they never get vertically centered.
  val clearQueryButton: @Composable () -> Unit = {
    if (query.isNotEmpty()) {
      // Persist the cleared state so reopening shows the empty screen.
      IconButton(onClick = { onQueryChange("") }) {
        Icon(AppIcons.Close, contentDescription = "Cancella query")
      }
    }
  }
  val fieldDivider: @Composable () -> Unit = {
    Box(Modifier.width(1.dp).height(24.dp).background(colors.outlineVariant))
  }
  val aiModeToggle: @Composable () -> Unit = {
    IconButton(
      onClick = onToggleAiMode,
      colors =
        IconButtonDefaults.iconButtonColors(
          containerColor = if (aiMode) colors.primary else Color.Transparent,
          contentColor = if (aiMode) colors.onPrimary else colors.primary,
        ),
    ) {
      Icon(AiIcon, contentDescription = if (aiMode) "Disattiva ricerca AI" else "Attiva ricerca AI")
    }
  }

  // The pill radius follows the measured field height so it stays circular at any
  // font scale; the AI prompt box is flatter and outlined in the accent color.
  val density = LocalDensity.current
  var fieldHeight by remember { mutableStateOf(56.dp) }
  val radius by animateDpAsState(
    targetValue = if (aiMode) 20.dp else fieldHeight / 2,
    animationSpec = tween(SEARCH_TRANSITION_MS),
    label = "searchFieldRadius",
  )
  val borderColor by animateColorAsState(
    targetValue = if (aiMode) colors.primary else Color.Transparent,
    animationSpec = tween(SEARCH_TRANSITION_MS),
    label = "searchFieldBorder",
  )
  val shape = RoundedCornerShape(radius)

  Box(
    Modifier
      .fillMaxWidth()
      .padding(horizontal = ScreenPadding)
      .onSizeChanged { fieldHeight = with(density) { it.height.toDp() } },
  ) {
    TextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = {
        Text(
          when {
            aiMode -> "Descrivi il versetto che cerchi..."
            noteMode -> "Cerca nelle note…"
            else -> "Testo o riferimento (1 Gv 1:3)"
          },
          maxLines = if (aiMode) 3 else 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      singleLine = !aiMode,
      // 4 righe: lo spazio per ✦ in alto e per l'invio in basso senza che si tocchino.
      minLines = if (aiMode) 4 else 1,
      maxLines = if (aiMode) 6 else 1,
      // Tighter line spacing while composing the AI prompt.
      textStyle =
        if (aiMode) {
          MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp)
        } else {
          LocalTextStyle.current
        },
      shape = shape,
      // Hidden in AI mode to leave as much room as possible for the prompt.
      leadingIcon =
        if (aiMode) {
          null
        } else {
          { Icon(AppIcons.Search, contentDescription = null, tint = colors.primary) }
        },
      trailingIcon = {
        if (aiMode) {
          // Reserve the width of the overlaid controls so the prompt text never
          // flows beneath them (clear + divider + sparkle).
          Spacer(Modifier.width(if (query.isEmpty()) 49.dp else 97.dp))
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            clearQueryButton()
            fieldDivider()
            aiModeToggle()
          }
        }
      },
      colors =
        TextFieldDefaults.colors(
          focusedContainerColor = colors.surfaceContainerHigh,
          unfocusedContainerColor = colors.surfaceContainerHigh,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent,
        ),
      keyboardOptions = KeyboardOptions(imeAction = if (aiMode) ImeAction.Send else ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch() }, onSend = { onSearch() }),
      modifier =
        Modifier
          .fillMaxWidth()
          .border(1.5.dp, borderColor, shape)
          .focusRequester(focusRequester),
    )
    if (aiMode) {
      // Icon slots center vertically in multiline fields, so the controls are drawn
      // as overlays pinned to the corners instead.
      Row(
        Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        clearQueryButton()
        fieldDivider()
        aiModeToggle()
      }
      IconButton(
        onClick = onRunAiSearch,
        enabled = query.isNotBlank() && !aiSearching,
        colors =
          IconButtonDefaults.iconButtonColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
          ),
        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp, end = 4.dp),
      ) {
        Icon(AppIcons.Send, contentDescription = "Avvia ricerca AI")
      }
    }
  }
}

/** Scope filters of the text search plus the switch to the notes search. */
@Composable
private fun ScopeChips(
  scope: SearchScope,
  noteMode: Boolean,
  onScopeChange: (SearchScope) -> Unit,
  onToggleNoteMode: () -> Unit,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = ScreenPadding, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SEARCH_SCOPES.forEach { s ->
      ExploreChip(label = s.label, selected = !noteMode && scope == s, onClick = { onScopeChange(s) })
    }
    Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
    ExploreChip(label = "Note", selected = noteMode, icon = AppIcons.Edit, onClick = onToggleNoteMode)
  }
}

/** Borderless filter chip: tonal when idle, filled with the accent color when selected. */
@Composable
private fun ExploreChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  icon: ImageVector? = null,
) {
  val colors = MaterialTheme.colorScheme
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label) },
    shape = CircleShape,
    leadingIcon =
      icon?.let {
        { Icon(it, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
      },
    colors =
      FilterChipDefaults.filterChipColors(
        containerColor = colors.surfaceContainer,
        labelColor = colors.onSurfaceVariant,
        iconColor = colors.onSurfaceVariant,
        selectedContainerColor = colors.primary,
        selectedLabelColor = colors.onPrimary,
        selectedLeadingIconColor = colors.onPrimary,
      ),
    border =
      FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = Color.Transparent,
        selectedBorderColor = Color.Transparent,
      ),
  )
}

/** Start page (empty query): first aid, recent searches and examples. */
private fun LazyListScope.exploreHome(
  aiMode: Boolean,
  aiSearching: Boolean,
  history: List<SearchHistoryEntry>,
  onFirstAid: (String) -> Unit,
  onRestoreHistory: (SearchHistoryEntry) -> Unit,
  onClearHistory: () -> Unit,
  onExample: (String) -> Unit,
) {
  item(key = "pronto-soccorso") {
    ProntoSoccorsoSection(
      enabled = !aiSearching,
      onTheme = onFirstAid,
      modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
    )
  }
  if (history.isNotEmpty()) {
    item(key = "recenti") {
      RecentSearches(history, onRestoreHistory, onClearHistory)
    }
  }
  item(key = "esempi") {
    Column(Modifier.padding(top = 16.dp)) {
      GroupHeader(
        "Prova a cercare",
        Modifier.padding(horizontal = 24.dp).padding(bottom = 10.dp),
      )
      LazyRow(
        contentPadding = PaddingValues(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (aiMode) {
          items(AI_EXAMPLES, key = { it }) { ExampleCard("Descrizione", it, wide = true) { onExample(it) } }
        } else {
          items(SEARCH_EXAMPLES, key = { it.text }) { ExampleCard(it.kind, it.text) { onExample(it.text) } }
        }
      }
    }
  }
}

/** Recent searches as a segmented card, collapsed to the latest few. */
@Composable
private fun RecentSearches(
  history: List<SearchHistoryEntry>,
  onRestore: (SearchHistoryEntry) -> Unit,
  onClear: () -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val shown = if (expanded) history else history.take(RECENT_COLLAPSED)
  Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 16.dp)) {
    GroupHeader("Recenti", Modifier.padding(start = 8.dp)) {
      TextButton(onClick = onClear) { Text("Cancella") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      shown.forEachIndexed { index, entry ->
        Surface(
          onClick = { onRestore(entry) },
          shape = groupedShape(index, shown.size),
          color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            if (entry.aiMode) {
              TonalIcon(AiIcon, size = 36.dp)
            } else {
              TonalIcon(
                AppIcons.Search,
                size = 36.dp,
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(Modifier.weight(1f)) {
              Text(
                entry.query,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                "${if (entry.aiMode) "Ricerca AI" else "Testo"} · " + formatDate(entry.timestamp, "d MMM HH:mm"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
    if (history.size > RECENT_COLLAPSED) {
      TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(if (expanded) "Mostra meno" else "Mostra tutte (${history.size})")
      }
    }
  }
}

/** Card of the examples carousel: kind of query above, the query in serif. */
@Composable
private fun ExampleCard(kind: String, text: String, wide: Boolean = false, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.width(if (wide) 220.dp else 150.dp),
  ) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
      Text(
        kind.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text,
        style = ScriptureTypography.body,
        maxLines = if (wide) 3 else 2,
        minLines = if (wide) 3 else 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp),
      )
    }
  }
}

/** Warm gradient of the first-aid cards (terracotta to gold). */
@Composable
private fun firstAidBrush(): Brush =
  Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer),
  )

/** Sezione "pronto soccorso": tematiche difficili con versetti e riflessione via AI. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProntoSoccorsoSection(enabled: Boolean, onTheme: (String) -> Unit, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.extraLarge)
      .background(firstAidBrush())
      .padding(vertical = 20.dp),
  ) {
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
      TonalIcon(AppIcons.Favorite, size = 44.dp, container = colors.secondary, content = colors.onSecondary)
      Column(Modifier.padding(start = 14.dp)) {
        Text(
          "Pronto soccorso",
          style = MaterialTheme.typography.headlineSmall,
          color = colors.onSecondaryContainer,
        )
        Text(
          "Versetti e una riflessione per i momenti difficili",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSecondaryContainer.copy(alpha = 0.8f),
        )
      }
    }
    // Two rows that scroll sideways together, instead of a tall wall of chips.
    FlowRow(
      Modifier
        .padding(top = 18.dp)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .alpha(if (enabled) 1f else 0.5f),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      maxItemsInEachRow = (FIRST_AID_THEMES.size + 1) / 2,
    ) {
      FIRST_AID_THEMES.forEach { theme ->
        Surface(
          onClick = { onTheme(theme) },
          enabled = enabled,
          shape = CircleShape,
          color = colors.surface.copy(alpha = 0.75f),
          contentColor = colors.onSurface,
        ) {
          Text(
            theme,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          )
        }
      }
    }
    Row(
      Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        AiIcon,
        contentDescription = null,
        tint = colors.onSecondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.size(14.dp),
      )
      Text(
        "Con l'assistente AI",
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSecondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 6.dp),
      )
    }
  }
}

/** Card con la riflessione di conforto generata dall'AI per il tema scelto. */
@Composable
private fun FirstAidCard(theme: String, reflection: String, onSaveToNote: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Column(
    Modifier
      .fillMaxWidth()
      .padding(horizontal = ScreenPadding, vertical = 8.dp)
      .clip(MaterialTheme.shapes.extraLarge)
      .background(firstAidBrush())
      .padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      TonalIcon(AppIcons.Favorite, size = 32.dp, container = colors.secondary, content = colors.onSecondary)
      Text(
        "Pronto soccorso · $theme",
        style = MaterialTheme.typography.labelLarge,
        color = colors.onSecondaryContainer,
        modifier = Modifier.padding(start = 10.dp),
      )
    }
    Text(
      reflection,
      style = ScriptureTypography.body,
      color = colors.onSurface,
      modifier = Modifier.padding(top = 12.dp, end = 8.dp),
    )
    Row(Modifier.padding(top = 4.dp)) {
      TextButton(onClick = onSaveToNote) {
        Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Crea nota dalla riflessione", modifier = Modifier.padding(start = 6.dp))
      }
      TextButton(onClick = { openUrl(aiReportUrl(reflection, "pronto soccorso: $theme")) }) {
        Text("Segnala")
      }
    }
  }
}

/** Shortcut to the passage when the query is a reference ("Gv 3:16"). */
@Composable
private fun GoToPassageCard(label: String, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = colors.primaryContainer,
    contentColor = colors.onPrimaryContainer,
    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 4.dp),
  ) {
    Row(
      Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      TonalIcon(SettingsIcons.MenuBook, container = colors.primary, content = colors.onPrimary)
      Column(Modifier.weight(1f)) {
        Text("Vai al passo", style = MaterialTheme.typography.labelMedium)
        Text(label, style = MaterialTheme.typography.titleLarge.copy(fontFamily = ScriptureTypography.body.fontFamily))
      }
      Icon(AppIcons.ArrowForward, contentDescription = null)
    }
  }
}

@Composable
private fun ResultsCount(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
  )
}

@Composable
private fun SearchProgress() {
  LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 12.dp))
}

@Composable
private fun ResultDivider() {
  HorizontalDivider(
    Modifier.padding(horizontal = 20.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
  )
}

/** Verse result: reference in the accent color, serif text with the query words highlighted. */
@Composable
private fun VerseResultRow(reference: String, text: String, query: String?, onClick: () -> Unit) {
  val highlight =
    SpanStyle(
      background = MaterialTheme.colorScheme.primaryContainer,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      fontWeight = FontWeight.SemiBold,
    )
  val body = remember(text, query, highlight) { highlightMatches(text, query, highlight) }
  Column(
    Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 12.dp),
  ) {
    Text(
      reference,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      body,
      style = ScriptureTypography.body.copy(fontSize = 16.sp, lineHeight = 24.sp),
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 4.dp),
    )
  }
}

/** Header and rows of a group of AI matches (perfect or similar). */
private fun LazyListScope.aiSection(
  key: String,
  title: String,
  matches: List<AiVerseMatch>,
  bookNames: Map<Int, String>,
  onGoToVerse: (Int, Int, Int) -> Unit,
) {
  if (matches.isEmpty()) return
  item(key = "$key-header") {
    GroupHeader("$title · ${matches.size}", Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
  }
  itemsIndexed(matches, key = { _, it -> "$key-${it.ref.book}-${it.ref.chapter}-${it.ref.verse}" }) { index, match ->
    if (index > 0) ResultDivider()
    val r = match.ref
    VerseResultRow(
      reference = aiRangeLabel(match, bookNames),
      text = match.text.orEmpty(),
      query = null,
      onClick = { onGoToVerse(r.book, r.chapter, r.verse ?: 1) },
    )
  }
}

/** Note found by the notes search, as a tonal card. */
@Composable
private fun NoteResultCard(note: NoteEntity, query: String, onClick: () -> Unit) {
  val highlight =
    SpanStyle(
      background = MaterialTheme.colorScheme.primaryContainer,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        note.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        remember(note.content, query, highlight) { highlightMatches(note.content, query, highlight) },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 4.dp),
      )
      Text(
        formatDate(note.updatedAt, "d MMM HH:mm"),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
      )
    }
  }
}

/** Characters of context kept before the first match when the text is trimmed. */
private const val SNIPPET_CONTEXT = 40

/**
 * Styles every case-insensitive occurrence of the [query] words in [text]. When the
 * first match is far into the text, the beginning is cut (with "…") so the match
 * stays visible in a few-line preview.
 */
internal fun highlightMatches(text: String, query: String?, style: SpanStyle): AnnotatedString {
  val words = query.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { it.lowercase() }
  // lowercase() can change the length of a few characters: skip highlighting then.
  if (words.isEmpty() || text.lowercase().length != text.length) return AnnotatedString(text)
  val first = words.mapNotNull { w -> text.lowercase().indexOf(w).takeIf { it >= 0 } }.minOrNull()
  val shown =
    if (first != null && first > SNIPPET_CONTEXT * 2) {
      val start = text.lastIndexOf(' ', first - SNIPPET_CONTEXT).let { if (it < 0) first - SNIPPET_CONTEXT else it + 1 }
      "…" + text.substring(start)
    } else {
      text
    }
  val lower = shown.lowercase()
  return buildAnnotatedString {
    append(shown)
    words.forEach { w ->
      var i = lower.indexOf(w)
      while (i >= 0) {
        addStyle(style, i, i + w.length)
        i = lower.indexOf(w, i + w.length)
      }
    }
  }
}
