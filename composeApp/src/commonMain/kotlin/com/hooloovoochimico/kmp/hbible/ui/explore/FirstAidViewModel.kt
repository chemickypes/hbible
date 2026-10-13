package com.hooloovoochimico.kmp.hbible.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.content.PackageFetcher
import com.hooloovoochimico.kmp.hbible.data.local.FirstAidEntity
import com.hooloovoochimico.kmp.hbible.ui.common.passageText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Content package with the "Pronto soccorso" entries (written in the CMS). */
const val FIRST_AID_PACKAGE = "first_aid.json"

/** State of one "Pronto soccorso" page. */
data class FirstAidUiState(
  val slug: String = "",
  val entry: FirstAidEntity? = null,
  /** Verses resolved against the books, first with references only, then with their text. */
  val verses: List<AiVerseMatch> = emptyList(),
  val loading: Boolean = true,
  /** The entry is not on this device (offline at first opening, or removed from the CMS). */
  val unavailable: Boolean = false,
)

/**
 * Pagina di una voce del "Pronto soccorso": riflessione scritta nel CMS e
 * versetti mostrati nella traduzione del lettore. Legato alla destinazione.
 */
class FirstAidViewModel(
  private val repository: BibleRepository,
  private val fetcher: PackageFetcher,
) : ViewModel() {

  private val stateInternal = MutableStateFlow(FirstAidUiState())
  val state: StateFlow<FirstAidUiState> = stateInternal

  private var job: Job? = null
  private var translation: String = ""

  fun open(slug: String, translation: String) {
    if (slug == stateInternal.value.slug && translation == this.translation && job?.isActive == true) return
    this.translation = translation
    job?.cancel()
    stateInternal.value = FirstAidUiState(slug = slug)
    job =
      viewModelScope.launch {
        if (repository.firstAid(slug).first() == null) fetcher.ensurePackage(FIRST_AID_PACKAGE)
        repository.firstAid(slug).collect { entry ->
          if (entry == null) {
            stateInternal.value = FirstAidUiState(slug = slug, loading = false, unavailable = true)
            return@collect
          }
          val books = repository.books().first()
          val refs = entry.verses.split('\n').mapNotNull { resolveAiMatch(it, books) }
          stateInternal.value = FirstAidUiState(slug = slug, entry = entry, verses = refs, loading = false)
          val withText = refs.map { it.copy(text = repository.passageText(translation, it)) }
          stateInternal.value = stateInternal.value.copy(verses = withText)
        }
      }
  }
}
