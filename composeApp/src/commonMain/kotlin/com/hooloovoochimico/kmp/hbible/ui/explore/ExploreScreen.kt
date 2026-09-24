package com.hooloovoochimico.kmp.hbible.ui.explore

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.BibleReferenceParser
import com.hooloovoochimico.kmp.hbible.data.SearchScope
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyMessage
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import com.hooloovoochimico.kmp.hbible.ui.common.ScreenTitle

/** Shared duration of the AI-mode transitions (bar morph + filter chips fade). */
private const val SEARCH_TRANSITION_MS = 500

private val SEARCH_EXAMPLES =
  listOf("1 Gv 1:3", "Sal 23", "cantico dei cantici 2", "amore", "non temere")

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
  onSaveReflectionToNote: ((String, String) -> Unit)? = null,
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

  // Only pop the keyboard for a fresh search; a restored one shows its results.
  LaunchedEffect(Unit) { if (query.isEmpty()) focusRequester.requestFocus() }

  // Shared pieces of the search bar's trailing controls; in AI mode they are drawn
  // as overlays instead of icon slots so they never get vertically centered.
  val clearQueryButton: @Composable () -> Unit = {
    if (query.isNotEmpty()) {
      IconButton(
        onClick = {
          // Persist the cleared state so reopening shows the empty screen.
          viewModel.onQueryChange("")
        },
      ) {
        Icon(Icons.Default.Close, contentDescription = "Cancella query")
      }
    }
  }
  val fieldDivider: @Composable () -> Unit = {
    Box(
      Modifier
        .width(1.dp)
        .height(24.dp)
        .background(MaterialTheme.colorScheme.outlineVariant),
    )
  }
  val aiModeToggle: @Composable () -> Unit = {
    IconButton(onClick = { viewModel.onToggleAiMode() }) {
      Icon(
        AiIcon,
        contentDescription = if (aiMode) "Disattiva ricerca AI" else "Attiva ricerca AI",
        tint =
          if (aiMode) {
            MaterialTheme.colorScheme.primary
          } else {
            LocalContentColor.current
          },
      )
    }
  }

  // The bar morphs between the pill shape and the flatter AI prompt box; the pill
  // radius follows the measured field height so it stays circular at any font scale.
  val density = LocalDensity.current
  var fieldHeight by remember { mutableStateOf(56.dp) }
  val searchFieldRadius by animateDpAsState(
    targetValue = if (aiMode) 10.dp else fieldHeight / 2,
    animationSpec = tween(SEARCH_TRANSITION_MS),
    label = "searchFieldRadius",
  )

  Column(
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
  ) {
    ScreenTitle("Esplora", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    Box(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .onSizeChanged { fieldHeight = with(density) { it.height.toDp() } },
    ) {
      TextField(
        value = query,
        onValueChange = { viewModel.onQueryChange(it) },
        placeholder = {
          Text(
            when {
              aiMode -> "Descrivi il versetto che cerchi..."
              state.noteMode -> "Cerca nelle note…"
              else -> "Cerca testo o riferimento (1 Gv 1:3)"
            },
          )
        },
        singleLine = !aiMode,
        minLines = if (aiMode) 3 else 1,
        maxLines = if (aiMode) 6 else 1,
        // Tighter line spacing while composing the AI prompt.
        textStyle =
          if (aiMode) {
            MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp)
          } else {
            LocalTextStyle.current
          },
        shape = RoundedCornerShape(searchFieldRadius),
        // Hidden in AI mode to leave as much room as possible for the prompt.
        leadingIcon =
          if (aiMode) {
            null
          } else {
            { Icon(Icons.Default.Search, contentDescription = null) }
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
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
          ),
        keyboardOptions =
          KeyboardOptions(imeAction = if (aiMode) ImeAction.Send else ImeAction.Search),
        keyboardActions =
          KeyboardActions(
            onSearch = {
              if (aiMode) {
                viewModel.runAiSearch()
              } else {
                viewModel.recordQuery(query)
                keyboard?.hide()
              }
            },
          ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      )
      if (aiMode) {
        // Icon slots center vertically in multiline fields, so the sparkle is drawn
        // as an overlay pinned to the top end instead.
        Row(
          Modifier.align(Alignment.TopEnd),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          clearQueryButton()
          fieldDivider()
          aiModeToggle()
        }
        IconButton(
          onClick = { viewModel.runAiSearch() },
          enabled = query.isNotBlank() && !state.aiSearching,
          modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp),
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Avvia ricerca AI")
        }
      }
    }
    AnimatedVisibility(
      visible = !aiMode,
      enter =
        fadeIn(tween(SEARCH_TRANSITION_MS)) +
          expandVertically(expandFrom = Alignment.Top, animationSpec = tween(SEARCH_TRANSITION_MS)),
      exit =
        fadeOut(tween(SEARCH_TRANSITION_MS)) +
          shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(SEARCH_TRANSITION_MS)),
    ) {
      Row(
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SEARCH_SCOPES.forEach { s ->
          val selected = !state.noteMode && state.scope == s
          FilterChip(
            selected = selected,
            onClick = { viewModel.onScopeChange(s) },
            label = { Text(s.label) },
            leadingIcon =
              if (selected) {
                {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                  )
                }
              } else {
                null
              },
          )
        }
        FilterChip(
          selected = state.noteMode,
          onClick = { viewModel.onToggleNoteMode() },
          label = { Text("Note") },
        )
      }
    }

    if (query.trim().isEmpty()) {
      LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(bottom = Dimens.bottomBarClearance),
      ) {
        item(key = "hint") {
          Text(
            if (aiMode) {
              "Descrivi con parole tue il versetto che cerchi: l'AI individua i riferimenti " +
                "più probabili e li mostra come lista (cerca in Tutta la Bibbia, $translationName)."
            } else {
              "Cerca parole o frasi nel testo, oppure digita un riferimento per andare direttamente al versetto."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
          )
        }
        item(key = "pronto-soccorso") {
          ProntoSoccorsoSection(
            enabled = !state.aiSearching,
            onTheme = { viewModel.onFirstAid(it) },
          )
        }
        if (state.history.isNotEmpty()) {
          item(key = "recenti-header") {
            Row(
              Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "Recenti:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = { viewModel.clearHistory() }) {
                Text("Cancella")
              }
            }
          }
          items(state.history, key = { "h-${it.timestamp}-${it.query}" }) { entry ->
            Row(
              Modifier
                .fillMaxWidth()
                .clickable {
                  keyboard?.hide()
                  viewModel.restoreHistory(entry)
                }
                .padding(start = 24.dp, end = 24.dp)
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                if (entry.aiMode) AiIcon else Icons.Default.Search,
                contentDescription = if (entry.aiMode) "Ricerca AI" else "Ricerca testo",
                tint =
                  if (entry.aiMode) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                modifier = Modifier.size(18.dp),
              )
              Column(Modifier.padding(start = 12.dp)) {
                Text(
                  entry.query,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  "${if (entry.aiMode) "AI" else "Testo"} · " + formatDate(entry.timestamp, "d MMM HH:mm"),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        item(key = "esempi-header") {
          Text(
            "Esempi:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
          )
        }
        (if (aiMode) AI_EXAMPLES else SEARCH_EXAMPLES).forEach { example ->
          item(key = "ex-$example") {
            TextButton(
              onClick = {
                viewModel.onQueryChange(example)
                if (aiMode) {
                  viewModel.runAiSearch()
                } else {
                  viewModel.recordQuery(example)
                }
              },
              modifier = Modifier.padding(start = 12.dp),
            ) {
              Text(example, fontStyle = FontStyle.Italic)
            }
          }
        }
      }
    } else if (aiMode) {
      val savedAi = state.ai
      val aiActive = savedAi != null && savedAi.query == query.trim()
      val activeAiError = savedAi?.takeIf { it.query == query.trim() }?.error
      when {
        state.aiSearching -> {
          LinearProgressIndicator(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          )
          Text(
            "L'AI sta cercando i versetti...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
        activeAiError != null -> {
          Text(
            activeAiError,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
        !aiActive -> {
          Text(
            "Premi il tasto di invio per far cercare l'AI.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
        else -> {
          val r = aiResults
          if (r != null && r.perfect.isEmpty() && r.similar.isEmpty()) {
            EmptyMessage(
              "Nessun risultato",
              Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
          } else if (r != null) {
            val bookNames = remember(books) { books.associate { it.n to it.name } }
            LazyColumn(
              Modifier.weight(1f).navigationBarsPadding(),
              contentPadding = PaddingValues(bottom = Dimens.bottomBarClearance),
            ) {
              val reflection = savedAi?.matches?.riflessione.orEmpty()
              if (reflection.isNotBlank()) {
                item(key = "first-aid-card") {
                  val themeLabel =
                    (savedAi?.query.orEmpty()).replaceFirstChar { it.uppercase() }
                  FirstAidCard(
                    theme = themeLabel,
                    reflection = reflection,
                    onSaveToNote =
                      onSaveReflectionToNote?.let { save ->
                        { save(themeLabel, reflection) }
                      },
                  )
                }
              }
              if (r.perfect.isNotEmpty()) {
                item(key = "perfect-header") {
                  Text(
                    "Corrispondenze perfette",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                  )
                }
                items(
                  r.perfect,
                  key = { "p-${it.ref.book}-${it.ref.chapter}-${it.ref.verse}" },
                ) { match ->
                  AiResultRow(match, bookNames, onGoToVerse)
                }
              }
              if (r.similar.isNotEmpty()) {
                item(key = "similar-header") {
                  Text(
                    "Simili",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                  )
                }
                items(
                  r.similar,
                  key = { "s-${it.ref.book}-${it.ref.chapter}-${it.ref.verse}" },
                ) { match ->
                  AiResultRow(match, bookNames, onGoToVerse)
                }
              }
            }
          }
        }
      }
    } else {
      val bookNames = remember(books) { books.associate { it.n to it.name } }
      if (!state.noteMode) {
        val ref = remember(query, books) { BibleReferenceParser.parse(query, books) }
        ref?.let { r ->
          AssistChip(
            onClick = {
              keyboard?.hide()
              viewModel.recordQuery(query)
              onGoToVerse(r.book, r.chapter, r.verse ?: 1)
            },
            label = {
              Text("Vai a ${bookNames[r.book]} ${r.chapter}${r.verse?.let { ":$it" } ?: ""}")
            },
            leadingIcon = {
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
              )
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
          )
        }
      }
      if (state.searching) {
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
      } else if (state.noteMode) {
        Text(
          "${state.noteResults.size} note trovate",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (state.noteResults.isEmpty()) {
          EmptyMessage(
            "Nessuna nota corrisponde alla ricerca.",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        } else {
          LazyColumn(
            Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = Dimens.bottomBarClearance),
          ) {
            items(state.noteResults, key = { "note-${it.id}" }) { note ->
              Row(
                Modifier
                  .fillMaxWidth()
                  .clickable {
                    keyboard?.hide()
                    onOpenNote(note.id)
                  }
                  .padding(horizontal = 16.dp, vertical = 8.dp),
              ) {
                Column {
                  Text(
                    note.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    formatDate(note.updatedAt, "d MMM HH:mm"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      } else {
        Text(
          "${state.results.size}${if (state.results.size == SEARCH_LIMIT) "+" else ""} risultati · $translationName",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (state.results.isEmpty()) {
          EmptyMessage(
            "Nessun risultato",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        } else {
          LazyColumn(
            Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = Dimens.bottomBarClearance),
          ) {
            items(state.results, key = { "${it.translation}-${it.book}-${it.chapter}-${it.verse}" }) { verse ->
              Row(
                Modifier
                  .fillMaxWidth()
                  .clickable {
                    keyboard?.hide()
                    viewModel.recordQuery(query)
                    onGoToVerse(verse.book, verse.chapter, verse.verse)
                  }
                  .padding(horizontal = 16.dp, vertical = 8.dp),
              ) {
                Column {
                  Text(
                    "${bookNames[verse.book]} ${verse.chapter}:${verse.verse}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    verse.text.replace("\n", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
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

@Composable
private fun AiResultRow(
  match: AiVerseMatch,
  bookNames: Map<Int, String>,
  onGoToVerse: (Int, Int, Int) -> Unit,
) {
  val r = match.ref
  Row(
    Modifier
      .fillMaxWidth()
      .clickable {
        onGoToVerse(r.book, r.chapter, r.verse ?: 1)
      }
      .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Column {
      Text(
        aiRangeLabel(match, bookNames),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      match.text?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/** Sezione "pronto soccorso": tematiche difficili con versetti e riflessione via AI. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProntoSoccorsoSection(enabled: Boolean, onTheme: (String) -> Unit) {
  HBibleCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Default.Favorite,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Text(
        "Pronto soccorso",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
    Text(
      "Versetti e una riflessione per i momenti difficili, con l'AI.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FIRST_AID_THEMES.forEach { theme ->
        AssistChip(
          onClick = { if (enabled) onTheme(theme) },
          label = { Text(theme) },
        )
      }
    }
  }
}

/** Card con la riflessione di conforto generata dall'AI per il tema scelto. */
@Composable
private fun FirstAidCard(theme: String, reflection: String, onSaveToNote: (() -> Unit)? = null) {
  HBibleCard(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Default.Favorite,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text(
        "Pronto soccorso · $theme",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
    Text(
      reflection,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(top = 8.dp),
    )
    if (onSaveToNote != null) {
      TextButton(onClick = onSaveToNote) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
          "Crea nota dalla riflessione",
          modifier = Modifier.padding(start = 6.dp),
        )
      }
    }
  }
}
