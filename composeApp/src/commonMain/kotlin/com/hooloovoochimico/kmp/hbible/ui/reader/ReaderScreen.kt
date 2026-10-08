package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.compose.foundation.layout.BoxWithConstraints
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import com.hooloovoochimico.kmp.hbible.ui.common.centeringPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.SHOW_CMS_HOME_CONTENT
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_NAMES
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.platform.copyToClipboard
import com.hooloovoochimico.kmp.hbible.platform.shareText
import com.hooloovoochimico.kmp.hbible.platform.toast
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.theme.ExpressiveMotion
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.BookSheetRow
import com.hooloovoochimico.kmp.hbible.ui.common.SectionHeader
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import com.hooloovoochimico.kmp.hbible.ui.common.displayAbbr
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsViewModel
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import kotlinx.coroutines.flow.first

/**
 * Contenuto della tab BIBBIA: lista versetti, top bar con titolo/selezione,
 * sheet libri/capitoli. Le altre tab e gli overlay (dettaglio, info libro,
 * editor note, impostazioni AI) sono destinazioni di navigazione in App.kt.
 */
/** Versetto da raggiungere con scroll alla riapertura del capitolo. */
data class PendingScroll(val book: Int, val chapter: Int, val verse: Int)

/** Material "content_copy": two overlapping rectangles. */
private val CopyIcon: ImageVector =
  ImageVector.Builder(
    name = "CopyVerses",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  )
    .path(fill = SolidColor(Color.Black)) {
      moveTo(16f, 1f)
      horizontalLineTo(4f)
      curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
      verticalLineToRelative(14f)
      horizontalLineToRelative(2f)
      verticalLineTo(3f)
      horizontalLineToRelative(12f)
      verticalLineTo(1f)
      close()
      moveTo(19f, 5f)
      horizontalLineTo(8f)
      curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
      verticalLineToRelative(14f)
      curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
      horizontalLineToRelative(11f)
      curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
      verticalLineTo(7f)
      curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
      close()
      moveTo(19f, 21f)
      horizontalLineTo(8f)
      verticalLineTo(7f)
      horizontalLineToRelative(11f)
      close()
    }
    .build()

