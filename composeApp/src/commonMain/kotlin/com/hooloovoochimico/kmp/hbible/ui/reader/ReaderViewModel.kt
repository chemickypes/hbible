package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.LastPosition
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ReaderSelection(
  val translation: String = "NR",
  val book: Int = 1,
  val chapter: Int = 1,
)

/** Content of the verse detail page. */
data class VerseDetail(
  val ref: VerseRef,
  val original: com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity?,
  val references: List<VerseRef>,
  /** Number of verses in the opened set and current index (0-based). */
  val count: Int,
  val index: Int,
)

/** State of the book info page: cached introduction plus generation progress. */
data class BookInfoUiState(
  val book: Int = 0,
  val info: BookInfoEntity? = null,
  val busy: Boolean = false,
  val error: String? = null,
)

sealed interface ReaderUiState {
  data object Loading : ReaderUiState

  data class Error(val throwable: Throwable) : ReaderUiState

  data class Ready(
    val books: List<BookEntity>,
    val selection: ReaderSelection,
    val verses: List<VerseEntity>,
  ) : ReaderUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
  private val repository: BibleRepository,
  private val settingsRepository: SettingsRepository,
  private val aiGateway: AiGateway,
) : ViewModel() {

  private val initialPosition = settingsRepository.loadLastPosition()
  private val selection =
    MutableStateFlow(ReaderSelection(initialPosition.translation, initialPosition.book, initialPosition.chapter))
  private val imported = MutableStateFlow(false)
  private val error = MutableStateFlow<Throwable?>(null)
  private val detailRequest = MutableStateFlow<Pair<List<VerseRef>, Int>?>(null)

  init {
    viewModelScope.launch {
      try {
        repository.ensureImported()
      } catch (t: Throwable) {
        error.value = t
        return@launch
      }
      imported.value = true
    }
    // Persist the reading position whenever the user moves to another chapter.
    viewModelScope.launch {
      selection.drop(1).collect {
        settingsRepository.saveLastPosition(LastPosition(it.translation, it.book, it.chapter))
      }
    }
  }

  private val verses =
    selection.flatMapLatest { s ->
      repository.chapter(s.translation, s.book, s.chapter)
    }

  val uiState: StateFlow<ReaderUiState> =
    combine(error, imported, repository.books(), selection, verses) {
        err,
        ready,
        books,
        sel,
        chapterVerses,
     ->
      val e = err
      when {
        e != null -> ReaderUiState.Error(e)
        !ready -> ReaderUiState.Loading
        else -> ReaderUiState.Ready(books, sel, chapterVerses)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderUiState.Loading)

  /** Detail page content for the requested verse; null while nothing is open. */
  val verseDetail: StateFlow<VerseDetail?> =
    detailRequest.flatMapLatest { req ->
      if (req == null) {
        flowOf(null)
      } else {
        val (verses, index) = req
        val ref = verses[index]
        combine(
          repository.originalVerse(ref.book, ref.chapter, ref.verse),
          repository.crossReferences(ref.book, ref.chapter, ref.verse),
        ) { original, refs ->
          VerseDetail(
            ref = ref,
            original = original,
            references = refs.map { VerseRef(it.toBook, it.toChapter, it.toVerse) },
            count = verses.size,
            index = index,
          )
        }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  fun openVerseDetail(verses: List<VerseRef>, index: Int = 0) {
    if (verses.isEmpty()) return
    detailRequest.value = verses to index.coerceIn(0, verses.lastIndex)
  }

  fun closeVerseDetail() {
    detailRequest.value = null
  }

  fun selectTranslation(translation: String) {
    selection.value = selection.value.copy(translation = translation)
  }

  fun selectBook(book: Int) {
    selection.value = selection.value.copy(book = book, chapter = 1)
  }

  fun selectChapter(chapter: Int) {
    selection.value = selection.value.copy(chapter = chapter)
  }

  suspend fun verse(book: Int, chapter: Int, verse: Int): VerseEntity? =
    repository.verse(selection.value.translation, book, chapter, verse)

  /** Other verses containing the same original-language lemma (Strong's number). */
  suspend fun lemmaOccurrences(lang: String, lemma: String, ref: VerseRef): List<VerseRef> =
    repository
      .lemmaOccurrences(lang, lemma, ref.book, ref.chapter, ref.verse)
      .map { VerseRef(it.book, it.chapter, it.verse) }

  /** Strong's dictionary entry for a lemma, if bundled. */
  suspend fun lexeme(lang: String, number: String): com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity? =
    repository.lexeme(lang, number)

  /** Translates a dictionary gloss to Italian with the configured AI and caches it. */
  suspend fun translateGloss(lang: String, number: String, gloss: String): String? =
    try {
      val translation = aiGateway.translateGloss(gloss, lang)
      if (translation.isNotBlank()) {
        repository.saveGlossIt(lang, number, translation)
        translation
      } else {
        null
      }
    } catch (t: Throwable) {
      null
    }

  /** True when at least one AI provider is configured and usable. */
  val aiConfigured: Boolean
    get() = aiGateway.chain.isNotEmpty()

  /** Multi-turn chat with the configured AI, for verse/book conversations. */
  suspend fun chat(system: String, messages: List<AiChatMessage>): String =
    aiGateway.chat(system, messages)

  // --- Book info ---

  private val bookInfoStateInternal = MutableStateFlow(BookInfoUiState())
  val bookInfoState: StateFlow<BookInfoUiState> = bookInfoStateInternal

  /** Opens the info page of a book: cached text or automatic generation. */
  fun openBookInfo(book: Int) {
    viewModelScope.launch {
      bookInfoStateInternal.value = BookInfoUiState(book = book)
      val cached =
        try {
          repository.bookInfo(book).first()
        } catch (t: Throwable) {
          null
        }
      if (bookInfoStateInternal.value.book != book) return@launch
      if (cached != null) {
        bookInfoStateInternal.value = bookInfoStateInternal.value.copy(info = cached)
      } else {
        generateBookInfo()
      }
    }
  }

  /** Generates (or regenerates) the introduction of the open book with the AI. */
  fun generateBookInfo() {
    val book = bookInfoStateInternal.value.book
    if (book == 0 || bookInfoStateInternal.value.busy) return
    viewModelScope.launch {
      bookInfoStateInternal.value = bookInfoStateInternal.value.copy(busy = true, error = null)
      try {
        val books = repository.books().first()
        val entity = books.firstOrNull { it.n == book } ?: throw IllegalStateException("Libro non trovato")
        val testament = if (book <= 39) "Antico Testamento" else "Nuovo Testamento"
        val (info, model) = aiGateway.bookInfo(entity.name, testament, entity.chapters)
        val row =
          BookInfoEntity(
            book = book,
            contextText = info.contesto.trim(),
            protagonists = info.protagonisti.trim(),
            christocentric = info.cristocentrico.trim(),
            model = model,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
          )
        repository.saveBookInfo(row)
        if (bookInfoStateInternal.value.book == book) {
          bookInfoStateInternal.value = bookInfoStateInternal.value.copy(info = row, busy = false)
        }
      } catch (t: CancellationException) {
        throw t
      } catch (t: Throwable) {
        if (bookInfoStateInternal.value.book == book) {
          bookInfoStateInternal.value =
            bookInfoStateInternal.value.copy(busy = false, error = t.message ?: "Generazione non riuscita")
        }
      }
    }
  }

  fun closeBookInfo() {
    bookInfoStateInternal.value = BookInfoUiState()
  }
}
