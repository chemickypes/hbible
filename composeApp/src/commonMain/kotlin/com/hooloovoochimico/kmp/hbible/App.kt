package com.hooloovoochimico.kmp.hbible

import com.hooloovoochimico.kmp.hbible.data.DEFAULT_TRANSLATION
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import com.hooloovoochimico.kmp.hbible.ui.reader.BookInfoViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.VerseDetailViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import com.hooloovoochimico.kmp.hbible.theme.Dimens
import com.hooloovoochimico.kmp.hbible.theme.Spacing
import com.hooloovoochimico.kmp.hbible.ui.common.ContentWidth
import com.hooloovoochimico.kmp.hbible.ui.common.LocalBottomBarClearance
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_NAMES
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import com.hooloovoochimico.kmp.hbible.theme.ExpressiveMotion
import com.hooloovoochimico.kmp.hbible.theme.HBibleTheme
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import com.hooloovoochimico.kmp.hbible.ui.explore.ExploreScreen
import com.hooloovoochimico.kmp.hbible.ui.explore.ExploreViewModel
import com.hooloovoochimico.kmp.hbible.ui.interlineare.InterlineareScreen
import com.hooloovoochimico.kmp.hbible.ui.interlineare.InterlineareViewModel
import com.hooloovoochimico.kmp.hbible.ui.notes.NoteEditorScreen
import com.hooloovoochimico.kmp.hbible.ui.notes.NotesScreen
import com.hooloovoochimico.kmp.hbible.ui.notes.NotesViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.BookInfoScreen
import com.hooloovoochimico.kmp.hbible.ui.reader.PendingScroll
import com.hooloovoochimico.kmp.hbible.ui.reader.ReaderScreen
import com.hooloovoochimico.kmp.hbible.ui.reader.ReaderSelection
import com.hooloovoochimico.kmp.hbible.ui.reader.ReaderUiState
import com.hooloovoochimico.kmp.hbible.ui.reader.ReaderViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.VerseDetailScreen
import com.hooloovoochimico.kmp.hbible.ui.settings.AiSettingsScreen
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsScreen
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsViewModel
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

// --- Destinazioni del nav graph (route type-safe) ---

@Serializable
data object ReaderRoute

@Serializable
data object NotesRoute

@Serializable
data object ExploreRoute

@Serializable
data object InterlineareRoute

@Serializable
data object SettingsRoute

@Serializable
data class VerseDetailRoute(
  val book: Int,
  val chapter: Int,
  val verse: Int,
  val index: Int,
  val count: Int,
  /** Word initially selected in the detail card (-1 = none), from Interlineare. */
  val word: Int = -1,
)

@Serializable
data class BookInfoRoute(val book: Int)

@Serializable
data class NoteEditorRoute(val noteId: Long? = null)

@Serializable
data object AiSettingsRoute

/**
 * Controllo route type-safe. In navigation 2.10 `NavDestination.hasRoute<T>()`
 * è una member extension del Companion: la chiamata richiede il Companion in
 * scope (il helper lo fornisce).
 */
private inline fun <reified T : Any> NavDestination?.hasRouteCompat(): Boolean {
  val destination = this ?: return false
  return with(NavDestination.Companion) { destination.hasRoute<T>() }
}

/** Top-level destinations of the floating bottom bar. */
internal enum class ReaderTab {
  BIBBIA,
  NOTE,
  RICERCA,
  INTERLINEARE,
  IMPOSTAZIONI,
}

/** Simple filled book with a bookmark, used for the Bibbia destination. */
private val BibleIcon: ImageVector =
  ImageVector.Builder(
    name = "BibleBook",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  )
    .path(fill = SolidColor(Color.Black)) {
      moveTo(18f, 2f)
      horizontalLineTo(6f)
      curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
      verticalLineToRelative(16f)
      curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
      horizontalLineToRelative(12f)
      curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
      verticalLineTo(4f)
      curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
      close()
      moveTo(6f, 4f)
      horizontalLineTo(11f)
      verticalLineTo(12f)
      lineTo(8.5f, 10.5f)
      lineTo(6f, 12f)
      close()
    }
    .build()

/**
 * Owner radice dei ViewModel condivisi (nel sorgente erano activity-scoped con
 * Hilt). Usato SOLO come fallback quando la piattaforma non fornisce un
 * [LocalViewModelStoreOwner]: su Android c'è sempre l'Activity, che conserva i
 * ViewModel attraverso rotazione/resize/multi-window; un owner creato con
 * `remember` verrebbe invece distrutto (e i VM svuotati) a ogni ricreazione.
 */
private class RootViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore: ViewModelStore = ViewModelStore()
}

/** Owner della piattaforma se presente, altrimenti uno legato alla composizione. */
@Composable
private fun rememberRootViewModelStoreOwner(): ViewModelStoreOwner {
  LocalViewModelStoreOwner.current?.let { return it }
  val fallback = remember { RootViewModelStoreOwner() }
  DisposableEffect(fallback) {
    onDispose { fallback.viewModelStore.clear() }
  }
  return fallback
}

