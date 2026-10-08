package com.hooloovoochimico.kmp.hbible.ui.interlineare

import com.hooloovoochimico.kmp.hbible.data.DEFAULT_TRANSLATION
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.InterlinearPosition
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_META
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whole state of the interlinear tab: one verse with its original words. */
data class InterlineareUiState(
  val books: List<BookEntity> = emptyList(),
  val position: InterlinearPosition = InterlinearPosition(1, 1, 1),
  val translation: String = DEFAULT_TRANSLATION,
  val original: OriginalVerseEntity? = null,
  /** Alignment of [original] against [translation] (0-based token indices, -1 = none). */
  val alignment: List<Int> = emptyList(),
  val verse: VerseEntity? = null,
  /** The same verse in every bundled translation, in TRANSLATION_META order. */
  val versions: List<VerseEntity> = emptyList(),
  /** Short Italian lexicon glosses for the verse's Strong numbers ("h7971" -> "mandare via"). */
  val lexiconGlosses: Map<String, String> = emptyMap(),
  /** Number of the last verse of the open chapter (0 while unknown). */
  val chapterMaxVerse: Int = 0,
  val hasPrev: Boolean = false,
  val hasNext: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InterlineareViewModel(
  private val repository: BibleRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  private val position =
    MutableStateFlow(settingsRepository.loadInterlinearPosition())
  private val translation = MutableStateFlow(DEFAULT_TRANSLATION)
  private val books =
    repository.books().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    // Persist the position whenever the user moves to another verse.
    viewModelScope.launch {
      position.drop(1).collect { settingsRepository.saveInterlinearPosition(it) }
    }
  }

  /** Follows the translation selected in the reader (the shell keeps them in sync). */
  fun setTranslation(value: String) {
    if (translation.value != value) translation.value = value
  }

  val uiState: StateFlow<InterlineareUiState> =
    combine(books, position, translation) { b, pos, tr -> Triple(b, pos, tr) }
      .flatMapLatest { (b, pos, tr) ->
        combine(
          repository.originalVerse(pos.book, pos.chapter, pos.verse),
          repository.chapter(tr, pos.book, pos.chapter),
          flow { emit(repository.verse(tr, pos.book, pos.chapter, pos.verse)) },
          flow { emit(verseInAllTranslations(pos.book, pos.chapter, pos.verse)) },
          repository.alignments(pos.book, pos.chapter, pos.verse),
        ) { original, chapterVerses, verse, versions, alignments ->
          val book = b.firstOrNull { it.n == pos.book }
          val chapterMax = chapterVerses.maxOfOrNull { it.verse } ?: 0
          InterlineareUiState(
            books = b,
            position = pos,
            translation = tr,
            original = original,
            alignment = alignments[tr].orEmpty(),
            verse = verse,
            versions = versions,
            lexiconGlosses = lexiconGlosses(original),
            chapterMaxVerse = chapterMax,
            hasPrev = pos.verse > 1 || pos.chapter > 1 || pos.book > 1,
            hasNext =
              pos.verse < chapterMax ||
                (book != null && pos.chapter < book.chapters) ||
                pos.book < (b.lastOrNull()?.n ?: pos.book),
          )
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InterlineareUiState())

  /** Jumps to the given verse (picker result or external navigation), clamped to valid bounds. */
  fun moveTo(book: Int, chapter: Int, verse: Int) {
    val clampedBook = book.coerceIn(1, 66)
    val bookMeta = books.value.firstOrNull { it.n == clampedBook }
    viewModelScope.launch {
      val clampedChapter =
        chapter.coerceAtLeast(1).let { c -> bookMeta?.let { c.coerceAtMost(it.chapters) } ?: c }
      val maxVerse = lastVerseOf(clampedBook, clampedChapter)
      position.value = InterlinearPosition(clampedBook, clampedChapter, verse.coerceIn(1, maxVerse))
    }
  }

  /** Previous verse, crossing chapter and book boundaries. */
  fun prev() {
    val state = uiState.value
    if (!state.hasPrev) return
    val pos = state.position
    if (pos.verse > 1) {
      position.value = pos.copy(verse = pos.verse - 1)
      return
    }
    viewModelScope.launch {
      val target =
        when {
          pos.chapter > 1 ->
            InterlinearPosition(pos.book, pos.chapter - 1, lastVerseOf(pos.book, pos.chapter - 1))
          else -> {
            val previous = state.books.firstOrNull { it.n == pos.book - 1 } ?: return@launch
            InterlinearPosition(previous.n, previous.chapters, lastVerseOf(previous.n, previous.chapters))
          }
        }
      position.value = target
    }
  }

  /** Next verse, crossing chapter and book boundaries. */
  fun next() {
    val state = uiState.value
    if (!state.hasNext) return
    val pos = state.position
    if (pos.verse < state.chapterMaxVerse) {
      position.value = pos.copy(verse = pos.verse + 1)
      return
    }
    val book = state.books.firstOrNull { it.n == pos.book } ?: return
    if (pos.chapter < book.chapters) {
      position.value = pos.copy(chapter = pos.chapter + 1, verse = 1)
      return
    }
    val following = state.books.firstOrNull { it.n == pos.book + 1 } ?: return
    position.value = InterlinearPosition(following.n, 1, 1)
  }

  /** Verses of a chapter, used by the reference picker. */
  suspend fun versesOf(book: Int, chapter: Int): List<VerseEntity> =
    repository.chapter(translation.value, book, chapter).first().sortedBy { it.verse }

  /** Short lexicon glosses keyed by "h7971"/"g3056" for every Strong number of the verse. */
  private suspend fun lexiconGlosses(original: OriginalVerseEntity?): Map<String, String> {
    if (original == null) return emptyMap()
    val numbers =
      original.lemmas.split(" ").filter { it.isNotBlank() && it != "-" }.distinct()
    return numbers.mapNotNull { n ->
      repository.lexeme(original.lang, n)?.takeIf { it.glossIt.isNotBlank() }?.let {
        original.lang + n to shortGloss(it.glossIt)
      }
    }.toMap()
  }

  /** First meaningful section of the full definition: "alzarsi (in varie...)" -> "alzarsi". */
  private fun shortGloss(gloss: String): String {
    val bySemicolon = gloss.substringBefore(";").trim()
    val byComma = bySemicolon.substringBefore(", ").trim()
    val byParen = byComma.substringBefore(" (").trim()
    return listOf(byParen, byComma, bySemicolon, gloss).first { it.isNotBlank() }
  }

  /** The verse in every bundled translation that has it, in TRANSLATION_META display order. */
  private suspend fun verseInAllTranslations(book: Int, chapter: Int, verse: Int): List<VerseEntity> {
    val byTranslation =
      repository.verseAllTranslations(book, chapter, verse).associateBy { it.translation }
    return TRANSLATION_META.mapNotNull { meta -> byTranslation[meta.abbr] }
  }

  private suspend fun lastVerseOf(book: Int, chapter: Int): Int =
    repository.chapter(translation.value, book, chapter).first().maxOfOrNull { it.verse } ?: 1
}
