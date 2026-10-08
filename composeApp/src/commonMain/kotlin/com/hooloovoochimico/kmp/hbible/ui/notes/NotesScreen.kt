package com.hooloovoochimico.kmp.hbible.ui.notes

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyMessage
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons

/** Full-screen list of personal notes with live filter. */
@Composable
fun NotesScreen(
  books: List<BookEntity>,
  onDismiss: () -> Unit,
  onOpenNote: (Long) -> Unit,
  onNewNote: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: NotesViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val notes by viewModel.notes.collectAsStateWithLifecycle()
  var query by rememberSaveable { mutableStateOf("") }
  var pendingDelete by remember { mutableStateOf<Long?>(null) }

  val filtered =
    remember(notes, query) {
      val q = query.trim()
      if (q.isEmpty()) {
        notes
      } else {
        notes.filter {
          it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
        }
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
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text("Note", style = MaterialTheme.typography.titleLarge)
        Text(
          "${filtered.size} su ${notes.size}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      TextButton(onClick = onNewNote) {
        Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text("Nuova")
      }
    }

    TextField(
      value = query,
      onValueChange = { query = it },
      placeholder = { Text("Cerca nelle note…") },
      singleLine = true,
      shape = RoundedCornerShape(28.dp),
      leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { query = "" }) {
            Icon(AppIcons.Close, contentDescription = "Cancella filtro")
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )

    if (filtered.isEmpty()) {
      EmptyMessage(
        if (query.isBlank()) {
          "Nessuna nota: tocca \"Nuova\" per iniziare, o salva una risposta dalla chat AI."
        } else {
          "Nessuna nota corrisponde alla ricerca."
        },
        Modifier.padding(24.dp),
      )
    } else {
      LazyColumn(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        contentPadding = PaddingValues(bottom = LocalBottomBarClearance.current),
      ) {
        items(filtered, key = { it.id }) { note ->
          NoteRow(
            note = note,
            bookLabel = bookLabel(note, books),
            dateLabel = formatDate(note.updatedAt, "d MMM yyyy"),
            onClick = { onOpenNote(note.id) },
            onDelete = { pendingDelete = note.id },
          )
        }
      }
    }
  }

  pendingDelete?.let { id ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("Eliminare la nota?") },
      text = { Text("Questa azione non si può annullare.") },
      confirmButton = {
        TextButton(
          onClick = {
            pendingDelete = null
            viewModel.deleteNote(id)
          },
        ) {
          Text("Elimina", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) { Text("Annulla") }
      },
    )
  }
}

private fun bookLabel(note: NoteEntity, books: List<BookEntity>): String? {
  if (note.book <= 0) return null
  val name = books.firstOrNull { it.n == note.book }?.name ?: return null
  if (note.chapter <= 0) return name
  return "$name ${note.chapter}" + note.verse.takeIf { it > 0 }?.let { ":$it" }.orEmpty()
}

@Composable
private fun NoteRow(
  note: NoteEntity,
  bookLabel: String?,
  dateLabel: String,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        note.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        note.content,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        listOfNotNull(dateLabel, bookLabel).joinToString("  ·  "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    IconButton(onClick = onDelete) {
      Icon(
        AppIcons.Delete,
        contentDescription = "Elimina nota",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
