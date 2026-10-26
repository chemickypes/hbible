package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.content.PackageFetcher
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import com.hooloovoochimico.kmp.hbible.ui.common.passageText
import com.hooloovoochimico.kmp.hbible.ui.explore.AiVerseMatch
import com.hooloovoochimico.kmp.hbible.ui.explore.resolveAiMatch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Content package with the introductions of the books (written in the CMS). */
const val BOOK_INFO_PACKAGE = "book_info.json"

/** State of the book info page. */
data class BookInfoUiState(
  val book: Int = 0,
  val info: BookInfoEntity? = null,
  /** Key verse resolved against the books, with its text in the reader's translation. */
  val keyVerse: AiVerseMatch? = null,
  /** Downloading the introductions (first opening, before the daily sync). */
  val loading: Boolean = false,
  /** Not available: offline at first opening, or the CMS has no introduction for this book. */
  val unavailable: Boolean = false,
)

/**
 * Pagina "info sul libro": introduzione scritta nel CMS (pacchetto
 * `book_info.json`, scaricato con il sync o su richiesta alla prima apertura)
 * più la chat facoltativa con l'AI dell'utente. Legato alla destinazione
 * BookInfo: lo stato si azzera quando la pagina esce dal back stack.
 */
class BookInfoViewModel(
  private val repository: BibleRepository,
  private val aiGateway: AiGateway,
  settingsRepository: SettingsRepository,
  private val fetcher: PackageFetcher,
) : ViewModel() {

  private val stateInternal = MutableStateFlow(BookInfoUiState())
  val state: StateFlow<BookInfoUiState> = stateInternal

  private var job: Job? = null
  private var translation: String = ""

  /** Opens the info page of a book; the key verse is shown in [translation]. */
  fun open(book: Int, translation: String) {
    if (book == stateInternal.value.book && translation == this.translation && job?.isActive == true) return
    this.translation = translation
    job?.cancel()
    stateInternal.value = BookInfoUiState(book = book, loading = true)
    job =
      viewModelScope.launch {
        launch { download(book) }
        repository.bookInfo(book).collect { info ->
          if (info == null) return@collect
          val books = repository.books().first()
          val key =
            resolveAiMatch(info.keyVerse, books)?.let { m ->
              m.copy(text = repository.passageText(translation, m))
            }
          stateInternal.value = BookInfoUiState(book = book, info = info, keyVerse = key)
        }
      }
  }

  /** Tries the download again after [BookInfoUiState.unavailable]. */
  fun retry() {
    val book = stateInternal.value.book
    if (book == 0 || stateInternal.value.loading) return
    stateInternal.value = stateInternal.value.copy(loading = true, unavailable = false)
    viewModelScope.launch { download(book) }
  }

  /** Downloads the package when missing; marks the page unavailable when it still has no entry. */
  private suspend fun download(book: Int) {
    if (repository.bookInfo(book).first() != null) return
    fetcher.ensurePackage(BOOK_INFO_PACKAGE)
    if (repository.bookInfo(book).first() == null && stateInternal.value.book == book) {
      stateInternal.value = stateInternal.value.copy(loading = false, unavailable = true)
    }
  }

  /** Interruttore generale delle funzioni IA (Impostazioni → Versione x7); spento = niente elementi IA. */
  val aiFeaturesEnabled: StateFlow<Boolean> = settingsRepository.aiFeaturesEnabled

  /**
   * True when the AI features are on and at least one AI provider is configured and usable.
   * Reattivo: segue [SettingsRepository.aiConfig] e il flag, così la chat compare anche nel
   * dettaglio già aperto dopo aver configurato un provider in Impostazioni
   * (stessa regola di AiGateway.chain, config.enabledChain()).
   */
  val aiConfigured: StateFlow<Boolean> =
    combine(settingsRepository.aiFeaturesEnabled, settingsRepository.aiConfig) { enabled, config ->
      enabled && config.enabledChain().isNotEmpty()
    }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        settingsRepository.aiFeaturesEnabled.value && settingsRepository.aiConfig.value.enabledChain().isNotEmpty(),
      )

  /** Multi-turn chat with the configured AI about the book. */
  suspend fun chat(system: String, messages: List<AiChatMessage>): String = aiGateway.chat(system, messages)
}
