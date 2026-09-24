package com.hooloovoochimico.kmp.hbible.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRef
import com.hooloovoochimico.kmp.hbible.data.BibleReferenceParser
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.ExploreRepository
import com.hooloovoochimico.kmp.hbible.data.NotesRepository
import com.hooloovoochimico.kmp.hbible.data.SavedAiSearch
import com.hooloovoochimico.kmp.hbible.data.SearchHistoryEntry
import com.hooloovoochimico.kmp.hbible.data.SearchScope
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiMatches
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.ui.common.displayAbbr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Whole state of the explore (search) screen. */
data class ExploreUiState(
  val query: String = "",
  val scope: SearchScope = SearchScope.ALL,
  val aiMode: Boolean = false,
  val noteMode: Boolean = false,
  /** Tema del pronto soccorso attivo per la query corrente (null = ricerca normale). */
  val firstAidTheme: String? = null,
  val searching: Boolean = false,
  val results: List<VerseEntity> = emptyList(),
  val noteResults: List<NoteEntity> = emptyList(),
  /** Last AI search outcome tied to the current query (matches or error). */
  val ai: SavedAiSearch? = null,
  val aiSearching: Boolean = false,
  val history: List<SearchHistoryEntry> = emptyList(),
)

/** A reference returned by the AI, resolved against the local books table. */
data class AiVerseMatch(
  val ref: BibleRef,
  val verseEnd: Int? = null,
  val endChapter: Int? = null,
  val text: String? = null,
)

data class AiSearchResults(val perfect: List<AiVerseMatch>, val similar: List<AiVerseMatch>)