@Composable
fun App() {
    // Koin è già avviato a livello di processo (initKoin in HBibleApplication /
    // MainViewController): qui nessun bootstrap, così la ricreazione della UI
    // non ricrea il grafo né riapre il DB.
    val rootOwner = rememberRootViewModelStoreOwner()
    CompositionLocalProvider(LocalViewModelStoreOwner provides rootOwner) {
        // VM condivisi (scoping Activity del sorgente → owner radice del guscio).
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val readerViewModel: ReaderViewModel = koinViewModel()
        val notesViewModel: NotesViewModel = koinViewModel()
        val exploreViewModel: ExploreViewModel = koinViewModel()
        val interlineareViewModel: InterlineareViewModel = koinViewModel()
        val verseDetailViewModel: VerseDetailViewModel = koinViewModel()
        val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
        HBibleTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
            HBibleAppShell(
                readerViewModel = readerViewModel,
                notesViewModel = notesViewModel,
                exploreViewModel = exploreViewModel,
                interlineareViewModel = interlineareViewModel,
                settingsViewModel = settingsViewModel,
                verseDetailViewModel = verseDetailViewModel,
            )
        }
    }
}

@Composable
private fun HBibleAppShell(
  readerViewModel: ReaderViewModel,
  notesViewModel: NotesViewModel,
  exploreViewModel: ExploreViewModel,
  interlineareViewModel: InterlineareViewModel,
  settingsViewModel: SettingsViewModel,
  verseDetailViewModel: VerseDetailViewModel,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  // Sync contenuti dal CMS all'apertura (silenzioso; niente se URL non impostato).
  LaunchedEffect(Unit) { settingsViewModel.autoCheckOnOpen() }

  val isTabDestination: (NavDestination?) -> Boolean = { destination ->
    destination != null &&
      (
        destination.hasRouteCompat<ReaderRoute>() ||
          destination.hasRouteCompat<NotesRoute>() ||
          destination.hasRouteCompat<ExploreRoute>() ||
          destination.hasRouteCompat<InterlineareRoute>() ||
          destination.hasRouteCompat<SettingsRoute>()
      )
  }

  // Visibilità della bottom bar (sorgente: showNavBar di ReaderContent):
  // hide-on-scroll nel lettore + nascosta durante la selezione versetti e
  // sulle destinazioni full-screen.
  var navBarVisibleByScroll by remember { mutableStateOf(true) }
  var selectionActive by remember { mutableStateOf(false) }
  val pendingScroll = remember { mutableStateOf<PendingScroll?>(null) }

  // Finestre larghe (≥ 600dp: tablet, pieghevoli aperti, telefono in
  // orizzontale): navigation rail fissa a sinistra al posto della bottom bar
  // flottante, che su schermi ampi è lontana dal pollice e copre il testo.
  val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
  val useRail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
  // Finestre espanse (≥ 840dp: tablet in orizzontale, desktop windowing): il
  // dettaglio versetto si apre in un pannello accanto al lettore invece che a
  // tutto schermo. Sotto questa soglia due colonne sarebbero troppo strette.
  val twoPane = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

  // Insieme di versetti aperto nel dettaglio (sorgente: detailSet/detailIndex
  // di ReaderContent, rememberSaveable → qui a livello di guscio, condiviso
  // fra la destinazione VerseDetail e il pannello accanto al lettore).
  val refListSaver =
    Saver<List<VerseRef>, List<Int>>(
      save = { list -> list.flatMap { listOf(it.book, it.chapter, it.verse) } },
      restore = { flat -> flat.chunked(3).map { VerseRef(it[0], it[1], it[2]) } },
    )
  var detailSet by rememberSaveable(stateSaver = refListSaver) { mutableStateOf(emptyList<VerseRef>()) }
  var detailIndex by rememberSaveable { mutableStateOf(0) }
  // Dettaglio mostrato come pannello del lettore (solo con twoPane).
  var detailInPane by rememberSaveable { mutableStateOf(false) }

  // Editor note mostrato come pannello dell'elenco (6a, solo con twoPane):
  // null = nessuna nota aperta, 0L = nota nuova, >0 = id della nota.
  var notesPaneId by rememberSaveable { mutableStateOf<Long?>(null) }

  // Dettaglio versetto mostrato come pannello di Esplora (6b, solo con
  // twoPane). Mutuamente esclusivo con il pannello del lettore (detailInPane):
  // aprendo l'uno si chiude l'altro.
  var exploreDetailOpen by rememberSaveable { mutableStateOf(false) }

  // Info libro mostrate come pannello del lettore (6c, solo con twoPane),
  // alternativa al pannello del dettaglio: aprire uno chiude l'altro.
  var bookInfoInPane by rememberSaveable { mutableStateOf(false) }

  // Richiesta di focus sul campo di ricerca di Esplora (6e, Ctrl+F): contatore,
  // così ogni pressione ritriggera il LaunchedEffect della schermata.
  var exploreFocusSearchRequest by rememberSaveable { mutableStateOf(0) }

  // Selezione corrente del lettore, letta al momento dell'uso nei callback.
  fun currentSelection(): ReaderSelection =
    (readerViewModel.uiState.value as? ReaderUiState.Ready)?.selection ?: ReaderSelection()

  // Cambio tab: launchSingleTop + popUpTo(start){saveState} + restoreState.
  val goToTab: (Any) -> Unit = { route ->
    navBarVisibleByScroll = true
    selectionActive = false
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  // Porta il lettore sul capitolo del versetto e lo fa scorrere fino a lì.
  fun syncReaderTo(target: VerseRef) {
    pendingScroll.value = PendingScroll(target.book, target.chapter, target.verse)
    val sel = currentSelection()
    if (target.book != sel.book) {
      readerViewModel.selectBook(target.book)
      if (target.chapter != 1) readerViewModel.selectChapter(target.chapter)
    } else if (target.chapter != sel.chapter) {
      readerViewModel.selectChapter(target.chapter)
    }
  }

  // Porta il lettore al versetto indicato (sorgente: goToVerse di ReaderContent).
  val goToVerse: (Int, Int, Int) -> Unit = { book, chapter, verse ->
    navBarVisibleByScroll = true
    selectionActive = false
    syncReaderTo(VerseRef(book, chapter, verse))
    if (currentDestination?.hasRouteCompat<ReaderRoute>() != true) {
      goToTab(ReaderRoute)
    }
  }

  fun setDetail(refs: List<VerseRef>) {
    detailSet = refs
    detailIndex = 0
    verseDetailViewModel.open(refs, 0)
  }

  fun closeDetail() {
    detailSet = emptyList()
    detailIndex = 0
    detailInPane = false
    exploreDetailOpen = false
    verseDetailViewModel.close()
  }

  // Chiude il pannello info libro (6c): X del pannello o apertura del dettaglio.
  fun closeBookInfoPane() {
    bookInfoInPane = false
  }

  // Apre il dettaglio di un versetto da chip/riferimenti (sorgente: openDetailFor):
  // chiude editor/dettaglio correnti e porta il lettore al capitolo del versetto.
  // Con twoPane il dettaglio si apre nel pannello del lettore.
  val openDetailFor: (VerseRef) -> Unit = { target ->
    setDetail(listOf(target))
    syncReaderTo(target)
    if (twoPane) {
      detailInPane = true
      exploreDetailOpen = false
      closeBookInfoPane()
      if (currentDestination?.hasRouteCompat<ReaderRoute>() != true) goToTab(ReaderRoute)
    } else {
      navController.navigate(VerseDetailRoute(target.book, target.chapter, target.verse, 0, 1)) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
      }
    }
  }

  // Apre il dettaglio su una parola dalla tab Interlineare: stesso flusso di
  // openDetailFor (sincronizza il lettore sul capitolo così il dettaglio mostra
  // anche il versetto italiano) ma sempre come destinazione e senza popUpTo,
  // così "indietro" torna all'interlineare, con la parola da selezionare.
  val openInterlinearWord: (VerseRef, Int) -> Unit = { target, word ->
    setDetail(listOf(target))
    detailInPane = false
    syncReaderTo(target)
    navController.navigate(VerseDetailRoute(target.book, target.chapter, target.verse, 0, 1, word)) {
      launchSingleTop = true
    }
  }

  // Cambio di larghezza con un dettaglio aperto (rotazione, resize, pieghevole):
  // pannello → pagina quando la finestra si stringe, pagina → pannello quando si
  // allarga (solo se la pagina è stata aperta dal lettore).
  LaunchedEffect(twoPane) {
    val destination = navController.currentDestination
    if (!twoPane && detailInPane) {
      detailInPane = false
      val first = detailSet.firstOrNull()
      if (first != null && destination.hasRouteCompat<ReaderRoute>()) {
        navController.navigate(VerseDetailRoute(first.book, first.chapter, first.verse, detailIndex, detailSet.size))
      }
    } else if (
      twoPane &&
      destination.hasRouteCompat<VerseDetailRoute>() &&
      navController.previousBackStackEntry?.destination.hasRouteCompat<ReaderRoute>()
    ) {
      navController.popBackStack()
      detailInPane = detailSet.isNotEmpty()
    }
  }

  // Pannello note (6a): se la finestra si stringe, l'editor del pannello diventa
  // pagina (solo se l'elenco note è la destinazione corrente); se si allarga e
  // l'editor è pagina aperta dall'elenco, diventa pannello.
  LaunchedEffect(twoPane) {
    val destination = navController.currentDestination
    if (!twoPane && notesPaneId != null) {
      val id = notesPaneId
      notesPaneId = null
      if (destination.hasRouteCompat<NotesRoute>()) {
        navController.navigate(NoteEditorRoute(id?.takeIf { it > 0L }))
      }
    } else if (
      twoPane &&
      destination.hasRouteCompat<NoteEditorRoute>() &&
      navController.previousBackStackEntry?.destination.hasRouteCompat<NotesRoute>()
    ) {
      val editing = navController.currentBackStackEntry?.toRoute<NoteEditorRoute>()?.noteId ?: 0L
      navController.popBackStack()
      notesPaneId = editing
    }
  }

  // Pannello di Esplora (6b): restringendo, il dettaglio nel pannello diventa
  // pagina; allargando, la pagina aperta da Esplora torna pannello.
  LaunchedEffect(twoPane) {
    val destination = navController.currentDestination
    if (!twoPane && exploreDetailOpen) {
      exploreDetailOpen = false
      val first = detailSet.firstOrNull()
      if (first != null && destination.hasRouteCompat<ExploreRoute>()) {
        navController.navigate(VerseDetailRoute(first.book, first.chapter, first.verse, detailIndex, detailSet.size))
      }
    } else if (
      twoPane &&
      destination.hasRouteCompat<VerseDetailRoute>() &&
      navController.previousBackStackEntry?.destination.hasRouteCompat<ExploreRoute>()
    ) {
      navController.popBackStack()
      exploreDetailOpen = detailSet.isNotEmpty()
    }
  }

  // Pannello info libro (6c): restringendo diventa pagina; allargando, la
  // pagina aperta dal lettore torna pannello.
  LaunchedEffect(twoPane) {
    val destination = navController.currentDestination
    if (!twoPane && bookInfoInPane) {
      val book = currentSelection().book
      bookInfoInPane = false
      if (book > 0 && destination.hasRouteCompat<ReaderRoute>()) {
        navController.navigate(BookInfoRoute(book))
      }
    } else if (
      twoPane &&
      destination.hasRouteCompat<BookInfoRoute>() &&
      navController.previousBackStackEntry?.destination.hasRouteCompat<ReaderRoute>()
    ) {
      navController.popBackStack()
      bookInfoInPane = true
    }
  }

  // Apre l'editor di una nota: nel pannello dell'elenco con twoPane (6a),
  // come pagina a tutto schermo altrimenti.
  val openNote: (Long?) -> Unit = { id ->
    goToTab(NotesRoute)
    if (twoPane) {
      notesPaneId = id ?: 0L
    } else {
      navController.navigate(NoteEditorRoute(id))
    }
  }

  // Salva una risposta chat AI in nota e apre l'editor (sorgente: saveChatToNote):
  // atterra sulla tab NOTE con l'editor sopra, come nel sorgente.
  val saveChatToNote: suspend (String, String, Int, Int, Int) -> Unit = { title, content, b, c, v ->
    val id = notesViewModel.createNoteFromChat(title, content, b, c, v)
    openNote(id)
  }

  // Apre il dettaglio di un versetto da Esplora (6b): con twoPane resta su
  // Esplora e lo mostra nel pannello a destra (chiudendo quello del lettore),
  // altrimenti porta al lettore come nel sorgente.
  val openDetailFromExplore: (Int, Int, Int) -> Unit = { b, c, v ->
    setDetail(listOf(VerseRef(b, c, v)))
    if (twoPane) {
      detailInPane = false
      exploreDetailOpen = true
    } else {
      goToVerse(b, c, v)
    }
  }

  // Porta la tab Interlineare sul versetto indicato (azione della pagina dettaglio
  // versetto): moveTo valida i confini, poi si seleziona la tab come dal guscio.
  val openInterlinearFor: (VerseRef) -> Unit = { target ->
    interlineareViewModel.moveTo(target.book, target.chapter, target.verse)
    goToTab(InterlineareRoute)
  }

  val bottomClearance =
    if (useRail) {
      WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.lg
    } else {
      Dimens.bottomBarClearance
    }
  val selectTab: (ReaderTab) -> Unit = { tab -> goToTab(tab.route) }
  val shellScope = rememberCoroutineScope()

  // Dettaglio versetto, identico come pagina e come pannello: cambiano solo la
  // chiusura e cosa succede aprendo un riferimento.
  @Composable
  fun VerseDetail(initialWord: Int, fallback: VerseRef?, onClose: () -> Unit) {
    VerseDetailContent(
      readerViewModel = readerViewModel,
      viewModel = verseDetailViewModel,
      set = detailSet.ifEmpty { listOfNotNull(fallback) },
      index = detailIndex,
      initialWord = initialWord,
      onIndexChange = { detailIndex = it },
      onSaveAiToNote = { title, content, ref ->
        shellScope.launch { saveChatToNote(title, content, ref.book, ref.chapter, ref.verse) }
      },
      onDismiss = {
        closeDetail()
        onClose()
      },
      onOpenReference = { target ->
        // Sorgente: detailSet = emptyList() chiude l'overlay, poi si porta il
        // lettore al versetto.
        closeDetail()
        onClose()
        goToVerse(target.book, target.chapter, target.verse)
      },
      onOpenDetail = openDetailFor,
      onOpenInterlinear = openInterlinearFor,
    )
  }

  val rootFocus = remember { FocusRequester() }
  Box(
    Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      // Scorciatoie da tastiera (6e): focusabile per ricevere onKeyEvent;
      // onKeyEvent (bolla) e non onPreviewKeyEvent, così i campi di testo
      // consumano i tasti per primi.
      .focusRequester(rootFocus)
      .focusable()
      .onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when {
          event.isCtrlPressed && event.key == Key.F -> {
            goToTab(ExploreRoute)
            exploreFocusSearchRequest += 1
            true
          }
          event.key == Key.Escape -> {
            when {
              detailInPane || exploreDetailOpen -> closeDetail()
              bookInfoInPane -> closeBookInfoPane()
              notesPaneId != null -> notesPaneId = null
              else -> return@onKeyEvent false
            }
            true
          }
          event.key == Key.DirectionRight || event.key == Key.DirectionLeft -> {
            // Capitolo successivo/precedente solo sul lettore e senza selezione
            // attiva: nei limiti del libro.
            val onReader = currentDestination?.hasRouteCompat<ReaderRoute>() == true
            val ready = readerViewModel.uiState.value as? ReaderUiState.Ready
            if (!onReader || ready == null || selectionActive) return@onKeyEvent false
            val max = ready.books.firstOrNull { it.n == ready.selection.book }?.chapters ?: return@onKeyEvent false
            when {
              event.key == Key.DirectionRight && ready.selection.chapter < max -> {
                readerViewModel.selectChapter(ready.selection.chapter + 1)
                true
              }
              event.key == Key.DirectionLeft && ready.selection.chapter > 1 -> {
                readerViewModel.selectChapter(ready.selection.chapter - 1)
                true
              }
              else -> false
            }
          }
          else -> false
        }
      },
  ) {
    LaunchedEffect(Unit) { rootFocus.requestFocus() }
    Row(Modifier.fillMaxSize()) {
      if (useRail) {
        AppNavigationRail(currentRoute = currentDestination, onSelect = selectTab)
      }
      Box(Modifier.weight(1f).fillMaxHeight()) {
        CompositionLocalProvider(LocalBottomBarClearance provides bottomClearance) {
          NavHost(navController = navController, startDestination = ReaderRoute, modifier = Modifier.fillMaxSize()) {
            composable<ReaderRoute> {
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val books = (readerState as? ReaderUiState.Ready)?.books.orEmpty()
              val paneScope = rememberCoroutineScope()
              Row(Modifier.fillMaxSize()) {
                ReaderScreen(
                  pendingScroll = pendingScroll,
                  onNavBarVisibleChange = { navBarVisibleByScroll = it },
                  onSelectionActiveChange = { selectionActive = it },
                  onOpenVerseDetail = { refs ->
                    val first = refs.firstOrNull() ?: return@ReaderScreen
                    setDetail(refs)
                    if (twoPane) {
                      detailInPane = true
                      exploreDetailOpen = false
                      closeBookInfoPane()
                    } else {
                      navController.navigate(VerseDetailRoute(first.book, first.chapter, first.verse, 0, refs.size))
                    }
                  },
                  onOpenBookInfo = {
                    val book = currentSelection().book
                    if (book > 0) {
                      if (twoPane) {
                        // Pannello info libro (6c), alternativo al dettaglio.
                        detailInPane = false
                        exploreDetailOpen = false
                        bookInfoInPane = true
                      } else {
                        navController.navigate(BookInfoRoute(book))
                      }
                    }
                  },
                  widePickers = useRail,
                  viewModel = readerViewModel,
                  settingsViewModel = settingsViewModel,
                  modifier = Modifier.weight(1f),
                )
                if (twoPane && detailInPane && detailSet.isNotEmpty()) {
                  VerticalDivider()
                  Box(Modifier.width(Dimens.detailPaneWidth).fillMaxHeight()) {
                    VerseDetail(initialWord = -1, fallback = null, onClose = {})
                  }
                }
                if (twoPane && bookInfoInPane) {
                  // Pannello info libro (6c): VM separato da quello della
                  // pagina, riaperto al cambio di libro del lettore.
                  VerticalDivider()
                  Box(Modifier.width(Dimens.detailPaneWidth).fillMaxHeight()) {
                    val bookInfoViewModel: BookInfoViewModel = koinViewModel(key = "bookinfo-pane")
                    val paneBook =
                      (readerState as? ReaderUiState.Ready)?.let { state ->
                        state.books.firstOrNull { it.n == state.selection?.book }
                      }
                    if (paneBook != null) {
                      LaunchedEffect(paneBook.n) { bookInfoViewModel.open(paneBook.n) }
                      BookInfoScreen(
                        book = paneBook,
                        onSaveToNote = { content ->
                          paneScope.launch {
                            val id =
                              notesViewModel.createNoteFromChat("Chat su ${paneBook.name}", content, paneBook.n, 0, 0)
                            openNote(id)
                          }
                        },
                        onDismiss = { closeBookInfoPane() },
                        viewModel = bookInfoViewModel,
                      )
                    }
                  }
                }
              }
            }
            composable<NotesRoute> {
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val books = (readerState as? ReaderUiState.Ready)?.books.orEmpty()
              if (twoPane) {
                // Elenco + editor affiancati (6a): l'editor ha più spazio
                // (weight 1.4) e key(notesPaneId) ne azzera lo stato interno
                // cambiando nota; chiudere non tocca il back stack.
                Row(Modifier.fillMaxSize()) {
                  Box(Modifier.weight(1f)) {
                    ContentWidth {
                      NotesScreen(
                        books = books,
                        onDismiss = { goToTab(ReaderRoute) },
                        onOpenNote = { id -> notesPaneId = id },
                        onNewNote = { notesPaneId = 0L },
                        viewModel = notesViewModel,
                      )
                    }
                  }
                  VerticalDivider()
                  Box(Modifier.weight(1.4f).fillMaxHeight()) {
                    val paneId = notesPaneId
                    if (paneId == null) {
                      Text(
                        "Seleziona una nota",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                      )
                    } else {
                      key(paneId) {
                        val editorNote = remember { mutableStateOf<NoteEntity?>(null) }
                        LaunchedEffect(paneId) {
                          editorNote.value = if (paneId > 0L) notesViewModel.note(paneId) else null
                        }
                        // Come nella pagina: l'editor si mostra solo a nota
                        // caricata (o nota nuova).
                        if (paneId == 0L || editorNote.value != null) {
                          NoteEditorScreen(
                            note = editorNote.value,
                            books = books,
                            loadVerse = { ref ->
                              readerViewModel.verse(ref.book, ref.chapter, ref.verse)?.text
                            },
                            onOpenReference = { target ->
                              goToVerse(target.book, target.chapter, target.verse)
                            },
                            onOpenDetail = openDetailFor,
                            onDismiss = { notesPaneId = null },
                            viewModel = notesViewModel,
                          )
                        }
                      }
                    }
                  }
                }
              } else {
                ContentWidth {
                  NotesScreen(
                    books = books,
                    onDismiss = { goToTab(ReaderRoute) },
                    onOpenNote = { id -> navController.navigate(NoteEditorRoute(id)) },
                    onNewNote = { navController.navigate(NoteEditorRoute(null)) },
                    viewModel = notesViewModel,
                  )
                }
              }
            }
            composable<ExploreRoute> {
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val selection = (readerState as? ReaderUiState.Ready)?.selection
              val scope = rememberCoroutineScope()
              if (twoPane) {
                // Risultati + dettaglio affiancati (6b): i versetti puntati da
                // ricerca AI, "Pronto soccorso" e chip si aprono nel pannello
                // invece di portare al lettore.
                Row(Modifier.fillMaxSize()) {
                  Box(Modifier.weight(1f)) {
                    ContentWidth {
                      ExploreScreen(
                        translation = selection?.translation ?: DEFAULT_TRANSLATION,
                        translationName = TRANSLATION_NAMES[selection?.translation ?: DEFAULT_TRANSLATION] ?: "",
                        onDismiss = { goToTab(ReaderRoute) },
                        onGoToVerse = openDetailFromExplore,
                        onOpenNote = openNote,
                        onSaveReflectionToNote = { theme, reflection ->
                          scope.launch {
                            saveChatToNote("Pronto soccorso · $theme", reflection, 0, 0, 0)
                          }
                        },
                        focusSearchRequest = exploreFocusSearchRequest,
                        viewModel = exploreViewModel,
                      )
                    }
                  }
                  if (exploreDetailOpen && detailSet.isNotEmpty()) {
                    VerticalDivider()
                    Box(Modifier.width(Dimens.detailPaneWidth).fillMaxHeight()) {
                      VerseDetail(initialWord = -1, fallback = null, onClose = { exploreDetailOpen = false })
                    }
                  }
                }
              } else {
                ContentWidth {
                  ExploreScreen(
                    translation = selection?.translation ?: DEFAULT_TRANSLATION,
                    translationName = TRANSLATION_NAMES[selection?.translation ?: DEFAULT_TRANSLATION] ?: "",
                    onDismiss = { goToTab(ReaderRoute) },
                    onGoToVerse = goToVerse,
                    onOpenNote = openNote,
                    onSaveReflectionToNote = { theme, reflection ->
                      scope.launch {
                        saveChatToNote("Pronto soccorso · $theme", reflection, 0, 0, 0)
                      }
                    },
                    focusSearchRequest = exploreFocusSearchRequest,
                    viewModel = exploreViewModel,
                  )
                }
              }
            }
            composable<InterlineareRoute> {
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val selection = (readerState as? ReaderUiState.Ready)?.selection
              // La traduzione corrente del lettore decide il canale di allineamento
              // ar_<versione> e il versetto italiano mostrato dalla glossa.
              LaunchedEffect(selection?.translation) {
                interlineareViewModel.setTranslation(selection?.translation ?: DEFAULT_TRANSLATION)
              }
              InterlineareScreen(
                translationName = TRANSLATION_NAMES[selection?.translation ?: DEFAULT_TRANSLATION] ?: "",
                onOpenWord = openInterlinearWord,
                viewModel = interlineareViewModel,
              )
            }
            composable<SettingsRoute> {
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val selection = (readerState as? ReaderUiState.Ready)?.selection
              // Niente ContentWidth: su finestre larghe la schermata usa elenco + pannello.
              SettingsScreen(
                selectionTranslation = selection?.translation ?: DEFAULT_TRANSLATION,
                onSelectTranslation = readerViewModel::selectTranslation,
                onOpenAiSettings = { navController.navigate(AiSettingsRoute) },
                onDismiss = { goToTab(ReaderRoute) },
                viewModel = settingsViewModel,
              )
            }
            composable<VerseDetailRoute> { entry ->
              val route = entry.toRoute<VerseDetailRoute>()
              ContentWidth {
                VerseDetail(
                  initialWord = route.word,
                  fallback = VerseRef(route.book, route.chapter, route.verse),
                  onClose = { navController.popBackStack() },
                )
              }
            }
            composable<BookInfoRoute> { entry ->
              val route = entry.toRoute<BookInfoRoute>()
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val books = (readerState as? ReaderUiState.Ready)?.books.orEmpty()
              val infoBook = books.firstOrNull { it.n == route.book }
              if (infoBook != null) {
                // VM legato a questa destinazione: lo stato si azzera uscendo.
                val bookInfoViewModel: BookInfoViewModel = koinViewModel()
                LaunchedEffect(infoBook.n) { bookInfoViewModel.open(infoBook.n) }
                val scope = rememberCoroutineScope()
                ContentWidth {
                  BookInfoScreen(
                    book = infoBook,
                    onSaveToNote = { content ->
                      scope.launch {
                        val id = notesViewModel.createNoteFromChat("Chat su ${infoBook.name}", content, infoBook.n, 0, 0)
                        openNote(id)
                      }
                    },
                    onDismiss = { navController.popBackStack() },
                    viewModel = bookInfoViewModel,
                  )
                }
              }
            }
            composable<NoteEditorRoute> { entry ->
              val route = entry.toRoute<NoteEditorRoute>()
              val noteId = route.noteId ?: 0L
              val editorNote =
                remember(noteId) { mutableStateOf<NoteEntity?>(null) }
              LaunchedEffect(noteId) {
                editorNote.value = if (noteId > 0) notesViewModel.note(noteId) else null
              }
              val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
              val books = (readerState as? ReaderUiState.Ready)?.books.orEmpty()
              // Sorgente: l'editor si mostra solo a nota caricata (o nuova).
              if (noteId == 0L || editorNote.value != null) {
                ContentWidth {
                  NoteEditorScreen(
                    note = editorNote.value,
                    books = books,
                    loadVerse = { ref -> readerViewModel.verse(ref.book, ref.chapter, ref.verse)?.text },
                    onOpenReference = { target -> goToVerse(target.book, target.chapter, target.verse) },
                    onOpenDetail = openDetailFor,
                    onDismiss = { navController.popBackStack() },
                    viewModel = notesViewModel,
                  )
                }
              }
            }
            composable<AiSettingsRoute> {
              ContentWidth {
                AiSettingsScreen(
                  onDismiss = { navController.popBackStack() },
                  viewModel = settingsViewModel,
                )
              }
            }
          }
        }
      }
    }

    // Bottom bar flottante del guscio, solo su finestre compatte (sorgente:
    // ReaderTab bar di ReaderContent).
    val onReader = currentDestination?.hasRouteCompat<ReaderRoute>() == true
    val barVisible =
      !useRail && isTabDestination(currentDestination) && navBarVisibleByScroll && (!onReader || !selectionActive)
    AnimatedVisibility(
      visible = barVisible,
      enter =
        slideInVertically(ExpressiveMotion.fastSpatialSpring()) { it } +
          fadeIn(ExpressiveMotion.effectsTween(ExpressiveMotion.DURATION_MEDIUM)),
      exit =
        slideOutVertically(ExpressiveMotion.fastSpatialSpring()) { it } +
          fadeOut(ExpressiveMotion.effectsTween()),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      FloatingBottomBar(currentRoute = currentDestination, onSelect = selectTab)
    }
  }
}

