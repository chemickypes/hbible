package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.appLog
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_META
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiPrompts
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AiChatPanel
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyMessage
import com.hooloovoochimico.kmp.hbible.ui.common.GroupHeader
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import com.hooloovoochimico.kmp.hbible.ui.common.TonalIcon
import com.hooloovoochimico.kmp.hbible.ui.common.TranslationVersionRow
import com.hooloovoochimico.kmp.hbible.ui.common.VerseChipRow
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import com.hooloovoochimico.kmp.hbible.ui.common.groupedShape
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val VERSE_CHAT_SUGGESTIONS =
  listOf(
    "Cosa significa questo versetto?",
    "Qual è il contesto?",
    "Dove si ripete questo tema?",
    "Come si applica oggi?",
  )

/**
 * Full-screen verse detail page: the verse, the interlinear shortcut, the verse
 * in the other translations, the original text with its transliteration, cross
 * references and the AI chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
  /** Opens the interlinear tab on this verse (wired by the shell). */
  onOpenInterlinear: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: VerseDetailViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val scope = rememberCoroutineScope()
  val aiConfigured by viewModel.aiConfigured.collectAsStateWithLifecycle()
  val scrollState = rememberScrollState()
  LaunchedEffect(index) { scrollState.scrollTo(0) }
  val loadVerse: suspend (VerseRef) -> VerseEntity? = { ref ->
    viewModel.verse(translation, ref.book, ref.chapter, ref.verse)
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
        appLog.w(t) { "Chat AI sul versetto fallita" }
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
        Icon(AppIcons.Close, contentDescription = "Chiudi dettaglio")
      }
      Column(Modifier.padding(start = 4.dp)) {
        Text(reference, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
        Text(
          translationName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (count > 1) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onPrev, enabled = index > 0) {
            Icon(AppIcons.ChevronLeft, contentDescription = "Versetto precedente")
          }
          Text(
            "${index + 1} di $count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
          )
          IconButton(onClick = onNext, enabled = index < count - 1) {
            Icon(AppIcons.ChevronRight, contentDescription = "Versetto successivo")
          }
        }
      }
    }

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(scrollState)
        .padding(bottom = 24.dp),
    ) {
      verse?.let { v ->
        // Il versetto come citazione: filetto oro a sinistra.
        Row(
          Modifier
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp)
            .height(IntrinsicSize.Min),
        ) {
          Box(
            Modifier
              .width(3.dp)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.primary, CircleShape),
          )
          Text(v.text, style = ScriptureTypography.detail, modifier = Modifier.padding(start = 16.dp))
        }
      }

      // Azione principale: il versetto nella tab Interlineare (parola per parola).
      FilledTonalButton(
        onClick = onOpenInterlinear,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Icon(AppIcons.Menu, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
          "Interlineare",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(start = 10.dp),
        )
      }

      // --- Altre traduzioni: il versetto come appare nelle altre versioni dell'app ---
      GroupHeader("Altre traduzioni", Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp))
      val others = detail?.versions.orEmpty().filter { it.translation != translation }
      if (detail != null && others.isEmpty()) {
        EmptyMessage(
          "Nessun'altra traduzione per questo versetto.",
          Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
          MaterialTheme.typography.bodySmall,
        )
      }
      others.forEachIndexed { i, version ->
        TranslationVersionRow(
          shape = groupedShape(i, others.size),
          abbr = version.translation,
          name = TRANSLATION_META.firstOrNull { it.abbr == version.translation }?.name ?: version.translation,
          text = version.text,
          current = false,
        )
      }

      // --- Testo originale e traslitterazione ---
      GroupHeader("Testo originale", Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp))
      val original = detail?.original
      if (original == null) {
        EmptyMessage(
          if (detail == null) "Caricamento…" else "Testo originale non disponibile per questo versetto.",
          Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
          MaterialTheme.typography.bodySmall,
        )
      } else {
        HBibleCard(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          shape = MaterialTheme.shapes.extraLarge,
          color = MaterialTheme.colorScheme.surfaceContainer,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ) {
            Text(
              if (original.lang == "he") "Ebraico" else "Greco",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          val originalText: @Composable () -> Unit = {
            Text(
              original.text,
              style = ScriptureTypography.original(original.lang),
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
          }
          // Ebraico: il testo resta da destra a sinistra anche in un blocco misto.
          if (original.lang == "he") {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
              originalText()
            }
          } else {
            originalText()
          }
          Text(
            original.transliteration,
            style = ScriptureTypography.transliteration,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
          )
        }
        Text(
          "Parola per parola nell'interlineare.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
        )
      }

      GroupHeader("Riferimenti", Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp))
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

      GroupHeader("Chiedi all'AI", Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp))
      HBibleCard(
        onClick = if (aiConfigured) ({ showChat = true }) else null,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          TonalIcon(SettingsIcons.AutoAwesome)
          Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
              if (aiConfigured) "Chatta su $reference" else "Assistente AI non configurato",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              if (aiConfigured) "Spiegazione, contesto e domande sul versetto"
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

      Spacer(Modifier.height(32.dp))
    }
  }
}
