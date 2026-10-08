package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography

/** Reference chip that expands in place to show the verse text. */
@Composable
fun ExpandableVerseChip(
  ref: VerseRef,
  label: String,
  expanded: Boolean,
  expandedText: String?,
  loading: Boolean,
  color: Color,
  contentColor: Color,
  onToggle: () -> Unit,
  onClose: () -> Unit,
  onOpen: () -> Unit,
  onOpenDetail: (() -> Unit)? = null,
) {
  Surface(
    onClick = onToggle,
    shape = MaterialTheme.shapes.extraLarge,
    color = color,
    contentColor = contentColor,
    modifier = if (expanded) Modifier.fillMaxWidth() else Modifier,
  ) {
    if (!expanded) {
      Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      )
    } else {
      Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onClose) {
            Icon(
              AppIcons.Close,
              contentDescription = "Chiudi riferimento",
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (loading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp).padding(vertical = 2.dp),
            strokeWidth = 2.dp,
          )
        } else {
          expandedText?.let { text ->
            Text(
              text,
              style = ScriptureTypography.body,
              modifier = Modifier.padding(top = 4.dp),
            )
          }
        }
        Row {
          TextButton(onClick = onOpen) {
            Text("Vai al brano ›")
          }
          if (onOpenDetail != null) {
            TextButton(onClick = onOpenDetail) {
              Text("Dettaglio ›")
            }
          }
        }
      }
    }
  }
}

/** Label for a chip: "Giovanni 3:16" or "Salmi 23" when the verse is missing. */
fun verseChipLabel(ref: VerseRef, bookName: (Int) -> String): String =
  when {
    ref.verse <= 0 -> "${bookName(ref.book)} ${ref.chapter}"
    ref.verseEnd != null && ref.verseEnd > ref.verse ->
      "${bookName(ref.book)} ${ref.chapter}:${ref.verse}-${ref.verseEnd}"
    else -> "${bookName(ref.book)} ${ref.chapter}:${ref.verse}"
  }

/**
 * FlowRow of expandable verse chips with shared expand state: only one chip is
 * expanded at a time and the verse text is loaded once on demand.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerseChipRow(
  refs: List<VerseRef>,
  bookName: (Int) -> String,
  loadVerse: suspend (VerseRef) -> String?,
  onOpenReference: (VerseRef) -> Unit,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
  onOpenDetail: ((VerseRef) -> Unit)? = null,
) {
  if (refs.isEmpty()) return
  val refSaver =
    Saver<VerseRef?, List<Int>>(
      save = { it?.let { listOf(it.book, it.chapter, it.verse) } ?: emptyList() },
      restore = { if (it.isEmpty()) null else VerseRef(it[0], it[1], it[2]) },
    )
  var expandedRef by rememberSaveable(stateSaver = refSaver) { mutableStateOf<VerseRef?>(null) }
  var expandedText by remember { mutableStateOf<String?>(null) }
  var expandedLoading by remember { mutableStateOf(false) }
  LaunchedEffect(expandedRef) {
    val target = expandedRef
    if (target == null) {
      expandedText = null
      return@LaunchedEffect
    }
    expandedLoading = true
    // Chapter-level chips (verse 0) preview the first verse of the chapter.
    val previewRef = if (target.verse > 0) target else target.copy(verse = 1)
    expandedText = loadVerse(previewRef)
    expandedLoading = false
  }
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    refs.forEach { ref ->
      ExpandableVerseChip(
        ref = ref,
        label = verseChipLabel(ref, bookName),
        expanded = expandedRef == ref,
        expandedText = expandedText,
        loading = expandedLoading,
        color = color,
        contentColor = contentColor,
        onToggle = { expandedRef = if (expandedRef == ref) null else ref },
        onClose = { expandedRef = null },
        onOpen = { onOpenReference(ref) },
        onOpenDetail = onOpenDetail?.let { open -> { open(ref) } },
      )
    }
  }
}