/**
 * Dettaglio versetto collegato ai ViewModel: usato dalla destinazione
 * VerseDetail (telefono) e dal pannello accanto al lettore (finestre espanse).
 * [set]/[index] sono l'insieme aperto, conservato nel guscio con rememberSaveable.
 */
@Composable
private fun VerseDetailContent(
  readerViewModel: ReaderViewModel,
  viewModel: VerseDetailViewModel,
  set: List<VerseRef>,
  index: Int,
  initialWord: Int,
  onIndexChange: (Int) -> Unit,
  onSaveAiToNote: (title: String, content: String, ref: VerseRef) -> Unit,
  onDismiss: () -> Unit,
  onOpenReference: (VerseRef) -> Unit,
  onOpenDetail: (VerseRef) -> Unit,
  onOpenInterlinear: (VerseRef) -> Unit,
) {
  val current = set.getOrNull(index.coerceIn(0, set.lastIndex.coerceAtLeast(0))) ?: return
  val safeIndex = set.indexOf(current)
  val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
  val ready = readerState as? ReaderUiState.Ready
  val books = ready?.books.orEmpty()
  val translation = ready?.selection?.translation ?: DEFAULT_TRANSLATION
  val detail by viewModel.verseDetail.collectAsStateWithLifecycle()
  val aiConfigured by viewModel.aiConfigured.collectAsStateWithLifecycle()
  // Ripristino dopo process death / cambio di layout (l'insieme è nel guscio):
  // riapre la richiesta se il ViewModel non ne ha una valida.
  LaunchedEffect(current) {
    if (viewModel.verseDetail.value?.ref != current) viewModel.open(set, safeIndex)
  }
  val bookName: (Int) -> String = { n -> books.firstOrNull { it.n == n }?.name.orEmpty() }
  VerseDetailScreen(
    verse = ready?.verses?.firstOrNull {
      it.book == current.book && it.chapter == current.chapter && it.verse == current.verse
    },
    detail = detail?.takeIf { it.ref == current },
    reference = "${bookName(current.book)} ${current.chapter}:${current.verse}",
    count = set.size,
    index = safeIndex,
    translation = translation,
    translationName = TRANSLATION_NAMES[translation] ?: "",
    bookName = bookName,
    initialWord = initialWord,
    onSaveAiToNote =
      if (aiConfigured) {
        { content ->
          onSaveAiToNote("Note su ${bookName(current.book)} ${current.chapter}:${current.verse}", content, current)
        }
      } else {
        null
      },
    onDismiss = onDismiss,
    onPrev = {
      if (safeIndex > 0) {
        onIndexChange(safeIndex - 1)
        viewModel.open(set, safeIndex - 1)
      }
    },
    onNext = {
      if (safeIndex < set.lastIndex) {
        onIndexChange(safeIndex + 1)
        viewModel.open(set, safeIndex + 1)
      }
    },
    onOpenReference = onOpenReference,
    onOpenDetail = onOpenDetail,
    onOpenInterlinear = { onOpenInterlinear(current) },
    viewModel = viewModel,
  )
}

