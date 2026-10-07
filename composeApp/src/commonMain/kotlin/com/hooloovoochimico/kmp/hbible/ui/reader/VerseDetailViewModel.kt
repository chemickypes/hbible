package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Content of the verse detail page. */
data class VerseDetail(
  val ref: VerseRef,
  val original: OriginalVerseEntity?,
  val references: List<VerseRef>,
  /** Number of verses in the opened set and current index (0-based). */
  val count: Int,
  val index: Int,
)

/**
 * Dettaglio versetto: testo originale, riferimenti incrociati, lessico Strong e
 * chat AI. Condiviso a livello di guscio perché lo stesso dettaglio si mostra
 * come destinazione a tutto schermo (telefono) o come pannello accanto al
 * lettore (finestre larghe).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerseDetailViewModel(
  private val repository: BibleRepository,
  private val aiGateway: AiGateway,
) : ViewModel() {

  private val detailRequest = MutableStateFlow<Pair<List<VerseRef>, Int>?>(null)

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

  fun open(verses: List<VerseRef>, index: Int = 0) {
    if (verses.isEmpty()) return
    detailRequest.value = verses to index.coerceIn(0, verses.lastIndex)
  }

  fun close() {
    detailRequest.value = null
  }

  /** A verse in the given translation (verse text of references and occurrences). */
  suspend fun verse(translation: String, book: Int, chapter: Int, verse: Int): VerseEntity? =
    repository.verse(translation, book, chapter, verse)

  /** Other verses containing the same original-language lemma (Strong's number). */
  suspend fun lemmaOccurrences(lang: String, lemma: String, ref: VerseRef): List<VerseRef> =
    repository
      .lemmaOccurrences(lang, lemma, ref.book, ref.chapter, ref.verse)
      .map { VerseRef(it.book, it.chapter, it.verse) }

  /** Strong's dictionary entry for a lemma, if bundled. */
  suspend fun lexeme(lang: String, number: String): LexemeEntity? = repository.lexeme(lang, number)

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
    } catch (e: CancellationException) {
      throw e
    } catch (t: Throwable) {
      null
    }

  /** True when at least one AI provider is configured and usable. */
  val aiConfigured: Boolean
    get() = aiGateway.chain.isNotEmpty()

  /** Multi-turn chat with the configured AI about the verse. */
  suspend fun chat(system: String, messages: List<AiChatMessage>): String = aiGateway.chat(system, messages)
}