@Composable
fun ReaderScreen(
  pendingScroll: MutableState<PendingScroll?>,
  onNavBarVisibleChange: (Boolean) -> Unit,
  onSelectionActiveChange: (Boolean) -> Unit,
  onOpenVerseDetail: (List<VerseRef>) -> Unit,
  onOpenBookInfo: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ReaderViewModel,
  settingsViewModel: SettingsViewModel,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (val s = state) {
    ReaderUiState.Loading -> {
      Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        CircularProgressIndicator(modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(24.dp))
        Text("Preparazione della biblioteca", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
          "Installo testi, lessico e riferimenti: accade solo alla prima apertura.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
    is ReaderUiState.Error -> {
      Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(
          "Errore durante il caricamento: ${s.throwable.message}",
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
    is ReaderUiState.Ready -> {
      val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
      ReaderContent(
        viewModel = viewModel,
        books = s.books,
        selection = s.selection,
        verses = s.verses,
        readerFontSize = settings.readerFontSize,
        pendingScroll = pendingScroll,
        onNavBarVisibleChange = onNavBarVisibleChange,
        onSelectionActiveChange = onSelectionActiveChange,
        onOpenVerseDetail = onOpenVerseDetail,
        onOpenBookInfo = onOpenBookInfo,
        modifier = modifier,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
  viewModel: ReaderViewModel,
  books: List<BookEntity>,
  selection: ReaderSelection,
  verses: List<VerseEntity>,
  readerFontSize: ReaderFontSize,
  pendingScroll: MutableState<PendingScroll?>,
  onNavBarVisibleChange: (Boolean) -> Unit,
  onSelectionActiveChange: (Boolean) -> Unit,
  onOpenVerseDetail: (List<VerseRef>) -> Unit,
  onOpenBookInfo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showBookSheet by rememberSaveable { mutableStateOf(false) }
  var showChapterSheet by rememberSaveable { mutableStateOf(false) }
  val votd by viewModel.votd.collectAsStateWithLifecycle()
  val posts by viewModel.posts.collectAsStateWithLifecycle()
  val selectedSetSaver =
    Saver<Set<Int>, List<Int>>(
      save = { it.toList() },
      restore = { it.toSet() },
    )
  var highlightedVerses by rememberSaveable(
    selection.translation,
    selection.book,
    selection.chapter,
    stateSaver = selectedSetSaver,
  ) {
    mutableStateOf(emptySet<Int>())
  }

  val currentBook = books.firstOrNull { it.n == selection.book }
  val bookName = currentBook?.name ?: ""
  val maxChapters = currentBook?.chapters ?: 1

  fun buildSelectionText(): String? {
    val selected = verses.filter { it.verse in highlightedVerses }.sortedBy { it.verse }
    if (selected.isEmpty()) return null
    val body = selected.joinToString("\n\n") { "${it.verse}  ${it.text}" }
    val reference = "$bookName ${selection.chapter} · ${TRANSLATION_NAMES[selection.translation] ?: ""}"
    return "$body\n\n— $reference".trimEnd()
  }

  fun shareSelected() {
    val text = buildSelectionText() ?: return
    shareText(text)
  }

  fun copySelected() {
    val text = buildSelectionText() ?: return
    copyToClipboard(text)
    val numbers = highlightedVerses.sorted()
    val message =
      if (numbers.size == 1) {
        "Versetto ${numbers.first()} è stato copiato"
      } else {
        "Versetti ${numbers.first()}–${numbers.last()} sono stati copiati"
      }
    toast(message)
  }

  fun showDetailFor(versesToShow: List<VerseEntity>) {
    onOpenVerseDetail(versesToShow.map { VerseRef(it.book, it.chapter, it.verse) })
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    // Screens manage their own status-bar padding; the top bar covers the inset on Bibbia.
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      AnimatedContent(
        targetState = highlightedVerses.isNotEmpty(),
        transitionSpec = {
          (
            fadeIn(ExpressiveMotion.effectsTween(ExpressiveMotion.DURATION_MEDIUM)) +
              scaleIn(
                initialScale = 0.92f,
                animationSpec = ExpressiveMotion.spatialSpring(),
              )
          ).togetherWith(fadeOut(ExpressiveMotion.effectsTween()))
        },
        label = "topBarTransition",
      ) { hasHighlight ->
        if (hasHighlight) {
          TopAppBar(
            navigationIcon = {
              IconButton(onClick = { highlightedVerses = emptySet() }) {
                Icon(AppIcons.Close, contentDescription = "Deseleziona versetti")
              }
            },
            title = {
              Text(
                if (highlightedVerses.size == 1) "1 versetto"
                else "${highlightedVerses.size} versetti",
              )
            },
            actions = {
              IconButton(onClick = { copySelected() }) {
                Icon(CopyIcon, contentDescription = "Copia versetti")
              }
              IconButton(onClick = { shareSelected() }) {
                Icon(AppIcons.Share, contentDescription = "Condividi")
              }
              IconButton(
                onClick = {
                  showDetailFor(verses.filter { it.verse in highlightedVerses }.sortedBy { it.verse })
                },
              ) {
                Icon(AppIcons.Info, contentDescription = "Dettaglio versetti")
              }
            },
          )
        } else {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp),
              ) {
                Column(
                  Modifier
                    .clickable { showBookSheet = true }
                    .padding(vertical = 4.dp),
                ) {
                  Text("$bookName ${selection.chapter}", style = MaterialTheme.typography.titleMedium)
                  Text(
                    TRANSLATION_NAMES[selection.translation] ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(onClick = onOpenBookInfo) {
                  Icon(AppIcons.Info, contentDescription = "Info sul libro")
                }
              }
            },
            actions = {
              TextButton(
                onClick = { viewModel.selectChapter(selection.chapter - 1) },
                enabled = selection.chapter > 1,
              ) {
                Text("‹", style = MaterialTheme.typography.titleLarge)
              }
              TextButton(onClick = { showChapterSheet = true }) { Text("Capitoli") }
              TextButton(
                onClick = { viewModel.selectChapter(selection.chapter + 1) },
                enabled = selection.chapter < maxChapters,
              ) {
                Text("›", style = MaterialTheme.typography.titleLarge)
              }
            },
          )
        }
      }
    },
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      val listState = rememberLazyListState()
      LaunchedEffect(selection) {
        onNavBarVisibleChange(true)
      }
      LaunchedEffect(highlightedVerses) {
        onSelectionActiveChange(highlightedVerses.isNotEmpty())
      }
      LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
          Triple(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
            listState.canScrollForward,
          )
        }
          .collect { (index, offset, canScrollForward) ->
            when {
              !canScrollForward -> onNavBarVisibleChange(true)
              index < lastIndex -> onNavBarVisibleChange(true)
              index > lastIndex || offset > lastOffset -> onNavBarVisibleChange(false)
              offset < lastOffset -> onNavBarVisibleChange(true)
            }
            lastIndex = index
            lastOffset = offset
          }
      }
      // Contenuti curati dal CMS (VOTD + feed) come banner fisso, solo in
      // "home" (Genesi 1). Fuori dalla LazyColumn: niente problemi di
      // ancoraggio/culling con l'arrivo asincrono dei dati. Gated dal
      // feature flag SHOW_CMS_HOME_CONTENT (struttura attiva, UI nascosta).
      Column(Modifier.fillMaxSize()) {
        if (SHOW_CMS_HOME_CONTENT && selection.book == 1 && selection.chapter == 1) {
          votd?.let { display ->
            VotdCard(
              display = display,
              onClick = {
                viewModel.selectBook(display.ref.book)
                viewModel.selectChapter(display.ref.chapter)
                pendingScroll.value =
                  PendingScroll(display.ref.book, display.ref.chapter, display.ref.verse)
              },
            )
          }
          if (posts.isNotEmpty()) {
            FeedSection(
              posts = posts,
              onOpenVerse = { post ->
                viewModel.selectBook(post.book)
                viewModel.selectChapter(post.chapter)
                pendingScroll.value = PendingScroll(post.book, post.chapter, post.verse)
              },
            )
          }
        }
        // Testo centrato e largo al massimo Dimens.readingMaxWidth (tablet): il
        // margine sta nel contentPadding, così si scorre su tutta la larghezza.
        val bottomClearance = LocalBottomBarClearance.current
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = centeringPadding(maxWidth, Dimens.readingMaxWidth)
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = side, end = side, bottom = bottomClearance),
        ) {
          items(verses, key = { "${it.translation}-${it.book}-${it.chapter}-${it.verse}" }) { verse ->
          VerseRow(
            verse = verse,
            highlighted = verse.verse in highlightedVerses,
            fontSize = readerFontSize,
            onClick = {
              highlightedVerses =
                if (verse.verse in highlightedVerses) highlightedVerses - verse.verse
                else highlightedVerses + verse.verse
            },
            onLongClick = { showDetailFor(listOf(verse)) },
          )
        }
      }
      }
      }
      // La chiave è il VALORE dello stato (come nel sorgente con `by`): il
      // ripristino del capitolo fa ripartire l'effetto e consuma lo scroll.
      LaunchedEffect(verses, pendingScroll.value) {
        val target = pendingScroll.value ?: return@LaunchedEffect
        val first = verses.firstOrNull() ?: return@LaunchedEffect
        if (first.book != target.book || first.chapter != target.chapter) return@LaunchedEffect
        val index = verses.indexOfFirst { it.verse == target.verse }
        listState.scrollToItem(if (index >= 0) index else 0)
        pendingScroll.value = null
      }
    }
  }

  if (showBookSheet) {
    ModalBottomSheet(
      onDismissRequest = { showBookSheet = false },
      sheetState =
        rememberBottomSheetState(
          initialValue = SheetValue.Hidden,
          // Tutto espanso: parzialmente espanso escluso (come skipPartiallyExpanded = true).
          enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
      Text(
        "Scegli un libro",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        item { SectionHeader("Antico Testamento", Modifier.padding(horizontal = 24.dp)) }
        items(books.filter { it.n <= 39 }, key = { it.n }) { book ->
          BookSheetRow(
            name = book.name,
            abbr = book.displayAbbr,
            selected = book.n == selection.book,
            onClick = {
              viewModel.selectBook(book.n)
              showBookSheet = false
              showChapterSheet = true
            },
          )
        }
        item { SectionHeader("Nuovo Testamento", Modifier.padding(horizontal = 24.dp)) }
        items(books.filter { it.n >= 40 }, key = { it.n }) { book ->
          BookSheetRow(
            name = book.name,
            abbr = book.displayAbbr,
            selected = book.n == selection.book,
            onClick = {
              viewModel.selectBook(book.n)
              showBookSheet = false
              showChapterSheet = true
            },
          )
        }
      }
    }
  }

  if (showChapterSheet) {
    ModalBottomSheet(onDismissRequest = { showChapterSheet = false }) {
      Text(
        "$bookName — capitolo",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
      ) {
        items((1..maxChapters).toList()) { chapter ->
          val selected = chapter == selection.chapter
          Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
              modifier =
                Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .then(
                    if (selected) {
                      Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                      Modifier
                    }
                  )
                  .clickable {
                    viewModel.selectChapter(chapter)
                    showChapterSheet = false
                  },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                chapter.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color =
                  if (selected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerseRow(
  verse: VerseEntity,
  highlighted: Boolean,
  fontSize: ReaderFontSize,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val underlineColor = MaterialTheme.colorScheme.primary
  Column(
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 2.dp)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(horizontal = 8.dp),
  ) {
    verse.title?.let { title ->
      Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
      )
    }
    Row(Modifier.padding(top = if (verse.title == null) 6.dp else 0.dp)) {
      Text(
        verse.verse.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 2.dp, end = 4.dp),
      )
      var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
      Box(
        Modifier
          .then(
            if (highlighted) {
              Modifier
                .background(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                  RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
            } else {
              Modifier
            }
          )
          .drawBehind {
          val layout = textLayout ?: return@drawBehind
          if (!highlighted) return@drawBehind
          val dash = 8.dp.toPx()
          val gap = 6.dp.toPx()
          val stroke = 2.dp.toPx()
          for (line in 0 until layout.lineCount) {
            val y = layout.getLineBaseline(line) + 3.dp.toPx()
            drawLine(
              color = underlineColor,
              start = Offset(layout.getLineLeft(line), y),
              end = Offset(layout.getLineRight(line), y),
              strokeWidth = stroke,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f),
            )
          }
        },
      ) {
        Text(
          verse.text,
          style = ScriptureTypography.reader(fontSize),
          onTextLayout = { textLayout = it },
        )
      }
    }
  }
}

/** Card del versetto del giorno (contenuto curato dal CMS), mostrata in home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VotdCard(
  display: VotdDisplay,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        "Versetto del giorno",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
      )
      Spacer(Modifier.height(6.dp))
      Text(
        display.text,
        style = ScriptureTypography.reader(ReaderFontSize.NORMAL),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        "${TRANSLATION_NAMES[display.translation] ?: display.translation} · ${display.ref.book}:${display.ref.chapter}:${display.ref.verse}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
      )
      display.note?.takeIf { it.isNotBlank() }?.let { note ->
        Spacer(Modifier.height(8.dp))
        Text(
          note,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

/** Sezione feed con i contenuti curati pubblicati dal CMS. */
@Composable
private fun FeedSection(
  posts: List<PostEntity>,
  onOpenVerse: (PostEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Text(
      "In evidenza",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    posts.take(3).forEach { post ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(Modifier.padding(14.dp)) {
          Text(post.title, style = MaterialTheme.typography.titleSmall)
          Spacer(Modifier.height(4.dp))
          Text(
            post.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 5,
          )
          val hasVerse = post.book > 0
          if (hasVerse) {
            Spacer(Modifier.height(8.dp))
            Text(
              "Vai al versetto · ${post.book}:${post.chapter}:${post.verse}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.clickable { onOpenVerse(post) },
            )
          }
        }
      }
    }
  }
}
