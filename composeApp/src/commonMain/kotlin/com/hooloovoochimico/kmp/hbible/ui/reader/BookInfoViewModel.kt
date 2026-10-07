package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.local.BookInfoEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** State of the book info page: cached introduction plus generation progress. */
data class BookInfoUiState(
  val book: Int = 0,
  val info: BookInfoEntity? = null,
  val busy: Boolean = false,
  val error: String? = null,
)

/**
 * Pagina "info sul libro": introduzione in cache o generata con l'AI, più la
 * chat sul libro. Legato alla destinazione BookInfo: lo stato si azzera quando
 * la pagina esce dal back stack.
 */
class BookInfoViewModel(
  private val repository: BibleRepository,
  private val aiGateway: AiGateway,
) : ViewModel() {

  private val stateInternal = MutableStateFlow(BookInfoUiState())
  val state: StateFlow<BookInfoUiState> = stateInternal

  /** Opens the info page of a book: cached text or automatic generation. */
  fun open(book: Int) {
    viewModelScope.launch {
      stateInternal.value = BookInfoUiState(book = book)
      val cached =
        try {
          repository.bookInfo(book).first()
        } catch (e: CancellationException) {
          throw e
        } catch (t: Throwable) {
          null
        }
      if (stateInternal.value.book != book) return@launch
      if (cached != null) {
        stateInternal.value = stateInternal.value.copy(info = cached)
      } else {
        generate()
      }
    }
  }

  /** Generates (or regenerates) the introduction of the open book with the AI. */
  fun generate() {
    val book = stateInternal.value.book
    if (book == 0 || stateInternal.value.busy) return
    viewModelScope.launch {
      stateInternal.value = stateInternal.value.copy(busy = true, error = null)
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
        if (stateInternal.value.book == book) {
          stateInternal.value = stateInternal.value.copy(info = row, busy = false)
        }
      } catch (t: CancellationException) {
        throw t
      } catch (t: Throwable) {
        if (stateInternal.value.book == book) {
          stateInternal.value =
            stateInternal.value.copy(busy = false, error = t.message ?: "Generazione non riuscita")
        }
      }
    }
  }

  /** True when at least one AI provider is configured and usable. */
  val aiConfigured: Boolean
    get() = aiGateway.chain.isNotEmpty()

  /** Multi-turn chat with the configured AI about the book. */
  suspend fun chat(system: String, messages: List<AiChatMessage>): String = aiGateway.chat(system, messages)
}
