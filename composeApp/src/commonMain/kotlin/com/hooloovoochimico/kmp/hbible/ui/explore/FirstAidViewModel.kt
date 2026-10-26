package com.hooloovoochimico.kmp.hbible.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.content.PackageFetcher
import com.hooloovoochimico.kmp.hbible.ui.common.passageText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Content package with the "Pronto soccorso" entries (written in the CMS). */
const val FIRST_AID_PACKAGE = "first_aid.json"

/** Content package with the "Temi" entries (written in the CMS). */
const val TOPICS_PACKAGE = "topics.json"

/** Sezione del CMS da cui arriva una voce: stesso formato, pacchetto e tabella diversi. */
enum class CuratedSection(val packageName: String) {
  FIRST_AID(FIRST_AID_PACKAGE),
  TOPICS(TOPICS_PACKAGE),
}

/** Voce di una sezione curata, ridotta ai campi mostrati nella pagina. */
data class CuratedEntry(
  val slug: String,
  val title: String,
  val subtitle: String,
  /** Verse references separated by "\n". */
  val verses: String,
  val body: String,
)

/** State of one "Pronto soccorso" or "Temi" page. */
data class FirstAidUiState(
  val slug: String = "",
  val entry: CuratedEntry? = null,
  /** Verses resolved against the books, first with references only, then with their text. */
  val verses: List<AiVerseMatch> = emptyList(),
  val loading: Boolean = true,
  /** The entry is not on this device (offline at first opening, or removed from the CMS). */
  val unavailable: Boolean = false,
)

/**
 * Pagina di una voce del "Pronto soccorso" o dei "Temi": riflessione scritta nel
 * CMS e versetti mostrati nella traduzione del lettore. Legato alla destinazione.
 */
class FirstAidViewModel(
  private val repository: BibleRepository,
  private val fetcher: PackageFetcher,
) : ViewModel() {

  private val stateInternal = MutableStateFlow(FirstAidUiState())
  val state: StateFlow<FirstAidUiState> = stateInternal

  private var job: Job? = null
  private var translation: String = ""
  private var section: CuratedSection = CuratedSection.FIRST_AID

  fun open(section: CuratedSection, slug: String, translation: String) {
    if (section == this.section && slug == stateInternal.value.slug &&
      translation == this.translation && job?.isActive == true
    ) {
      return
    }
    this.translation = translation
    this.section = section
    job?.cancel()
    stateInternal.value = FirstAidUiState(slug = slug)
    job =
      viewModelScope.launch {
        if (entry(section, slug).first() == null) fetcher.ensurePackage(section.packageName)
        entry(section, slug).collect { entry ->
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

  private fun entry(section: CuratedSection, slug: String): Flow<CuratedEntry?> =
    when (section) {
      CuratedSection.FIRST_AID ->
        repository.firstAid(slug).map { e -> e?.let { CuratedEntry(it.slug, it.title, it.subtitle, it.verses, it.body) } }
      CuratedSection.TOPICS ->
        repository.topic(slug).map { e -> e?.let { CuratedEntry(it.slug, it.title, it.subtitle, it.verses, it.body) } }
    }
}