/** Route, etichetta e icona di ogni destinazione principale (bottom bar e rail). */
private val ReaderTab.route: Any
  get() =
    when (this) {
      ReaderTab.BIBBIA -> ReaderRoute
      ReaderTab.NOTE -> NotesRoute
      ReaderTab.RICERCA -> ExploreRoute
      ReaderTab.INTERLINEARE -> InterlineareRoute
      ReaderTab.IMPOSTAZIONI -> SettingsRoute
    }

private val ReaderTab.label: String
  get() =
    when (this) {
      ReaderTab.BIBBIA -> "Bibbia"
      ReaderTab.NOTE -> "Note"
      ReaderTab.RICERCA -> "Cerca"
      ReaderTab.INTERLINEARE -> "Interlineare"
      ReaderTab.IMPOSTAZIONI -> "Impostazioni"
    }

private val ReaderTab.icon: ImageVector
  get() =
    when (this) {
      ReaderTab.BIBBIA -> BibleIcon
      ReaderTab.NOTE -> AppIcons.Edit
      ReaderTab.RICERCA -> AppIcons.Search
      ReaderTab.INTERLINEARE -> AppIcons.Menu
      ReaderTab.IMPOSTAZIONI -> AppIcons.Settings
    }

private fun ReaderTab.isSelected(destination: NavDestination?): Boolean =
  when (this) {
    ReaderTab.BIBBIA -> destination.hasRouteCompat<ReaderRoute>()
    ReaderTab.NOTE -> destination.hasRouteCompat<NotesRoute>()
    ReaderTab.RICERCA -> destination.hasRouteCompat<ExploreRoute>()
    ReaderTab.INTERLINEARE -> destination.hasRouteCompat<InterlineareRoute>()
    ReaderTab.IMPOSTAZIONI -> destination.hasRouteCompat<SettingsRoute>()
  }