private const val SEARCH_DEBOUNCE_MS = 250L

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(
  private val exploreRepository: ExploreRepository,
  private val bibleRepository: BibleRepository,
  private val notesRepository: NotesRepository,
  private val aiGateway: AiGateway,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      ExploreUiState(
        query = exploreRepository.loadLastQuery(),
        scope = exploreRepository.loadLastScope(),
        aiMode = exploreRepository.loadAiMode(),
        ai = exploreRepository.loadSavedAi(),
        history = exploreRepository.history(),
      ),
    )
  val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

  /** Books of the Bible, used to resolve AI references and build "Vai a" links. */
  val books: StateFlow<List<BookEntity>> =
    bibleRepository.books().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  /** Translation used to load verse previews; kept in sync with the reader. */
  private val translation = MutableStateFlow("NR")

  fun setTranslation(value: String) {
    translation.value = value
  }

  private var aiSearchJob: Job? = null

  init {
    // Debounced live search: text verses or notes, never in AI mode.
    viewModelScope.launch {
      _uiState
        .map { SearchSpec(it.query.trim(), it.scope, it.noteMode, it.aiMode) }
        .distinctUntilChanged()
        .collectLatest { spec ->
          if (spec.aiMode || spec.query.isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), noteResults = emptyList(), searching = false) }
            return@collectLatest
          }
          delay(SEARCH_DEBOUNCE_MS)
          val isNoteSearch = spec.noteMode
          _uiState.update { it.copy(searching = true, results = emptyList(), noteResults = emptyList()) }
          if (isNoteSearch) {
            val found =
              try {
                notesRepository.searchNotes(spec.query)
              } catch (t: Throwable) {
                emptyList()
              }
            _uiState.update { it.copy(searching = false, noteResults = found) }
          } else {
            val found =
              try {
                exploreRepository.search(translation.value, spec.query, spec.scope)
              } catch (t: Throwable) {
                emptyList()
              }
            _uiState.update { it.copy(searching = false, results = found) }
          }
        }
    }
  }

  /**
   * AI matches resolved against the books table, enriched with the local verse
   * text. Valid only for the query they were produced with.
   */
  val aiResults: StateFlow<AiSearchResults?> =
    combine(
      _uiState.map { Pair(it.ai, it.query.trim()) }.distinctUntilChanged(),
      books,
    ) { savedPair, bookList ->
      val (saved, query) = savedPair
      if (saved == null || saved.error != null || saved.matches == null || saved.query != query) {
        null
      } else {
        buildAiResults(saved.matches, bookList)
      }
    }
      .flatMapLatest { resolved ->
        if (resolved == null) {
          flowOf(null)
        } else {
          flow<AiSearchResults> {
            emit(resolved)
            emit(resolved.withTexts())
          }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // --- Events ---

  fun onQueryChange(query: String) {
    _uiState.update { it.copy(query = query, firstAidTheme = null) }
    exploreRepository.saveLastQuery(query, _uiState.value.scope)
  }

  fun onScopeChange(scope: SearchScope) {
    _uiState.update { it.copy(scope = scope, noteMode = false) }
    exploreRepository.saveLastQuery(_uiState.value.query, scope)
  }

  fun onToggleNoteMode() {
    _uiState.update { it.copy(noteMode = !it.noteMode) }
  }

  fun onToggleAiMode() {
    val newMode = !_uiState.value.aiMode
    _uiState.update { it.copy(aiMode = newMode, firstAidTheme = null) }
    exploreRepository.saveAiMode(newMode)
    // Run a search only when there is nothing to restore for this query.
    val state = _uiState.value
    if (newMode && state.query.isNotBlank() && state.ai?.query != state.query.trim()) {
      runAiSearch()
    }
  }

  /** Avvia una ricerca "pronto soccorso" sul tema scelto. */
  fun onFirstAid(theme: String) {
    _uiState.update { it.copy(firstAidTheme = theme, aiMode = true, query = theme.lowercase()) }
    exploreRepository.saveAiMode(true)
    exploreRepository.saveLastQuery(theme.lowercase(), _uiState.value.scope)
    runAiSearch()
  }

  fun runAiSearch() {
    val prompt = _uiState.value.query.trim()
    if (prompt.isEmpty() || _uiState.value.aiSearching) return
    aiSearchJob?.cancel()
    _uiState.update { it.copy(aiSearching = true, ai = null) }
    val theme = _uiState.value.firstAidTheme
    aiSearchJob =
      viewModelScope.launch {
        try {
          // Reuse previous results for the same query: no new tokens spent.
          val cached = exploreRepository.findAiCached(prompt)?.matches
          val bookCodes = books.value.map { it.displayAbbr }
          val matches =
            cached
              ?: (if (theme != null) aiGateway.firstAid(theme, bookCodes) else aiGateway.search(prompt, bookCodes))
                .also {
                  exploreRepository.addHistory(
                    SearchHistoryEntry(
                      prompt,
                      aiMode = true,
                      matches = it,
                      timestamp = Clock.System.now().toEpochMilliseconds(),
                    ),
                  )
                  _uiState.update { s -> s.copy(history = exploreRepository.history()) }
                }
          exploreRepository.saveAi(prompt, matches, null)
          _uiState.update { it.copy(ai = SavedAiSearch(prompt, matches, null), aiSearching = false) }
        } catch (t: CancellationException) {
          throw t
        } catch (t: Throwable) {
          val message = t.message ?: "Errore durante la ricerca AI"
          exploreRepository.saveAi(prompt, null, message)
          _uiState.update { it.copy(ai = SavedAiSearch(prompt, null, message), aiSearching = false) }
        }
      }
  }

  /** Records a text search in the history (explicit searches and result taps). */
  fun recordQuery(query: String) {
    val q = query.trim()
    if (q.isEmpty()) return
    exploreRepository.addHistory(
      SearchHistoryEntry(q, aiMode = false, timestamp = Clock.System.now().toEpochMilliseconds()),
    )
    _uiState.update { it.copy(history = exploreRepository.history()) }
  }

  /** Restores a past search, reusing the cached AI matches when present. */
  fun restoreHistory(entry: SearchHistoryEntry) {
    aiSearchJob?.cancel()
    _uiState.update {
      it.copy(
        query = entry.query,
        firstAidTheme = null,
        aiMode = entry.aiMode,
        ai =
          if (entry.aiMode && entry.matches != null) {
            SavedAiSearch(entry.query, entry.matches, null)
          } else {
            it.ai
          },
      )
    }
    exploreRepository.saveLastQuery(entry.query, _uiState.value.scope)
    exploreRepository.saveAiMode(entry.aiMode)
  }

  fun clearHistory() {
    exploreRepository.clearHistory()
    _uiState.update { it.copy(history = emptyList()) }
  }

  private suspend fun AiSearchResults.withTexts(): AiSearchResults =
    copy(
      perfect = perfect.map { withText(it) },
      similar = similar.map { withText(it) },
    )

  private suspend fun withText(match: AiVerseMatch): AiVerseMatch =
    match.copy(
      text =
        bibleRepository.verse(translation.value, match.ref.book, match.ref.chapter, match.ref.verse ?: 1)
          ?.text
          ?.replace("\n", " "),
    )

  private data class SearchSpec(
    val query: String,
    val scope: SearchScope,
    val noteMode: Boolean,
    val aiMode: Boolean,
  )
}

/**
 * Parses an AI reference such as "1 Gv 1:3" or "1Cor 13:4-7" into a resolved
 * match, keeping the full range for display.
 */
internal fun resolveAiMatch(raw: String, books: List<BookEntity>): AiVerseMatch? {
  val cleaned =
    raw.trim().trim('"', '\'', '.', ';', ',', '!', '?')
      .replace('–', '-')
      .replace('—', '-')
      .replace(';', ':')
  val start = cleaned.substringBefore('-').trim()
  if (start.isEmpty()) return null
  val ref = BibleReferenceParser.parse(start, books) ?: return null

  var endChapter: Int? = null
  var verseEnd: Int? = null
  val endPart = cleaned.substringAfter('-', "").trim()
  if (endPart.isNotEmpty()) {
    Regex("^(\\d{1,3})(?::(\\d{1,3}))?$").find(endPart)?.let { m ->
      val a = m.groupValues[1].toIntOrNull()
      val b = m.groupValues[2].toIntOrNull()
      if (b != null) {
        endChapter = a
        verseEnd = b
      } else if (a != null) {
        verseEnd = a
      }
    }
  }
  if (ref.verse == null) {
    // "Sal 23-24" is a chapter range; the verse template does not apply.
    endChapter = null
    verseEnd = null
  } else {
    if (endChapter == ref.chapter) endChapter = null
    val ve = verseEnd
    if (endChapter == null && ve != null && ve <= ref.verse) verseEnd = null
  }
  return AiVerseMatch(ref, verseEnd, endChapter)
}

/** Label like "1 Corinzi 13:4-7" or "Giovanni 3:16". */
internal fun aiRangeLabel(match: AiVerseMatch, bookNames: Map<Int, String>): String {
  val r = match.ref
  val book = bookNames[r.book].orEmpty()
  val verse = r.verse
  if (verse == null) return "$book ${r.chapter}"
  val end =
    when {
      match.verseEnd == null -> return "$book ${r.chapter}:$verse"
      match.endChapter != null -> "${match.endChapter}:${match.verseEnd}"
      else -> "${match.verseEnd}"
    }
  return "$book ${r.chapter}:$verse-$end"
}

internal fun buildAiResults(matches: AiMatches, books: List<BookEntity>): AiSearchResults =
  AiSearchResults(
    matches.perfect
      .mapNotNull { resolveAiMatch(it, books) }
      .distinctBy { Triple(it.ref.book, it.ref.chapter, it.ref.verse) }
      .take(8),
    matches.similar
      .mapNotNull { resolveAiMatch(it, books) }
      .distinctBy { Triple(it.ref.book, it.ref.chapter, it.ref.verse) }
      .take(8),
  )
