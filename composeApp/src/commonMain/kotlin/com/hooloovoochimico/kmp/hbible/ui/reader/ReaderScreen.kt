package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_META
import com.hooloovoochimico.kmp.hbible.data.shareAttribution
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.layout.BoxWithConstraints
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import com.hooloovoochimico.kmp.hbible.ui.common.centeringPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.platform.copyToClipboard
import com.hooloovoochimico.kmp.hbible.platform.shareText
import com.hooloovoochimico.kmp.hbible.platform.toast
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.theme.ExpressiveMotion
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.hooloovoochimico.kmp.hbible.ui.common.TonalIcon
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
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
  widePickers: Boolean = false,
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
        widePickers = widePickers,
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
  widePickers: Boolean,
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
    // shareAttribution: per la Bibbia Aperta la CC BY-SA chiede di citare opera e licenza.
    val reference = "$bookName ${selection.chapter} · ${shareAttribution(selection.translation)}"
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

  val recentBooks by viewModel.recentBooks.collectAsStateWithLifecycle()

  /** Scelta dal foglio dei libri: apre il capitolo dall'inizio e chiude il foglio. */
  fun pickPassage(book: Int, chapter: Int) {
    viewModel.select(book, chapter)
    pendingScroll.value = PendingScroll(book, chapter, 1)
    showBookSheet = false
    showChapterSheet = false
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
                // Libro e capitolo: apre la scelta del libro.
                Row(
                  Modifier
                    .weight(1f, fill = false)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { showBookSheet = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(Modifier.weight(1f, fill = false)) {
                    Text(
                      "$bookName ${selection.chapter}",
                      style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      TRANSLATION_NAMES[selection.translation] ?: "",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Icon(
                    AppIcons.KeyboardArrowDown,
                    contentDescription = "Scegli il libro",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp).size(20.dp),
                  )
                }
                IconButton(onClick = onOpenBookInfo) {
                  Icon(
                    AppIcons.Info,
                    contentDescription = "Info sul libro",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            },
            actions = {
              ChapterStepper(
                canGoBack = selection.chapter > 1,
                canGoForward = selection.chapter < maxChapters,
                onBack = { viewModel.selectChapter(selection.chapter - 1) },
                onForward = { viewModel.selectChapter(selection.chapter + 1) },
                onOpenChapters = { showChapterSheet = true },
                modifier = Modifier.padding(end = 8.dp),
              )
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
              ),
          )
        }
      }
    },
  ) { padding ->
    Box(Modifier.fillMaxSize().padding(padding)) {
      val listState = rememberLazyListState()
      // Pannello picker laterale (6d): su finestre ampie sostituisce i bottom
      // sheet; si chiude con X, scegliendo una voce o con il tasto indietro.
      BackHandler(enabled = widePickers && (showBookSheet || showChapterSheet)) {
        showBookSheet = false
        showChapterSheet = false
      }
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
      Row(Modifier.fillMaxSize()) {
        if (widePickers) {
          AnimatedVisibility(
            visible = showBookSheet || showChapterSheet,
            enter = expandHorizontally() + fadeIn(ExpressiveMotion.effectsTween()),
            exit = shrinkHorizontally() + fadeOut(ExpressiveMotion.effectsTween()),
          ) {
            Surface(
              tonalElevation = 2.dp,
              modifier = Modifier.width(360.dp).fillMaxHeight(),
            ) {
              Column {
                Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 8.dp), horizontalArrangement = Arrangement.End) {
                  IconButton(onClick = { showBookSheet = false; showChapterSheet = false }) {
                    Icon(AppIcons.Close, contentDescription = "Chiudi")
                  }
                }
                // key: riaprendo il pannello si riparte dal passo giusto (libri o capitoli).
                key(showChapterSheet) {
                  BookChapterPicker(
                    books = books,
                    currentBook = selection.book,
                    currentChapter = selection.chapter,
                    recents = recentBooks,
                    onPick = ::pickPassage,
                    startOnChapters = showChapterSheet,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            }
          }
        }
        Column(Modifier.weight(1f).fillMaxSize()) {
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
          if (verses.isNotEmpty()) {
            item(key = "chapter-header") { ChapterHeader(bookName, selection.chapter) }
          }
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
          // "Continua": capitolo successivo, o primo capitolo del libro seguente.
          val nextBook = books.firstOrNull { it.n == selection.book + 1 }
          if (verses.isNotEmpty() && (selection.chapter < maxChapters || nextBook != null)) {
            item(key = "next-chapter") {
              val inBook = selection.chapter < maxChapters
              NextChapterCard(
                label = if (inBook) "$bookName ${selection.chapter + 1}" else "${nextBook?.name} 1",
                onClick = {
                  if (inBook) {
                    viewModel.selectChapter(selection.chapter + 1)
                    pendingScroll.value = PendingScroll(selection.book, selection.chapter + 1, 1)
                  } else if (nextBook != null) {
                    viewModel.selectBook(nextBook.n)
                    pendingScroll.value = PendingScroll(nextBook.n, 1, 1)
                  }
                },
              )
            }
          }
          // Crediti della traduzione in fondo al capitolo (obbligatori per la CC BY-SA).
          val credit = TRANSLATION_META.firstOrNull { it.abbr == selection.translation }?.attribution.orEmpty()
          if (credit.isNotBlank() && verses.isNotEmpty()) {
            item(key = "credits") {
              Text(
                credit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
              )
            }
          }
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
        // +1: la prima voce della lista è l'intestazione del capitolo; il primo
        // versetto riporta in cima, intestazione compresa.
        listState.scrollToItem(if (index > 0) index + 1 else 0)
        pendingScroll.value = null
      }
    }
  }

  if (!widePickers && (showBookSheet || showChapterSheet)) {
    ModalBottomSheet(
      onDismissRequest = { showBookSheet = false; showChapterSheet = false },
      sheetState =
        rememberBottomSheetState(
          initialValue = SheetValue.Hidden,
          // Tutto espanso: parzialmente espanso escluso (come skipPartiallyExpanded = true).
          enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
      BookChapterPicker(
        books = books,
        currentBook = selection.book,
        currentChapter = selection.chapter,
        recents = recentBooks,
        onPick = ::pickPassage,
        startOnChapters = showChapterSheet,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
      )
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
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
      )
    }
    // Un versetto che apre un paragrafo (campo `p`, Bibbia Aperta) prende più aria sopra.
    val topPadding = if (verse.title != null) 0.dp else if (verse.paragraph) 18.dp else 6.dp
    Row(Modifier.padding(top = topPadding)) {
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
        val dimColor = MaterialTheme.colorScheme.onSurfaceVariant
        val annotated =
          remember(verse.text, dimColor) {
            buildAnnotatedString {
              for (segment in scriptureSegments(verse.text)) {
                when (segment.kind) {
                  ScriptureSegment.Kind.NORMAL -> append(segment.text)
                  ScriptureSegment.Kind.SELAH ->
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = dimColor)) { append(segment.text) }
                  ScriptureSegment.Kind.VARIANT ->
                    withStyle(SpanStyle(color = dimColor)) { append(segment.text) }
                }
              }
            }
          }
        Text(
          annotated,
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

/** Navigazione tra capitoli: capsula tonale con precedente, griglia dei capitoli, successivo. */
@Composable
private fun ChapterStepper(
  canGoBack: Boolean,
  canGoForward: Boolean,
  onBack: () -> Unit,
  onForward: () -> Unit,
  onOpenChapters: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, enabled = canGoBack) {
        Icon(AppIcons.ChevronLeft, contentDescription = "Capitolo precedente")
      }
      IconButton(onClick = onOpenChapters) {
        Icon(
          AppIcons.Grid,
          contentDescription = "Capitoli",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
      IconButton(onClick = onForward, enabled = canGoForward) {
        Icon(AppIcons.ChevronRight, contentDescription = "Capitolo successivo")
      }
    }
  }
}

/** Apertura del capitolo come in un'edizione a stampa: nome del libro, numero grande, filetto. */
@Composable
private fun ChapterHeader(bookName: String, chapter: Int) {
  Column(
    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      bookName.uppercase(),
      style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      chapter.toString(),
      style = MaterialTheme.typography.displayMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Box(
      Modifier
        .padding(top = 6.dp)
        .size(width = 40.dp, height = 2.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
  }
}

/** Invito a proseguire la lettura, in fondo al capitolo. */
@Composable
private fun NextChapterCard(label: String, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp),
  ) {
    Row(
      Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          "CONTINUA",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          label,
          style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
          modifier = Modifier.padding(top = 2.dp),
        )
      }
      TonalIcon(AppIcons.ArrowForward, container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
    }
  }
}
