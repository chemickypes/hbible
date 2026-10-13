package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
import com.hooloovoochimico.kmp.hbible.ui.explore.AiVerseMatch
import com.hooloovoochimico.kmp.hbible.ui.explore.aiRangeLabel
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyState
import com.hooloovoochimico.kmp.hbible.ui.common.TonalIcon
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.appLog
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiPrompts
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AiChatPanel
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val BOOK_CHAT_SUGGESTIONS =
  listOf(
    "Chi ha scritto il libro e quando?",
    "Qual è il messaggio principale?",
    "Come ci mostra Gesù?",
    "Quali passaggi sono chiave?",
  )

/**
 * Full-screen introduction of a Bible book written in the CMS (writer, period,
 * context, summary, key verse, characters, Christocentric vision, outline) plus
 * an optional chat about the book with the user's AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoScreen(
  book: BookEntity,
  onGoToVerse: (Int, Int, Int) -> Unit,
  onSaveToNote: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: BookInfoViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val state by viewModel.state.collectAsStateWithLifecycle()
  val aiConfigured by viewModel.aiConfigured.collectAsStateWithLifecycle()
  val testament = if (book.n <= 39) "Antico Testamento" else "Nuovo Testamento"

  // --- chat state (transient) ---
  var showChat by remember { mutableStateOf(false) }
  val messages = remember { mutableStateListOf<AiChatMessage>() }
  var chatBusy by remember { mutableStateOf(false) }
  var chatError by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  fun send(text: String) {
    if (chatBusy) return
    messages.add(AiChatMessage("user", text))
    chatBusy = true
    chatError = null
    scope.launch {
      try {
        val system =
          AiPrompts.bookChatSystem(
            book.name,
            testament,
            book.chapters,
            state.info?.let { listOf(it.theme, it.summary).filter(String::isNotBlank).joinToString("\n") },
          )
        val reply = viewModel.chat(system, messages.toList())
        messages.add(AiChatMessage("assistant", reply))
      } catch (t: CancellationException) {
        throw t
      } catch (t: Throwable) {
        appLog.w(t) { "Chat AI sul libro fallita" }
        chatError = t.message ?: "Errore durante la chat"
      }
      chatBusy = false
    }
  }

  Column(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Row(
      Modifier.fillMaxWidth().padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onDismiss) {
        Icon(AppIcons.Close, contentDescription = "Chiudi introduzione")
      }
    }
    // Frontespizio: testamento, nome del libro, numero di capitoli.
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp)) {
      Text(
        "INTRODUZIONE · ${testament.uppercase()}",
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.primary,
      )
      Text(book.name, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 4.dp))
      Text(
        if (book.chapters == 1) "1 capitolo" else "${book.chapters} capitoli",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    val info = state.info
    when {
      info != null -> {
        Column(
          Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        ) {
          if (info.theme.isNotBlank()) {
            Text(
              info.theme,
              style = ScriptureTypography.intro.copy(fontStyle = FontStyle.Italic),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
          }
          InfoSection("Scrittore", info.author)
          InfoSection("Periodo di scrittura", info.period)
          InfoSection("Contesto", info.contextText)
          InfoSection("Riassunto", info.summary)
          state.keyVerse?.let { key -> KeyVerseSection(key, info.keyVerseNote, book.name, onGoToVerse) }
          InfoSection("Personaggi importanti", info.characters)
          InfoSection("Visione cristocentrica", info.christocentric)
          InfoSection("Struttura", info.outline)
        }
      }
      state.loading -> {
        Column(
          Modifier.weight(1f).fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator()
          Text(
            "Scarico l'introduzione…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
          )
        }
      }
      else -> {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          EmptyState(
            SettingsIcons.MenuBook,
            "Introduzione non disponibile",
            hint = "Le introduzioni si scaricano con l'aggiornamento dei contenuti: collegati a internet e riprova.",
          )
          FilledTonalButton(onClick = { viewModel.retry() }) { Text("Riprova") }
        }
      }
    }

    HBibleCard(
      onClick = if (aiConfigured) ({ showChat = true }) else null,
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TonalIcon(SettingsIcons.AutoAwesome)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
          Text(
            if (aiConfigured) "Chatta su ${book.name}" else "Assistente AI non configurato",
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            if (aiConfigured) "Domande e approfondimenti sul libro"
            else "Configura un servizio in Impostazioni → Assistente AI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (aiConfigured) {
          Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
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
          "Chatta su ${book.name}",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        AiChatPanel(
          messages = messages,
          busy = chatBusy,
          error = chatError,
          onSend = ::send,
          onClear = {
            messages.clear()
            chatError = null
          },
          suggestions = BOOK_CHAT_SUGGESTIONS,
          onSaveToNote = { onSaveToNote(it.content) },
          placeholder = "Chiedi qualcosa su ${book.name}…",
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
    }
  }
}

/** Key verse: reference and text in the reader's translation (tap opens it), plus why it matters. */
@Composable
private fun KeyVerseSection(
  match: AiVerseMatch,
  note: String,
  bookName: String,
  onGoToVerse: (Int, Int, Int) -> Unit,
) {
  val r = match.ref
  HBibleCard(
    onClick = { onGoToVerse(r.book, r.chapter, r.verse ?: 1) },
    color = MaterialTheme.colorScheme.primaryContainer,
    contentPadding = PaddingValues(20.dp),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Text(
      "Versetto chiave · " + aiRangeLabel(match, mapOf(r.book to bookName)),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    match.text?.let {
      Text(
        it,
        style = ScriptureTypography.intro.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(top = 8.dp),
      )
    }
    if (note.isNotBlank()) {
      Text(
        note,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
        modifier = Modifier.padding(top = 8.dp),
      )
    }
  }
}

@Composable
private fun InfoSection(title: String, body: String) {
  if (body.isBlank()) return
  HBibleCard(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding = PaddingValues(20.dp),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(body, style = ScriptureTypography.intro, modifier = Modifier.padding(top = 8.dp))
  }
}
