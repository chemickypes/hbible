package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiPrompts
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AiChatPanel
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyMessage
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import com.hooloovoochimico.kmp.hbible.ui.common.SectionHeader
import com.hooloovoochimico.kmp.hbible.ui.common.VerseChipRow
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef

private const val LEMMA_LIMIT = 40

private val VERSE_CHAT_SUGGESTIONS =
  listOf(
    "Cosa significa questo versetto?",
    "Qual è il contesto?",
    "Dove si ripete questo tema?",
    "Come si applica oggi?",
  )

/** Full-screen verse detail page: original text, transliteration, cross references. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VerseDetailScreen(
  verse: VerseEntity?,
  detail: VerseDetail?,
  reference: String,
  count: Int,
  index: Int,
  translation: String,
  translationName: String,
  bookName: (Int) -> String,
  onSaveAiToNote: ((String) -> Unit)?,
  onDismiss: () -> Unit,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onOpenReference: (VerseRef) -> Unit,
  onOpenDetail: (VerseRef) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ReaderViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  LaunchedEffect(index) { scrollState.scrollTo(0) }
  val loadVerse: suspend (VerseRef) -> VerseEntity? = { ref ->
    viewModel.verse(ref.book, ref.chapter, ref.verse)
  }
  val occurrencesTextLoader: suspend (VerseRef) -> String? = { loadVerse(it)?.text }

  // --- AI chat on the verse (transient, resets when the verse changes) ---
  var showChat by rememberSaveable { mutableStateOf(false) }
  val chatMessages = remember { mutableStateListOf<AiChatMessage>() }
  var chatBusy by remember { mutableStateOf(false) }
  var chatError by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(reference) {
    chatMessages.clear()
    chatError = null
    chatBusy = false
  }
  fun sendChat(text: String) {
    if (chatBusy) return
    val verseText = verse?.text.orEmpty()
    chatMessages.add(AiChatMessage("user", text))
    chatBusy = true
    chatError = null
    scope.launch {
      try {
        val system = AiPrompts.verseChatSystem(reference, verseText, translationName)
        val reply = viewModel.chat(system, chatMessages.toList())
        chatMessages.add(AiChatMessage("assistant", reply))
      } catch (t: CancellationException) {
        throw t
      } catch (t: Throwable) {
        chatError = t.message ?: "Errore durante la chat"
      }
      chatBusy = false
    }
  }
  if (showChat) {
    ModalBottomSheet(onDismissRequest = { showChat = false }) {
      Column(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.88f)
          .padding(bottom = 24.dp)
          .imePadding(),
      ) {
        Text(
          "Chatta su $reference",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        AiChatPanel(
          messages = chatMessages,
          busy = chatBusy,
          error = chatError,
          onSend = ::sendChat,
          onClear = {
            chatMessages.clear()
            chatError = null
          },
          suggestions = VERSE_CHAT_SUGGESTIONS,
          onSaveToNote = onSaveAiToNote?.let { save -> { message: AiChatMessage -> save(message.content) } },
          placeholder = "Chiedi qualcosa su questo versetto…",
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
    }
  }

  // --- selected original-language word (lemma concordance) ---
  val original = detail?.original
  val words = original?.text?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
  val lemmaTokens = original?.lemmas?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
  val translitWords = original?.transliteration?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
  val selectable = original != null && words.isNotEmpty() && words.size == lemmaTokens.size
  var selectedWord by rememberSaveable(original, index) { mutableStateOf(-1) }
  // aligned Italian token index for the selected word (per current translation)
  val alignedArray =
    remember(original, translation) {
      val csv = if (translation == "R2") original?.italianR2 else original?.italianNr
      csv?.split(",")?.map { it.toIntOrNull() ?: -1 } ?: emptyList()
    }
  val italianVerseTokens = verse?.text?.split(Regex("\\s+")) ?: emptyList()
  val alignedItToken =
    if (selectable && selectedWord in alignedArray.indices) alignedArray[selectedWord] else -1
  val alignedItalianWord =
    alignedItToken.takeIf { it in italianVerseTokens.indices }?.let { italianVerseTokens[it] } ?: ""
  var occurrences by remember(original) { mutableStateOf(emptyList<VerseRef>()) }
  var occurrencesLoading by remember(original) { mutableStateOf(false) }
  val selectedLemma = if (selectable && selectedWord in lemmaTokens.indices) lemmaTokens[selectedWord] else ""
  LaunchedEffect(original, selectedWord) {
    val o = original
    if (o == null || selectedLemma.isEmpty() || selectedLemma == "-") {
      occurrences = emptyList()
      occurrencesLoading = false
      return@LaunchedEffect
    }
    occurrencesLoading = true
    occurrences = viewModel.lemmaOccurrences(o.lang, selectedLemma, VerseRef(o.book, o.chapter, o.verse))
    occurrencesLoading = false
  }
  var lexeme by remember(original, selectedWord) { mutableStateOf<LexemeEntity?>(null) }
  var translating by remember(original, selectedWord) { mutableStateOf(false) }
  var translateFailed by remember(original, selectedWord) { mutableStateOf(false) }
  LaunchedEffect(original, selectedWord) {
    val o = original
    lexeme =
      if (o == null || selectedLemma.isEmpty() || selectedLemma == "-") {
        null
      } else {
        viewModel.lexeme(o.lang, selectedLemma)
      }
  }

  Column(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .imePadding()
      .navigationBarsPadding(),
  ) {
    Row(
      Modifier
        .fillMaxWidth()
        .padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onDismiss) {
        Icon(Icons.Default.Close, contentDescription = "Chiudi dettaglio")
      }
      Column(Modifier.padding(start = 4.dp)) {
        Text(reference, style = MaterialTheme.typography.titleMedium)
        Text(
          translationName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (count > 1) {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onPrev, enabled = index > 0) { Text("‹ Prec.") }
        Text(
          "${index + 1} di $count",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center,
        )
        TextButton(onClick = onNext, enabled = index < count - 1) { Text("Succ. ›") }
      }
    }

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(scrollState)
        .padding(bottom = 24.dp),
    ) {
      verse?.let { v ->
        val annotated = buildAnnotatedString {
          var start = 0
          var tokenIdx = 0
          Regex("\\S+").findAll(v.text).forEach { match ->
            append(v.text.substring(start, match.range.first))
            val highlight = tokenIdx == alignedItToken && alignedItToken >= 0
            withStyle(
              SpanStyle(
                background =
                  if (highlight) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
              ),
            ) { append(match.value) }
            start = match.range.last + 1
            tokenIdx++
          }
          append(v.text.substring(start))
        }
        Text(
          annotated,
          style = ScriptureTypography.detail,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
      }

        SectionHeader(
          "Testo originale",
          Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        )
        val original = detail?.original
        if (original == null) {
          EmptyMessage(
            if (detail == null) "Caricamento…" else "Testo originale non disponibile per questo versetto.",
            Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            MaterialTheme.typography.bodySmall,
          )
        } else {
          Text(
            if (original.lang == "he") "Ebraico" else "Greco",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp),
          )
          val wordStyle = ScriptureTypography.original(original.lang)
          if (selectable) {
            val wordRow = @Composable { word: String, idx: Int ->
              val selected = idx == selectedWord
              Text(
                word,
                style = wordStyle,
                color =
                  if (selected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    )
                    .clickable { selectedWord = if (selected) -1 else idx }
                    .padding(horizontal = 2.dp),
              )
            }
            val content: @Composable () -> Unit = {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
              ) {
                words.forEachIndexed { idx, word -> wordRow(word, idx) }
              }
            }
            if (original.lang == "he") {
              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
              }
            } else {
              content()
            }
          } else {
            Text(
              original.text,
              style = wordStyle,
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
          }

          if (selectedWord >= 0 && selectedWord < words.size) {
            HBibleCard(
              shape = MaterialTheme.shapes.extraLarge,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  words[selectedWord],
                  style = ScriptureTypography.original(original.lang).copy(lineHeight = TextUnit.Unspecified),
                  modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { selectedWord = -1 }) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Deseleziona parola",
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              if (selectedWord < translitWords.size) {
                Text(
                  translitWords[selectedWord],
                  style = ScriptureTypography.originalGloss,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
                if (alignedItalianWord.isNotEmpty()) {
                  Text(
                    "In italiano:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                  )
                  Text(
                    alignedItalianWord,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                lexeme?.let { lx ->
                  Column(Modifier.padding(top = 6.dp)) {
                    val italian = lx.glossIt
                    if (italian.isNotBlank()) {
                      Text(
                        italian,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                      )
                      Text(
                        lx.gloss,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                      )
                    } else {
                      Text(
                        lx.gloss,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      TextButton(onClick = {
                        scope.launch {
                          translating = true
                          translateFailed = false
                          val result = viewModel.translateGloss(original.lang, lx.number, lx.gloss)
                          if (result != null) {
                            lexeme = lx.copy(glossIt = result)
                          } else {
                            translateFailed = true
                          }
                          translating = false
                        }
                      }) {
                        Text(
                          if (translating) "Traduzione in corso…" else "Traduci in italiano (AI)",
                        )
                      }
                      if (translateFailed && !translating) {
                        Text(
                          "Traduzione non riuscita: configura un servizio AI nelle Impostazioni → Ricerca AI.",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.error,
                        )
                      }
                    }
                  }
                }
                val count = occurrences.size
                val countLabel =
                  when {
                    occurrencesLoading -> "Ricerca in corso…"
                    selectedLemma == "-" -> "Lemma non disponibile"
                    count == 0 -> "Nessuna altra occorrenza"
                    count == 1 -> "Ricorre in 1 altro versetto"
                    count == LEMMA_LIMIT -> "Ricorre in $count+ altri versetti"
                    else -> "Ricorre in $count altri versetti"
                  }
                Text(
                  countLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                 if (!occurrencesLoading && occurrences.isNotEmpty()) {
                   VerseChipRow(
                     refs = occurrences,
                     bookName = bookName,
                     loadVerse = occurrencesTextLoader,
                     onOpenReference = onOpenReference,
                     color = MaterialTheme.colorScheme.secondaryContainer,
                     contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                     onOpenDetail = onOpenDetail,
                   )
                 }
            }
          }

          Text(
            original.transliteration,
            style = ScriptureTypography.transliteration,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
          )
      }

      SectionHeader("Riferimenti", Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp))
      val refs = detail?.references.orEmpty()
      if (detail != null && refs.isEmpty()) {
        EmptyMessage(
          "Nessun riferimento trovato.",
          Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
          MaterialTheme.typography.bodySmall,
        )
      } else {
        VerseChipRow(
          refs = refs,
          bookName = bookName,
          loadVerse = occurrencesTextLoader,
          onOpenReference = onOpenReference,
          onOpenDetail = onOpenDetail,
          modifier = Modifier.padding(horizontal = 24.dp),
        )
      }

      SectionHeader("Chiedi all'AI", Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp))
      if (viewModel.aiConfigured) {
        HBibleCard(
          onClick = { showChat = true },
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Text(
            "Chatta su $reference",
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            "Spiegazione, contesto e domande sul versetto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        EmptyMessage(
          "Per chattare configura un servizio AI in Impostazioni → Ricerca AI.",
          Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
          MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(Modifier.height(32.dp))
    }
  }
}