/**
 * Navigazione per finestre larghe: rail con icona + etichetta, voci centrate in
 * verticale (raggiungibili col pollice tenendo il tablet ai lati). Sempre
 * visibile, anche sulle destinazioni di dettaglio: nasconderla farebbe saltare
 * orizzontalmente il contenuto.
 */
@Composable
private fun AppNavigationRail(
  currentRoute: NavDestination?,
  onSelect: (ReaderTab) -> Unit,
) {
  NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
    Spacer(Modifier.weight(1f))
    ReaderTab.entries.forEach { tab ->
      NavigationRailItem(
        selected = tab.isSelected(currentRoute),
        onClick = { onSelect(tab) },
        icon = { Icon(tab.icon, contentDescription = null) },
        label = { Text(tab.label) },
        modifier = Modifier.padding(vertical = 4.dp),
      )
    }
    Spacer(Modifier.weight(1f))
  }
}

@Composable
private fun FloatingBottomBar(
  currentRoute: NavDestination?,
  onSelect: (ReaderTab) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 3.dp,
    shadowElevation = 6.dp,
    modifier =
      Modifier
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Row(
      Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ReaderTab.entries.forEach { tab ->
        val selected = tab.isSelected(currentRoute)
        Box(
          Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
              if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .clickable { onSelect(tab) }
            // 5 voci × (24dp icona + 2×14dp) + gap + padding: deve stare in 360dp
            // (telefoni stretti): a 20dp la quinta voce (Impostazioni) usciva
            // dallo schermo di ~28dp e non era raggiungibile.
            .padding(horizontal = 14.dp, vertical = 10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            tab.icon,
            contentDescription = tab.label,
            tint =
              if (selected) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
