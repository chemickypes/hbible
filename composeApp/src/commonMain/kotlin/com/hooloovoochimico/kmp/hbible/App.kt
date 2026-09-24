package com.hooloovoochimico.kmp.hbible

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
import com.hooloovoochimico.kmp.hbible.di.coreModule
import com.hooloovoochimico.kmp.hbible.di.platformModule
import com.hooloovoochimico.kmp.hbible.di.viewModelModule
import com.hooloovoochimico.kmp.hbible.theme.ExpressiveMotion
import com.hooloovoochimico.kmp.hbible.theme.HBibleTheme
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import com.hooloovoochimico.kmp.hbible.ui.explore.ExploreScreen
import com.hooloovoochimico.kmp.hbible.ui.explore.ExploreViewModel
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

// --- Destinazioni del nav graph (route type-safe) ---

@Serializable
data object ReaderRoute

@Serializable
data object NotesRoute

@Serializable
data object ExploreRoute

@Serializable
data object SettingsRoute

@Serializable
data class VerseDetailRoute(
  val book: Int,
  val chapter: Int,
  val verse: Int,
  val index: Int,
  val count: Int,
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
 * Owner radice dei ViewModel condivisi: nel sorgente erano activity-scoped
 * (Hilt). Su Android LocalViewModelStoreOwner ricade sull'Activity, ma su iOS
 * non c'è un default: si fornisce esplicitamente questo owner così i VM hoist
 * nel guscio vivono per tutta la sessione, su tutte le piattaforme.
 */
private class RootViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore: ViewModelStore = ViewModelStore()
}

