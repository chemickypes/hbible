package com.hooloovoochimico.kmp.hbible.ui.notes

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.data.BibleReferenceParser
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import com.hooloovoochimico.kmp.hbible.platform.formatDate
import com.hooloovoochimico.kmp.hbible.platform.shareText
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.VerseChipRow
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** TextFieldValue is not Bundle-saveable by default: store text and selection. */
private val TextFieldValueSaver =
  Saver<TextFieldValue, List<Any>>(
    save = { listOf(it.text, it.selection.min, it.selection.max) },
    restore = { saved ->
      val text = saved[0] as String
      val min = (saved[1] as Int).coerceIn(0, text.length)
      val max = (saved[2] as Int).coerceIn(0, text.length)
      TextFieldValue(text, TextRange(min, max))
    },
  )

/**
 * Editor of a personal note: rich-text content (bold, italic, underline,
 * strikethrough) and automatic recognition of Bible references shown as chips.
 * The title is derived from the first line of the content.
 */
@Composable
fun NoteEditorScreen(
  note: NoteEntity?,
  books: List<BookEntity>,
  loadVerse: suspend (VerseRef) -> String?,
  onOpenReference: (VerseRef) -> Unit,
  onOpenDetail: (VerseRef) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: NotesViewModel,
) {
  val scope = rememberCoroutineScope()
  var content by rememberSaveable(stateSaver = TextFieldValueSaver) {
    mutableStateOf(TextFieldValue(note?.content.orEmpty()))
  }
  val stylesSaver =
    Saver<List<StyleRange>, String>(
      save = { NoteStyles.encode(it) },
      restore = { NoteStyles.decode(it) },
    )
  var styles by rememberSaveable(stateSaver = stylesSaver) {
    mutableStateOf(NoteStyles.decode(note?.styles.orEmpty()))
  }
  var dirty by rememberSaveable { mutableStateOf(false) }
  var confirmDelete by remember { mutableStateOf(false) }

  val bookName: (Int) -> String = { n -> books.firstOrNull { it.n == n }?.name ?: "" }
  val refs =
    remember(content.text, books) {
      if (content.text.isBlank()) emptyList() else BibleReferenceParser.findReferences(content.text, books)
    }
  val linkedRef =
    note?.takeIf { it.book > 0 && it.chapter > 0 }?.let { VerseRef(it.book, it.chapter, it.verse) }

  fun deriveTitle(plain: String): String =
    when {
      linkedRef != null ->
        "Note su ${bookName(linkedRef.book)} ${linkedRef.chapter}" +
          linkedRef.verse.takeIf { v -> v > 0 }?.let { ":$it" }.orEmpty()
      plain.isNotBlank() -> plain.take(40).trim() + if (plain.length > 40) "…" else ""
      else -> "Nota"
    }

  fun buildEntity(): NoteEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    val base = note ?: NoteEntity(title = "", content = "", createdAt = now, updatedAt = now)
    val plain = content.text
    return base.copy(
      title = deriveTitle(plain),
      content = plain,
      styles = NoteStyles.encode(styles),
      updatedAt = now,
    )
  }

  fun shareNote() {
    val plain = content.text
    if (plain.isBlank()) return
    // Without a linked verse the title is derived from the content: avoid duplicating it.
    val text = if (linkedRef != null) "${deriveTitle(plain)}\n\n$plain" else plain
    shareText(text)
  }

  fun persistAndClose() {
    val entity = buildEntity()
    dirty = false
    scope.launch {
      viewModel.saveNote(entity)
      onDismiss()
    }
  }

  BackHandler(enabled = true) {
    if (dirty) persistAndClose() else onDismiss()
  }

  Column(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Row(
      Modifier.fillMaxWidth().padding(end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = { if (dirty) persistAndClose() else onDismiss() }) {
        Icon(AppIcons.Close, contentDescription = "Chiudi nota")
      }
      Column(Modifier.padding(start = 4.dp).weight(1f)) {
        Text(if (note == null) "Nuova nota" else "Nota", style = MaterialTheme.typography.titleMedium)
        Text(
          "Modifica" + note?.let { " · " + formatDate(it.updatedAt, "d MMMM yyyy, HH:mm") }.orEmpty(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      TextButton(onClick = { persistAndClose() }, enabled = dirty) { Text("Salva") }
      IconButton(onClick = { shareNote() }, enabled = content.text.isNotBlank()) {
        Icon(AppIcons.Share, contentDescription = "Condividi nota")
      }
      if (note != null) {
        IconButton(onClick = { confirmDelete = true }) {
          Icon(AppIcons.Delete, contentDescription = "Elimina nota")
        }
      }
    }

    linkedRef?.let { ref ->
      AssistChip(
        onClick = { onOpenReference(ref) },
        label = {
          Text(
            "Versetto: ${bookName(ref.book)} ${ref.chapter}" +
              ref.verse.takeIf { v -> v > 0 }?.let { ":$it" }.orEmpty(),
          )
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      )
    }

    Row(
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FormatButton("B", NoteStyleType.BOLD, styles, content, onStyle = { type ->
        val sel = content.selection
        if (!sel.collapsed) {
          styles = toggleStyle(styles, sel.min, sel.max, type)
          dirty = true
        }
      }, fontWeight = FontWeight.Bold)
      FormatButton("I", NoteStyleType.ITALIC, styles, content, onStyle = { type ->
        val sel = content.selection
        if (!sel.collapsed) {
          styles = toggleStyle(styles, sel.min, sel.max, type)
          dirty = true
        }
      }, fontStyle = FontStyle.Italic)
      FormatButton("U", NoteStyleType.UNDERLINE, styles, content, onStyle = { type ->
        val sel = content.selection
        if (!sel.collapsed) {
          styles = toggleStyle(styles, sel.min, sel.max, type)
          dirty = true
        }
      }, textDecoration = TextDecoration.Underline)
      FormatButton("S", NoteStyleType.STRIKETHROUGH, styles, content, onStyle = { type ->
        val sel = content.selection
        if (!sel.collapsed) {
          styles = toggleStyle(styles, sel.min, sel.max, type)
          dirty = true
        }
      }, textDecoration = TextDecoration.LineThrough)
      Spacer(Modifier.weight(1f))
    }
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
    ) {
      BasicTextField(
        value = content,
        onValueChange = { new ->
          styles = adjustRanges(content.text, new.text, styles)
          content = new
          dirty = true
        },
        textStyle = ScriptureTypography.body.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          ) {
            Box(Modifier.padding(12.dp).heightIn(min = 140.dp)) {
              if (content.text.isEmpty()) {
                Text(
                  "Scrivi la nota… (es. \"Gv 3:16 mi ricorda che…\")",
                  style = ScriptureTypography.body.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                )
              }
              inner()
            }
          }
        },
        visualTransformation =
          NoteStyleTransformation(
            styles,
            refRanges = refs.map { it.start until it.end },
            refColor = MaterialTheme.colorScheme.primaryContainer,
          ),
        modifier = Modifier.fillMaxWidth(),
      )
      if (refs.isNotEmpty()) {
        Text(
          "Riferimenti trovati",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        )
        VerseChipRow(
          refs =
            refs.map {
              VerseRef(it.ref.book, it.ref.chapter, it.ref.verse ?: 0, it.ref.verseEnd)
            },
          bookName = bookName,
          loadVerse = loadVerse,
          onOpenReference = onOpenReference,
          onOpenDetail = onOpenDetail,
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        )
      }
    }
  }

  if (confirmDelete && note != null) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("Eliminare la nota?") },
      text = { Text("Questa azione non si può annullare.") },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDelete = false
            viewModel.deleteNote(note.id)
            onDismiss()
          },
        ) {
          Text("Elimina", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("Annulla") }
      },
    )
  }
}

@Composable
private fun FormatButton(
  label: String,
  type: String,
  styles: List<StyleRange>,
  content: TextFieldValue,
  onStyle: (String) -> Unit,
  fontWeight: FontWeight? = null,
  fontStyle: FontStyle? = null,
  textDecoration: TextDecoration? = null,
) {
  val active = isStyled(styles, content.selection.min, content.selection.max, type)
  Surface(
    onClick = { onStyle(type) },
    shape = RoundedCornerShape(8.dp),
    color =
      if (active) MaterialTheme.colorScheme.primaryContainer
      else Color.Transparent,
    contentColor =
      if (active) MaterialTheme.colorScheme.onPrimaryContainer
      else MaterialTheme.colorScheme.onSurfaceVariant,
    enabled = !content.selection.collapsed,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
      ),
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}
