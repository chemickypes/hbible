package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.aiReportUrl
import com.hooloovoochimico.kmp.hbible.platform.openUrl

/**
 * Multi-turn AI chat: message bubbles, suggestions when empty, optional
 * "salva in nota" action on assistant replies.
 */
@Composable
fun AiChatPanel(
  messages: List<AiChatMessage>,
  busy: Boolean,
  error: String?,
  onSend: (String) -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
  suggestions: List<String> = emptyList(),
  onSaveToNote: ((AiChatMessage) -> Unit)? = null,
  placeholder: String = "Chiedi qualcosa…",
) {
  var draft by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  LaunchedEffect(messages.size, busy) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex + if (busy) 1 else 0)
    }
  }

  Column(modifier.fillMaxWidth().imePadding()) {
    if (messages.isEmpty() && !busy && error == null) {
      if (suggestions.isNotEmpty()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
          Text(
            "Puoi chiedere ad esempio:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
          )
          suggestions.take(4).forEach { suggestion ->
            AssistChip(
              onClick = { onSend(suggestion) },
              label = { Text(suggestion) },
              modifier = Modifier.padding(bottom = 6.dp),
            )
          }
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(messages) { message ->
          ChatBubble(
            message = message,
            onSaveToNote = if (message.role == "assistant") onSaveToNote else null,
          )
        }
        if (busy) {
          item(key = "typing") {
            Row(
              Modifier.fillMaxWidth().padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.padding(2.dp),
                strokeWidth = 2.dp,
              )
              Text(
                "L'AI sta rispondendo…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
    error?.let {
      Text(
        it,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
      )
    }
    if (messages.isNotEmpty()) {
      TextButton(onClick = onClear, modifier = Modifier.padding(start = 12.dp)) {
        Icon(
          AppIcons.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Nuova conversazione")
      }
    }
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        placeholder = { Text(placeholder) },
        modifier = Modifier.weight(1f),
        maxLines = 4,
        shape = MaterialTheme.shapes.extraLarge,
      )
      IconButton(
        onClick = {
          val text = draft.trim()
          if (text.isNotEmpty() && !busy) {
            onSend(text)
            draft = ""
          }
        },
        enabled = draft.isNotBlank() && !busy,
      ) {
        Icon(AppIcons.Send, contentDescription = "Invia messaggio")
      }
    }
  }
}

@Composable
private fun ChatBubble(
  message: AiChatMessage,
  onSaveToNote: ((AiChatMessage) -> Unit)?,
) {
  val isUser = message.role == "user"
  Column(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color =
        if (isUser) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceContainerHigh
        },
      contentColor =
        if (isUser) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSurface
        },
    ) {
      Text(
        message.content,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      )
    }
    if (!isUser) {
      Row {
        if (onSaveToNote != null) {
          TextButton(onClick = { onSaveToNote(message) }) {
            Text("Salva in nota", style = MaterialTheme.typography.labelMedium)
          }
        }
        TextButton(onClick = { openUrl(aiReportUrl(message.content, "chat")) }) {
          Text("Segnala", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}