@Composable
fun App() {
    // Bootstrap Koin (opzione A, PLAN §12): il grafo vive con la UI, uniforme
    // su tutte le piattaforme; le definizioni sono lazy (nessun avvio DB qui).
    // Koin 4.2: l'overload con KoinAppDeclaration è deprecato → koinConfiguration {}.
    KoinApplication(koinConfiguration { modules(coreModule, platformModule, viewModelModule) }) {
        val rootOwner = remember { RootViewModelStoreOwner() }
        DisposableEffect(rootOwner) {
            onDispose { rootOwner.viewModelStore.clear() }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides rootOwner) {
            // VM condivisi (scoping Activity del sorgente → owner radice del guscio).
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val readerViewModel: ReaderViewModel = koinViewModel()
            val notesViewModel: NotesViewModel = koinViewModel()
            val exploreViewModel: ExploreViewModel = koinViewModel()
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
            HBibleTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                HBibleAppShell(
                    readerViewModel = readerViewModel,
                    notesViewModel = notesViewModel,
                    exploreViewModel = exploreViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}

@Composable
private fun HBibleAppShell(
  readerViewModel: ReaderViewModel,
  notesViewModel: NotesViewModel,
  exploreViewModel: ExploreViewModel,
  settingsViewModel: SettingsViewModel,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  val isTabDestination: (NavDestination?) -> Boolean = { destination ->
    destination != null &&
      (
        destination.hasRouteCompat<ReaderRoute>() ||
          destination.hasRouteCompat<NotesRoute>() ||
          destination.hasRouteCompat<ExploreRoute>() ||
          destination.hasRouteCompat<SettingsRoute>()
      )
  }

  // Visibilità della bottom bar (sorgente: showNavBar di ReaderContent):
  // hide-on-scroll nel lettore + nascosta durante la selezione versetti e
  // sulle destinazioni full-screen.
  var navBarVisibleByScroll by remember { mutableStateOf(true) }
  var selectionActive by remember { mutableStateOf(false) }
  val pendingScroll = remember { mutableStateOf<PendingScroll?>(null) }

  // Insieme di versetti aperto nel dettaglio (sorgente: detailSet/detailIndex
  // di ReaderContent, rememberSaveable → qui a livello di guscio, condiviso
  // con la destinazione VerseDetail).
  val refListSaver =
    Saver<List<VerseRef>, List<Int>>(
      save = { list -> list.flatMap { listOf(it.book, it.chapter, it.verse) } },
      restore = { flat -> flat.chunked(3).map { VerseRef(it[0], it[1], it[2]) } },
    )
  var detailSet by rememberSaveable(stateSaver = refListSaver) { mutableStateOf(emptyList<VerseRef>()) }
  var detailIndex by rememberSaveable { mutableStateOf(0) }

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

  // Porta il lettore al versetto indicato (sorgente: goToVerse di ReaderContent).
  val goToVerse: (Int, Int, Int) -> Unit = { book, chapter, verse ->
    navBarVisibleByScroll = true
    selectionActive = false
    pendingScroll.value = PendingScroll(book, chapter, verse)
    val sel = currentSelection()
    if (book != sel.book) {
      readerViewModel.selectBook(book)
      if (chapter != 1) readerViewModel.selectChapter(chapter)
    } else if (chapter != sel.chapter) {
      readerViewModel.selectChapter(chapter)
    }
    if (currentDestination?.hasRouteCompat<ReaderRoute>() != true) {
      goToTab(ReaderRoute)
    }
  }

  // Apre il dettaglio di un versetto da chip/riferimenti (sorgente: openDetailFor):
  // chiude editor/dettaglio correnti e porta il lettore al capitolo del versetto.
  val openDetailFor: (VerseRef) -> Unit = { target ->
    detailSet = listOf(target)
    detailIndex = 0
    readerViewModel.openVerseDetail(listOf(target), 0)
    pendingScroll.value = PendingScroll(target.book, target.chapter, target.verse)
    val sel = currentSelection()
    if (target.book != sel.book) {
      readerViewModel.selectBook(target.book)
      if (target.chapter != 1) readerViewModel.selectChapter(target.chapter)
    } else if (target.chapter != sel.chapter) {
      readerViewModel.selectChapter(target.chapter)
    }
    navController.navigate(VerseDetailRoute(target.book, target.chapter, target.verse, 0, 1)) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = false }
      launchSingleTop = true
    }
  }

  // Salva una risposta chat AI in nota e apre l'editor (sorgente: saveChatToNote):
  // atterra sulla tab NOTE con l'editor sopra, come nel sorgente.
  val saveChatToNote: suspend (String, String, Int, Int, Int) -> Unit = { title, content, b, c, v ->
    val id = notesViewModel.createNoteFromChat(title, content, b, c, v)
    goToTab(NotesRoute)
    navController.navigate(NoteEditorRoute(id))
  }

  Box(Modifier.fillMaxSize()) {
    NavHost(navController = navController, startDestination = ReaderRoute, modifier = Modifier.fillMaxSize()) {
      composable<ReaderRoute> {
        ReaderScreen(
          pendingScroll = pendingScroll,
          onNavBarVisibleChange = { navBarVisibleByScroll = it },
          onSelectionActiveChange = { selectionActive = it },
          onOpenVerseDetail = { refs ->
            detailSet = refs
            detailIndex = 0
            readerViewModel.openVerseDetail(refs, 0)
            refs.firstOrNull()?.let { ref ->
              navController.navigate(VerseDetailRoute(ref.book, ref.chapter, ref.verse, 0, refs.size))
            }
          },
          onOpenBookInfo = {
            val book = currentSelection().book
            if (book > 0) navController.navigate(BookInfoRoute(book))
          },
          viewModel = readerViewModel,
          settingsViewModel = settingsViewModel,
        )
      }
      composable<NotesRoute> {
        val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
        val books = (readerState as? ReaderUiState.Ready)?.books.orEmpty()
        NotesScreen(
          books = books,
          onDismiss = { goToTab(ReaderRoute) },
          onOpenNote = { id -> navController.navigate(NoteEditorRoute(id)) },
          onNewNote = { navController.navigate(NoteEditorRoute(null)) },
          viewModel = notesViewModel,
        )
      }
      composable<ExploreRoute> {
        val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
        val selection = (readerState as? ReaderUiState.Ready)?.selection
        val scope = rememberCoroutineScope()
        ExploreScreen(
          translation = selection?.translation ?: "NR",
          translationName = TRANSLATION_NAMES[selection?.translation ?: "NR"] ?: "",
          onDismiss = { goToTab(ReaderRoute) },
          onGoToVerse = goToVerse,
          onOpenNote = { id ->
            goToTab(NotesRoute)
            navController.navigate(NoteEditorRoute(id))
          },
          onSaveReflectionToNote = { theme, reflection ->
            scope.launch {
              saveChatToNote("Pronto soccorso · $theme", reflection, 0, 0, 0)
            }
          },
          viewModel = exploreViewModel,
        )
      }
      composable<SettingsRoute> {
        val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
        val selection = (readerState as? ReaderUiState.Ready)?.selection
        SettingsScreen(
          selectionTranslation = selection?.translation ?: "NR",
          onSelectTranslation = readerViewModel::selectTranslation,
          onOpenAiSettings = { navController.navigate(AiSettingsRoute) },
          onDismiss = { goToTab(ReaderRoute) },
          viewModel = settingsViewModel,
        )
      }
      composable<VerseDetailRoute> { entry ->
        val route = entry.toRoute<VerseDetailRoute>()
        val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
        val ready = readerState as? ReaderUiState.Ready
        val books = ready?.books.orEmpty()
        val verses = ready?.verses.orEmpty()
        val selection = ready?.selection
        val detail by readerViewModel.verseDetail.collectAsStateWithLifecycle()
        val set = detailSet.ifEmpty { listOf(VerseRef(route.book, route.chapter, route.verse)) }
        val index = detailIndex.coerceIn(0, set.lastIndex.coerceAtLeast(0))
        val currentDetailRef = set.getOrNull(index) ?: VerseRef(route.book, route.chapter, route.verse)
        // Ripristino dopo process death (l'insieme dettaglio è rememberSaveable
        // nel guscio): riapre la richiesta se il ViewModel non ne ha una valida.
        LaunchedEffect(Unit) {
          val current = readerViewModel.verseDetail.value
          if (current == null || current.ref != currentDetailRef) {
            readerViewModel.openVerseDetail(set, index)
          }
        }
        val scope = rememberCoroutineScope()
        VerseDetailScreen(
          verse = verses.firstOrNull {
            it.book == currentDetailRef.book && it.chapter == currentDetailRef.chapter && it.verse == currentDetailRef.verse
          },
          detail = detail?.takeIf { it.ref == currentDetailRef },
          reference = "${books.firstOrNull { it.n == currentDetailRef.book }?.name ?: ""} " +
            "${currentDetailRef.chapter}:${currentDetailRef.verse}",
          count = set.size,
          index = index,
          translation = selection?.translation ?: "NR",
          translationName = TRANSLATION_NAMES[selection?.translation ?: "NR"] ?: "",
          bookName = { n -> books.firstOrNull { it.n == n }?.name ?: "" },
          onSaveAiToNote =
            if (readerViewModel.aiConfigured) {
              { content ->
                scope.launch {
                  saveChatToNote(
                    "Note su ${books.firstOrNull { it.n == currentDetailRef.book }?.name.orEmpty()} " +
                      "${currentDetailRef.chapter}:${currentDetailRef.verse}",
                    content,
                    currentDetailRef.book,
                    currentDetailRef.chapter,
                    currentDetailRef.verse,
                  )
                }
              }
            } else {
              null
            },
          onDismiss = {
            detailSet = emptyList()
            detailIndex = 0
            readerViewModel.closeVerseDetail()
            navController.popBackStack()
          },
          onPrev = {
            if (index > 0) {
              detailIndex -= 1
              readerViewModel.openVerseDetail(set, detailIndex)
            }
          },
          onNext = {
            if (index < set.lastIndex) {
              detailIndex += 1
              readerViewModel.openVerseDetail(set, detailIndex)
            }
          },
          onOpenReference = { target ->
            detailSet = emptyList()
            detailIndex = 0
            readerViewModel.closeVerseDetail()
            // Sorgente: detailSet = emptyList() chiude l'overlay → qui si rimuove
            // la destinazione dal back stack, poi si porta il lettore al versetto.
            navController.popBackStack()
            goToVerse(target.book, target.chapter, target.verse)
          },
          onOpenDetail = openDetailFor,
          viewModel = readerViewModel,
        )
      }
      composable<BookInfoRoute> { entry ->
        val route = entry.toRoute<BookInfoRoute>()
        val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
        val books = (readerState as? ReaderUiState.Ready)?.books.orEmpty()
        val infoBook = books.firstOrNull { it.n == route.book }
        if (infoBook != null) {
          LaunchedEffect(infoBook.n) { readerViewModel.openBookInfo(infoBook.n) }
          val scope = rememberCoroutineScope()
          BookInfoScreen(
            book = infoBook,
            onSaveToNote = { content ->
              scope.launch {
                val id = notesViewModel.createNoteFromChat("Chat su ${infoBook.name}", content, infoBook.n, 0, 0)
                navController.navigate(NoteEditorRoute(id))
              }
            },
            onDismiss = {
              readerViewModel.closeBookInfo()
              navController.popBackStack()
            },
            viewModel = readerViewModel,
          )
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
      composable<AiSettingsRoute> {
        AiSettingsScreen(
          onDismiss = { navController.popBackStack() },
          viewModel = settingsViewModel,
        )
      }
    }

    // Bottom bar flottante del guscio (sorgente: ReaderTab bar di ReaderContent).
    val onReader = currentDestination?.hasRouteCompat<ReaderRoute>() == true
    val barVisible =
      isTabDestination(currentDestination) && navBarVisibleByScroll && (!onReader || !selectionActive)
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
      FloatingBottomBar(
        currentRoute = currentDestination,
        onSelect = { tab ->
          when (tab) {
            ReaderTab.BIBBIA -> goToTab(ReaderRoute)
            ReaderTab.NOTE -> goToTab(NotesRoute)
            ReaderTab.RICERCA -> goToTab(ExploreRoute)
            ReaderTab.IMPOSTAZIONI -> goToTab(SettingsRoute)
          }
        },
      )
    }
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
      Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ReaderTab.entries.forEach { tab ->
        val selected = when (tab) {
          ReaderTab.BIBBIA -> currentRoute?.hasRouteCompat<ReaderRoute>() == true
          ReaderTab.NOTE -> currentRoute?.hasRouteCompat<NotesRoute>() == true
          ReaderTab.RICERCA -> currentRoute?.hasRouteCompat<ExploreRoute>() == true
          ReaderTab.IMPOSTAZIONI -> currentRoute?.hasRouteCompat<SettingsRoute>() == true
        }
        val icon =
          when (tab) {
            ReaderTab.BIBBIA -> BibleIcon
            ReaderTab.NOTE -> Icons.Default.Edit
            ReaderTab.RICERCA -> Icons.Default.Search
            ReaderTab.IMPOSTAZIONI -> Icons.Default.Settings
          }
        Box(
          Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
              if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .clickable { onSelect(tab) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            icon,
            contentDescription = tab.name,
            tint =
              if (selected) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
