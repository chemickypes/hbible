package com.hooloovoochimico.kmp.hbible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.verseInAllTranslations
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.ui.common.VerseRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Content of the verse detail page. */
data class VerseDetail(
  val ref: VerseRef,
  val original: OriginalVerseEntity?,
  /**
   * The verse in every bundled translation that has it, in display order (the
   * current translation included: the screen filters it out).
   */
  val versions: List<VerseEntity> = emptyList(),
  val references: List<VerseRef>,
  /** Number of verses in the opened set and current index (0-based). */
  val count: Int,
  val index: Int,
)

/**
 * Dettaglio versetto: testo originale, altre traduzioni, riferimenti incrociati
 * e chat AI. La parola originale (lessico, occorrenze) sta nell'interlineare.
 * Condiviso a livello di guscio perché lo stesso dettaglio si mostra
 * come destinazione a tutto schermo (telefono) o come pannello accanto al
 * lettore (finestre larghe).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerseDetailViewModel(
  private val repository: BibleRepository,
  private val aiGateway: AiGateway,
  settingsRepository: SettingsRepository,
) : ViewModel() {

  private val detailRequest = MutableStateFlow<Pair<List<VerseRef>, Int>?>(null)

  /** Detail page content for the requested verse; null while nothing is open. */
  val verseDetail: StateFlow<VerseDetail?> =
    detailRequest.flatMapLatest { req ->
      if (req == null) {
        flowOf<VerseDetail?>(null)
      } else {
        val (verses, index) = req
        val ref = verses[index]
        combine(
          repository.originalVerse(ref.book, ref.chapter, ref.verse),
          repository.crossReferences(ref.book, ref.chapter, ref.verse),
          flow { emit(repository.verseInAllTranslations(ref.book, ref.chapter, ref.verse)) },
        ) { original, refs, versions: List<VerseEntity> ->
          VerseDetail(
            ref = ref,
            original = original,
            versions = versions,
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

  /**
   * True when at least one AI provider is configured and usable. Reattivo:
   * segue [SettingsRepository.aiConfig], così la chat compare anche nel
   * dettaglio già aperto dopo aver configurato un provider in Impostazioni
   * (stessa regola di AiGateway.chain, config.enabledChain()).
   */
  val aiConfigured: StateFlow<Boolean> =
    settingsRepository.aiConfig
      .map { it.enabledChain().isNotEmpty() }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        settingsRepository.aiConfig.value.enabledChain().isNotEmpty(),
      )

  /** Multi-turn chat with the configured AI about the verse. */
  suspend fun chat(system: String, messages: List<AiChatMessage>): String = aiGateway.chat(system, messages)
}
