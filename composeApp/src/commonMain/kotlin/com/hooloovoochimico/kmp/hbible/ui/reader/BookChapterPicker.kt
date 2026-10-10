package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hooloovoochimico.kmp.hbible.data.RecentBook
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyState
import com.hooloovoochimico.kmp.hbible.ui.common.displayAbbr

/** Sezione del canone mostrata come intestazione sopra le tessere dei libri. */
private data class BookSection(val title: String, val books: IntRange)

private val SECTIONS =
  listOf(
    BookSection("Pentateuco", 1..5),
    BookSection("Libri storici", 6..17),
    BookSection("Poetici e sapienziali", 18..22),
    BookSection("Profeti maggiori", 23..27),
    BookSection("Profeti minori", 28..39),
    BookSection("Vangeli e Atti", 40..44),
    BookSection("Lettere di Paolo", 45..57),
    BookSection("Lettere universali", 58..65),
    BookSection("Profezia", 66..66),
  )

private const val LAST_OT_BOOK = 39

/** Voce della griglia dei libri: intestazione di sezione (a tutta riga) o tessera. */
private sealed interface PickerEntry {
  data class Header(val title: String) : PickerEntry

  data class Tile(val book: BookEntity) : PickerEntry
}

/** Minuscolo e senza accenti, per cercare "giosue" o "GV". */
internal fun normalizeBookQuery(text: String): String =
  text.lowercase()
    .map {
      when (it) {
        'à', 'á' -> 'a'
        'è', 'é' -> 'e'
        'ì', 'í' -> 'i'
        'ò', 'ó' -> 'o'
        'ù', 'ú' -> 'u'
        else -> it
      }
    }
    .joinToString("")
    .filterNot { it.isWhitespace() }

/** Il libro corrisponde alla ricerca per nome o abbreviazione ("1cor", "corinzi", "gb"). */
internal fun bookMatches(name: String, abbr: String, query: String): Boolean {
  val q = normalizeBookQuery(query)
  return q.isEmpty() || normalizeBookQuery(name).contains(q) || normalizeBookQuery(abbr).contains(q)
}

/**
 * Foglio "Scegli un libro" (variante A del prototipo): ricerca, libri recenti, interruttore
 * Antico/Nuovo Testamento e tessere per sezione; toccando un libro si sceglie il capitolo
 * nello stesso foglio. Si apre già sul libro in lettura. [startOnChapters] apre direttamente
 * i capitoli del libro in lettura (pulsante griglia del Lettore).
 */
@Composable
fun BookChapterPicker(
  books: List<BookEntity>,
  currentBook: Int,
  currentChapter: Int,
  recents: List<RecentBook>,
  onPick: (book: Int, chapter: Int) -> Unit,
  modifier: Modifier = Modifier,
  startOnChapters: Boolean = false,
) {
  var pickedBook by rememberSaveable { mutableStateOf(if (startOnChapters) currentBook else 0) }
  val picked = books.firstOrNull { it.n == pickedBook }

  Column(modifier) {
    if (picked == null) {
      BookStep(books, currentBook, recents, onPick, onOpenBook = { book ->
        // Un libro di un solo capitolo (Abdia, Filemone…) si apre subito.
        if (book.chapters <= 1) onPick(book.n, 1) else pickedBook = book.n
      })
    } else {
      ChapterStep(
        book = picked,
        currentChapter = if (picked.n == currentBook) currentChapter else 0,
        onBack = { pickedBook = 0 },
        onPick = { chapter -> onPick(picked.n, chapter) },
      )
    }
  }
}

