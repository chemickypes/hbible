package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.CmsRepository
import com.hooloovoochimico.kmp.hbible.data.LastPosition
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VotdEntity
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderSelection(
  val translation: String = "NR",
  val book: Int = 1,
  val chapter: Int = 1,
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

/** Verse of the day with its resolved text, ready to display in the reader home. */
data class VotdDisplay(
  val ref: VerseRef,
  val translation: String,
  val text: String,
  val note: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
  private val repository: BibleRepository,
  private val settingsRepository: SettingsRepository,
  private val cmsRepository: CmsRepository,
) : ViewModel() {

  private val initialPosition = settingsRepository.loadLastPosition()
  private val selection =
    MutableStateFlow(ReaderSelection(initialPosition.translation, initialPosition.book, initialPosition.chapter))
  private val imported = MutableStateFlow(false)
  private val error = MutableStateFlow<Throwable?>(null)

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

  /** Verse of the day (CMS), with the verse text resolved in the preferred translation. */
  val votd: StateFlow<VotdDisplay?> =
    cmsRepository.votdForToday().flatMapLatest<VotdEntity?, VotdDisplay?> { entry ->
      if (entry == null) {
        flowOf(null)
      } else {
        kotlinx.coroutines.flow.flow {
          val translation = entry.translation ?: selection.value.translation
          val text = repository.verse(translation, entry.book, entry.chapter, entry.verse)?.text
          if (text != null) {
            emit(VotdDisplay(VerseRef(entry.book, entry.chapter, entry.verse), translation, text, entry.note))
          } else {
            emit(null)
          }
        }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  /** Curated feed items (CMS), newest first. */
  val posts: StateFlow<List<PostEntity>> =
    cmsRepository.publishedPosts()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
