package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
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
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiPrompts
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import com.hooloovoochimico.kmp.hbible.ui.common.AiChatPanel
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
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
 * Full-screen introduction of a Bible book: AI-generated context, protagonists
 * and Christocentric vision (cached locally) plus a chat about the book.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoScreen(
  book: BookEntity,
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
            state.info?.contextText,
          )
        val reply = viewModel.chat(system, messages.toList())
        messages.add(AiChatMessage("assistant", reply))
      } catch (t: CancellationException) {
        throw t
      } catch (t: Throwable) {
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
      Column(Modifier.padding(start = 4.dp)) {
        Text(book.name, style = MaterialTheme.typography.titleMedium)
        Text(
          "Introduzione · $testament",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    val info = state.info
    when {
      state.busy && info == null -> {
        Column(
          Modifier.weight(1f).fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator()
          Text(
            "L'AI sta scrivendo l'introduzione…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
          )
        }
      }
      info != null -> {
        Column(
          Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        ) {
          InfoSection("Contesto", info.contextText)
          InfoSection("Protagonisti", info.protagonists)
          InfoSection("Visione cristocentrica", info.christocentric)
          Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Generata con l'AI · " + formatDate(info.updatedAt, "d MMMM yyyy HH:mm"),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.generate() }, enabled = !state.busy) {
              Icon(
                AppIcons.Refresh,
                contentDescription = "Rigenera introduzione",
                modifier = Modifier.size(20.dp),
              )
            }
          }
          if (state.busy) {
            Text(
              "Rigenerazione in corso…",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 24.dp),
            )
          }
        }
      }
      else -> {
        Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
          Text(
            state.error ?: "Nessuna introduzione disponibile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
          TextButton(onClick = { viewModel.generate() }) { Text("Riprova") }
        }
      }
    }

    HorizontalDivider()
    if (aiConfigured) {
      HBibleCard(
        onClick = { showChat = true },
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
      ) {
        Text(
          "Chatta su ${book.name}",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          "Domande e approfondimenti sul libro",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      Text(
        "Per chattare configura un servizio AI in Impostazioni → Ricerca AI.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
      )
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

@Composable
private fun InfoSection(title: String, body: String) {
  if (body.isBlank()) return
  Text(
    title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
  )
  Text(
    body,
    style = ScriptureTypography.intro,
    modifier = Modifier.padding(horizontal = 24.dp),
  )
}