@Composable
private fun BookStep(
  books: List<BookEntity>,
  currentBook: Int,
  recents: List<RecentBook>,
  onPick: (book: Int, chapter: Int) -> Unit,
  onOpenBook: (BookEntity) -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  var oldTestament by rememberSaveable { mutableStateOf(currentBook <= LAST_OT_BOOK) }
  val searching = query.isNotBlank()

  Text(
    "Scegli un libro",
    style = MaterialTheme.typography.headlineSmall,
    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp),
  )
  TextField(
    value = query,
    onValueChange = { query = it },
    placeholder = { Text("Cerca un libro (es. Romani, 1Cor)") },
    singleLine = true,
    shape = CircleShape,
    leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    trailingIcon = {
      if (searching) {
        IconButton(onClick = { query = "" }) { Icon(AppIcons.Close, contentDescription = "Cancella ricerca") }
      }
    },
    colors =
      TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
      ),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
  )

  val bookByN = remember(books) { books.associateBy { it.n } }
  // Il libro in lettura è già aperto: i recenti utili sono gli altri.
  val otherRecents = recents.filter { it.book != currentBook && it.book in bookByN }
  if (!searching && otherRecents.isNotEmpty()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(top = 10.dp),
    ) {
      items(otherRecents, key = { it.book }) { recent ->
        Surface(
          onClick = { onPick(recent.book, recent.chapter) },
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
          Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(
              "${bookByN.getValue(recent.book).name} ${recent.chapter}",
              style = MaterialTheme.typography.labelLarge,
            )
            Text(
              " · riprendi",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
  if (!searching) {
    TestamentSwitch(
      oldTestament = oldTestament,
      onChange = { oldTestament = it },
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
    )
  }

  val entries =
    remember(books, query, oldTestament) {
      buildList {
        for (section in SECTIONS) {
          val inTestament = (section.books.first <= LAST_OT_BOOK) == oldTestament
          if (!searching && !inTestament) continue
          val tiles = books.filter { it.n in section.books && bookMatches(it.name, it.displayAbbr, query) }
          if (tiles.isEmpty()) continue
          add(PickerEntry.Header(section.title))
          tiles.forEach { add(PickerEntry.Tile(it)) }
        }
      }
    }
  if (entries.isEmpty()) {
    EmptyState(AppIcons.Search, "Nessun libro con questo nome")
    return
  }

  val gridState = rememberLazyGridState()
  // Si apre sulla sezione del libro in lettura (e ci torna cambiando testamento).
  LaunchedEffect(oldTestament, searching) {
    if (searching) {
      gridState.scrollToItem(0)
      return@LaunchedEffect
    }
    val tile = entries.indexOfFirst { it is PickerEntry.Tile && it.book.n == currentBook }
    val header = if (tile < 0) 0 else entries.subList(0, tile).indexOfLast { it is PickerEntry.Header }
    gridState.scrollToItem(header.coerceAtLeast(0))
  }
  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    state = gridState,
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    items(
      entries,
      key = { entry ->
        when (entry) {
          is PickerEntry.Header -> "h-${entry.title}"
          is PickerEntry.Tile -> "b-${entry.book.n}"
        }
      },
      span = { entry -> GridItemSpan(if (entry is PickerEntry.Header) maxLineSpan else 1) },
    ) { entry ->
      when (entry) {
        is PickerEntry.Header ->
          Text(
            entry.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
          )
        is PickerEntry.Tile ->
          BookTile(entry.book, current = entry.book.n == currentBook, onClick = { onOpenBook(entry.book) })
      }
    }
  }
}

/** Interruttore Antico/Nuovo Testamento a pillola. */
@Composable
private fun TestamentSwitch(oldTestament: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
  Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
    Row(Modifier.padding(3.dp)) {
      listOf(true to "Antico Testamento", false to "Nuovo Testamento").forEach { (old, label) ->
        val selected = old == oldTestament
        Surface(
          onClick = { onChange(old) },
          shape = CircleShape,
          color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
          contentColor =
            if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
          shadowElevation = if (selected) 1.dp else 0.dp,
          modifier = Modifier.weight(1f).semantics { this.selected = selected; role = Role.Tab },
        ) {
          Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}

/** Tessera del libro: abbreviazione grande in serif, nome e numero di capitoli. */
@Composable
private fun BookTile(book: BookEntity, current: Boolean, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = if (current) colors.primary else colors.surfaceContainer,
    contentColor = if (current) colors.onPrimary else colors.onSurface,
    modifier =
      Modifier.semantics {
        contentDescription = book.name + if (current) ", in lettura" else ""
      },
  ) {
    Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 9.dp)) {
      Text(book.displayAbbr, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp))
      val secondary = if (current) colors.onPrimary.copy(alpha = 0.85f) else colors.onSurfaceVariant
      Text(book.name, style = MaterialTheme.typography.labelMedium, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        if (book.chapters == 1) "1 cap." else "${book.chapters} cap.",
        style = MaterialTheme.typography.labelSmall,
        color = secondary,
      )
    }
  }
}

/** Secondo passo: griglia dei capitoli del libro scelto, con "‹" per tornare ai libri. */
@Composable
private fun ChapterStep(book: BookEntity, currentChapter: Int, onBack: () -> Unit, onPick: (Int) -> Unit) {
  Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(
      onClick = onBack,
      colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      Icon(AppIcons.ChevronLeft, contentDescription = "Torna ai libri")
    }
    Column(Modifier.padding(start = 10.dp)) {
      Text(book.name, style = MaterialTheme.typography.headlineSmall)
      Text(
        "${book.chapters} capitoli · scegli il capitolo",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
  val gridState = rememberLazyGridState()
  LaunchedEffect(book.n) { if (currentChapter > 0) gridState.scrollToItem((currentChapter - 1).coerceAtLeast(0)) }
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 56.dp),
    state = gridState,
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items((1..book.chapters).toList(), key = { it }) { chapter ->
      val selected = chapter == currentChapter
      Surface(
        onClick = { onPick(chapter) },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.aspectRatio(1f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(chapter.toString(), style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}
