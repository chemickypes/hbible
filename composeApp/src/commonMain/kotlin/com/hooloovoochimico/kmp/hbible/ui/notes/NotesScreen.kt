package com.hooloovoochimico.kmp.hbible.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyState
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import com.hooloovoochimico.kmp.hbible.ui.common.ScreenTitle
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsIcons

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

  Box(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(Modifier.fillMaxHeight().widthIn(max = Dimens.contentMaxWidth)) {
      Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          ScreenTitle("Note", Modifier.semantics { heading() })
          Text(
            when {
              notes.isEmpty() -> "I tuoi appunti di studio"
              query.isBlank() -> if (notes.size == 1) "1 nota" else "${notes.size} note"
              else -> "${filtered.size} su ${notes.size}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = onNewNote) {
          Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("Nuova", modifier = Modifier.padding(start = 6.dp))
        }
      }

      if (notes.isNotEmpty()) {
        TextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Cerca nelle note…") },
          singleLine = true,
          shape = RoundedCornerShape(28.dp),
          leadingIcon = {
            Icon(AppIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          },
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
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        )
      }

      when {
        notes.isEmpty() ->
          EmptyState(
            AppIcons.Notes,
            "Nessuna nota",
            hint = "Tocca \"Nuova\" per iniziare, o salva una risposta dalla chat AI.",
          )
        filtered.isEmpty() ->
          EmptyState(AppIcons.Search, "Nessun risultato", hint = "Nessuna nota corrisponde alla ricerca.")
        else ->
          LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding =
              PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = LocalBottomBarClearance.current),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(filtered, key = { it.id }) { note ->
              NoteCard(
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

/** Note as a tonal card: serif title, preview, linked passage and date. */
@Composable
private fun NoteCard(
  note: NoteEntity,
  bookLabel: String?,
  dateLabel: String,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    // Senza versetto collegato il titolo è l'inizio del testo (NoteEditorScreen.deriveTitle):
    // in quel caso si mostra solo il testo, senza ripeterlo.
    val derivedTitle = note.content.trim().startsWith(note.title.removeSuffix("…").trim())
    Column(Modifier.padding(start = 18.dp, top = 6.dp, end = 4.dp, bottom = 14.dp)) {
      Row(verticalAlignment = Alignment.Top) {
        Text(
          if (derivedTitle) note.content.trim() else note.title.ifBlank { "Senza titolo" },
          style =
            if (derivedTitle) ScriptureTypography.body
            else MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
          maxLines = if (derivedTitle) 4 else 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(top = 10.dp),
        )
        IconButton(onClick = onDelete) {
          Icon(
            AppIcons.Delete,
            contentDescription = "Elimina nota",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      if (!derivedTitle && note.content.isNotBlank()) {
        Text(
          note.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(end = 14.dp),
        )
      }
      Row(
        Modifier.padding(top = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (bookLabel != null) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ) {
            Row(
              Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(SettingsIcons.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
              Text(
                bookLabel,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp),
              )
            }
          }
        }
        Text(
          dateLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
