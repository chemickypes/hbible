package com.hooloovoochimico.kmp.hbible.ui.interlineare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_META
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.alignmentTokens
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyMessage
import com.hooloovoochimico.kmp.hbible.ui.common.GroupHeader
import com.hooloovoochimico.kmp.hbible.ui.common.OriginalWordCard
import com.hooloovoochimico.kmp.hbible.ui.common.ScreenTitle
import com.hooloovoochimico.kmp.hbible.ui.common.TranslationVersionRow
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import com.hooloovoochimico.kmp.hbible.ui.common.groupedShape
import com.hooloovoochimico.kmp.hbible.ui.reader.BookChapterPicker
import com.hooloovoochimico.kmp.hbible.ui.reader.VerseStep

/**
 * Tab INTERLINEARE: un versetto alla volta in stile interlinearbible.org —
 * colonne di parole originali con numero Strong, traslitterazione e glossa
 * italiana. Il tap su una parola apre la sua scheda (lessico, occorrenze) in un
 * foglio; dalla scheda si può aprire un versetto citato o il dettaglio.
 */
/** Punteggiatura che il token italiano allineato porta attaccato e che il gloss di fallback ignora. */
private const val EDGE_PUNCT = ".,;:!?·«»\"'’‘()[]{}…—–"

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InterlineareScreen(
  translationName: String,
  onOpenReference: (VerseRef) -> Unit,
  onOpenDetail: (VerseRef) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: InterlineareViewModel,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val selected by viewModel.selectedWord.collectAsStateWithLifecycle()
  val aiFeaturesOn by viewModel.aiFeaturesEnabled.collectAsStateWithLifecycle()
  var showPicker by rememberSaveable { mutableStateOf(false) }
  val position = state.position
  val bookName = state.books.firstOrNull { it.n == position.book }?.name.orEmpty()

  Column(
    modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .padding(top = 12.dp),
  ) {
    // Titolo grande, come le altre pagine a schermo intero ("Esplora", "Impostazioni").
    ScreenTitle("Interlineare", Modifier.padding(horizontal = 20.dp))
    Text(
      "Il testo originale parola per parola",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp),
    )

    // Barra del riferimento: versetto precedente/successivo, al centro il picker.
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = viewModel::prev, enabled = state.hasPrev) {
          Icon(AppIcons.ChevronLeft, contentDescription = "Versetto precedente")
        }
        Column(
          Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable { showPicker = true }
            .padding(vertical = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "$bookName ${position.chapter}:${position.verse}",
              style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
            )
            Icon(
              AppIcons.KeyboardArrowDown,
              contentDescription = "Scegli il versetto",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(start = 2.dp).size(20.dp),
            )
          }
          Text(
            translationName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = viewModel::next, enabled = state.hasNext) {
          Icon(AppIcons.ChevronRight, contentDescription = "Versetto successivo")
        }
      }
    }
    Spacer(Modifier.height(8.dp))

    val original = state.original
    when {
      original == null && state.verse == null -> {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      original == null -> {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          EmptyMessage(
            "Testo originale non disponibile per questo versetto.",
            Modifier.padding(horizontal = 24.dp),
          )
        }
      }
      else -> {
        Column(
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        ) {
          WordColumns(
            original = original,
            verse = state.verse,
            alignment = state.alignment,
            lexiconGlosses = state.lexiconGlosses,
            onWordClick = viewModel::selectWord,
          )
          GroupHeader(
            "Il versetto nelle versioni",
            Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
          )
          state.versions.forEachIndexed { index, version ->
            TranslationVersionRow(
              shape = groupedShape(index, state.versions.size),
              abbr = version.translation,
              name =
                TRANSLATION_META.firstOrNull { it.abbr == version.translation }?.name
                  ?: version.translation,
              text = version.text,
              current = version.translation == state.translation,
            )
          }
          Spacer(Modifier.padding(bottom = 96.dp))
        }
      }
    }
  }

  // Scheda della parola toccata: foglio a tutta larghezza (telefono e tablet).
  selected?.let { word ->
    ModalBottomSheet(
      onDismissRequest = viewModel::clearWord,
      sheetState =
        rememberBottomSheetState(
          initialValue = SheetValue.Hidden,
          enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
      Column(
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
      ) {
        OriginalWordCard(
          info = word.info,
          lang = word.lang,
          bookName = { n -> state.books.firstOrNull { it.n == n }?.name.orEmpty() },
          loadVerse = { ref -> viewModel.verseText(ref) },
          onOpenReference = { ref ->
            viewModel.clearWord()
            onOpenReference(ref)
          },
          onOpenDetail = { ref ->
            viewModel.clearWord()
            onOpenDetail(ref)
          },
          onTranslateGloss =
            if (aiFeaturesOn) {
              { lexeme -> viewModel.translateGloss(word.lang, lexeme) }
            } else {
              null
            },
          onClose = viewModel::clearWord,
        )
      }
    }
  }

  if (showPicker) {
    ModalBottomSheet(
      onDismissRequest = { showPicker = false },
      sheetState =
        rememberBottomSheetState(
          initialValue = SheetValue.Hidden,
          // Tutto espanso: parzialmente espanso escluso (come skipPartiallyExpanded = true).
          enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
      // Stesso foglio del Lettore, con il passo in più per il versetto.
      BookChapterPicker(
        books = state.books,
        currentBook = position.book,
        currentChapter = position.chapter,
        recents = emptyList(),
        onPick = { book, chapter ->
          viewModel.moveTo(book, chapter, 1)
          showPicker = false
        },
        verseStep =
          VerseStep(
            currentVerse = position.verse,
            versesOf = viewModel::versesOf,
            onPick = { book, chapter, verse ->
              viewModel.moveTo(book, chapter, verse)
              showPicker = false
            },
          ),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
      )
    }
  }
}

/** FlowRow of compact word columns, right-to-left for Hebrew. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordColumns(
  original: OriginalVerseEntity,
  verse: VerseEntity?,
  alignment: List<Int>,
  lexiconGlosses: Map<String, String>,
  onWordClick: (Int) -> Unit,
) {
  val words = original.text.split(" ").filter { it.isNotEmpty() }
  val lemmas = original.lemmas.split(" ").filter { it.isNotEmpty() }
  val translitWords = original.transliteration.split(" ").filter { it.isNotEmpty() }
  val glossesIt = original.glossesIt.split("\t")
  val alignedArray = alignment
  val italianTokens = verse?.text?.let(::alignmentTokens) ?: emptyList()

  val content: @Composable () -> Unit = {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      words.forEachIndexed { index, word ->
        val strongKey = original.lang + lemmas.getOrNull(index)?.trim().orEmpty()
        val gloss =
          glossesIt.getOrNull(index)?.takeIf { it.isNotEmpty() }
            ?: alignedArray.getOrNull(index)
              ?.takeIf { it >= 0 }
              ?.let { italianTokens.getOrNull(it)?.trim { c -> c in EDGE_PUNCT }?.takeIf { s -> s.isNotEmpty() } }
            ?: lexiconGlosses[strongKey]
              ?.takeIf { lemmas.getOrNull(index)?.trim() != "-" && it.isNotEmpty() }
              .orEmpty()
        OriginalWordColumn(
          word = word,
          strong = strongLabel(original.lang, lemmas.getOrNull(index)),
          transliteration = translitWords.getOrNull(index).orEmpty(),
          gloss = gloss,
          lang = original.lang,
          onClick = { onWordClick(index) },
        )
      }
    }
  }
  if (original.lang == "he") {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      content()
    }
  } else {
    content()
  }
}

/** Strong's number of the word at [index], "h7225"/"g976" style; null if absent. */
private fun strongLabel(lang: String, lemma: String?): String? {
  val number = lemma?.trim().orEmpty()
  if (number.isEmpty() || number == "-") return null
  return if (lang == "he") "h$number" else "g$number"
}

@Composable
private fun OriginalWordColumn(
  word: String,
  strong: String?,
  transliteration: String,
  gloss: String,
  lang: String,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 4.dp),
  ) {
    if (strong != null) {
      Text(
        strong,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    if (transliteration.isNotEmpty()) {
      Text(
        transliteration,
        style = MaterialTheme.typography.labelMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    Text(
      word,
      style = ScriptureTypography.original(lang).copy(lineHeight = TextUnit.Unspecified),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(vertical = 2.dp),
    )
    if (gloss.isNotEmpty()) {
      Text(
        gloss,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
      )
    }
  }
}
