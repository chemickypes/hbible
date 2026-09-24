package com.hooloovoochimico.kmp.hbible.data

import androidx.room.RoomRawQuery
import com.hooloovoochimico.kmp.hbible.data.ai.AiMatches
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Search scope filter: optional restriction to a book range. */
enum class SearchScope(val label: String, val books: IntRange?) {
  ALL("Tutta la Bibbia", null),
  AT("AT", 1..39),
  NT("NT", 40..66),
  VANGELI("Vangeli", 40..43),
  LETTERE("Lettere", 44..66),
  STORICI("Libri Storici", 6..16),
  PROFETI("Profeti", 23..39),
  SALMI("Salmi", 17..22),
  PENTATEUCO("Pentateuco", 1..5),
}

/** A past search kept in history; AI entries cache their results (no new tokens). */
@Serializable
data class SearchHistoryEntry(
  val query: String,
  val aiMode: Boolean,
  val matches: AiMatches? = null,
  val timestamp: Long,
)

/** Last AI search outcome: either matches or an error, tied to its query. */
data class SavedAiSearch(val query: String, val matches: AiMatches?, val error: String?)

/** Explore (search) data: full-text verse search plus persisted search state. */
interface ExploreRepository {
  /** Case-insensitive all-words search in the given translation and scope. */
  suspend fun search(translation: String, query: String, scope: SearchScope): List<VerseEntity>

  // --- Persisted search state (last query, scope, AI mode) ---

  fun loadLastQuery(): String

  fun loadLastScope(): SearchScope

  fun saveLastQuery(query: String, scope: SearchScope)

  fun loadAiMode(): Boolean

  fun saveAiMode(mode: Boolean)

  /** Last AI search, so the screen can restore query and outcome together. */
  fun loadSavedAi(): SavedAiSearch?

  fun saveAi(query: String, matches: AiMatches?, error: String?)

  // --- Search history ---

  fun history(): List<SearchHistoryEntry>

  fun addHistory(entry: SearchHistoryEntry)

  /** Previous AI results for the same query, so no new API call is needed. */
  fun findAiCached(query: String): SearchHistoryEntry?

  fun clearHistory()
}

class DefaultExploreRepository(
  private val db: BibleDatabase,
  /** File "settings" (ultime ricerche), come nel sorgente. */
  private val settings: Settings,
  /** File "search_history" (cronologia), come nel sorgente. */
  private val historySettings: Settings,
) : ExploreRepository {

  override suspend fun search(
    translation: String,
    query: String,
    scope: SearchScope,
  ): List<VerseEntity> {
    val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    val escaped =
      words.map {
        it.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
      }
    val likeClause =
      escaped.joinToString(" AND ") { "text LIKE '%' || ? || '%' ESCAPE '\\'" }
    val scopeClause =
      when (val range = scope.books) {
        null -> "1=1"
        else -> "book BETWEEN ${range.first} AND ${range.last}"
      }
    val sql =
      "SELECT * FROM verses WHERE translation = ? AND ($likeClause) AND $scopeClause " +
        "ORDER BY book, chapter, verse LIMIT 300"
    // Ricerca con @RawQuery KMP (RoomRawQuery, PLAN §12): SQL e binding
    // posizionale identici al sorgente (SimpleSQLiteQuery con args).
    val args = listOf<Any?>(translation) + escaped
    return db.bibleDao().searchRaw(
      RoomRawQuery(sql) { statement ->
        args.forEachIndexed { index, arg -> statement.bindText(index + 1, arg as String) }
      },
    )
  }

  override fun loadLastQuery(): String = SearchPreferences.loadQuery(settings)

  override fun loadLastScope(): SearchScope = SearchPreferences.loadScope(settings)

  override fun saveLastQuery(query: String, scope: SearchScope) {
    SearchPreferences.save(settings, query, scope)
  }

  override fun loadAiMode(): Boolean = SearchPreferences.loadAiMode(settings)

  override fun saveAiMode(mode: Boolean) {
    SearchPreferences.saveAiMode(settings, mode)
  }

  override fun loadSavedAi(): SavedAiSearch? = SearchPreferences.loadSavedAi(settings)

  override fun saveAi(query: String, matches: AiMatches?, error: String?) {
    SearchPreferences.saveAi(settings, query, matches, error)
  }

  override fun history(): List<SearchHistoryEntry> = SearchHistory.load(historySettings)

  override fun addHistory(entry: SearchHistoryEntry) {
    SearchHistory.add(historySettings, entry)
  }

  override fun findAiCached(query: String): SearchHistoryEntry? = SearchHistory.findAiCached(historySettings, query)

  override fun clearHistory() {
    SearchHistory.clear(historySettings)
  }
}

/** Storage for the last performed search (file "settings", come nel sorgente). */
object SearchPreferences {
  const val PREFS = "settings"
  private const val KEY_QUERY = "last_search_query"
  private const val KEY_SCOPE = "last_search_scope"
  private const val KEY_AI_MODE = "last_search_ai_mode"
  private const val KEY_AI_QUERY = "last_search_ai_query"
  private const val KEY_AI_MATCHES = "last_search_ai_matches"
  private const val KEY_AI_ERROR = "last_search_ai_error"

  private val json = Json { ignoreUnknownKeys = true }

  fun loadQuery(settings: Settings): String =
    settings.getString(KEY_QUERY, "").orEmpty()

  fun loadScope(settings: Settings): SearchScope {
    val name = settings.getStringOrNull(KEY_SCOPE)
    return SearchScope.entries.firstOrNull { it.name == name } ?: SearchScope.ALL
  }

  fun save(settings: Settings, query: String, scope: SearchScope) {
    settings.putString(KEY_QUERY, query)
    settings.putString(KEY_SCOPE, scope.name)
  }

  fun loadAiMode(settings: Settings): Boolean = settings.getBoolean(KEY_AI_MODE, false)

  fun saveAiMode(settings: Settings, mode: Boolean) {
    settings.putBoolean(KEY_AI_MODE, mode)
  }

  /** Last AI search, so the screen can restore query and outcome together. */
  fun loadSavedAi(settings: Settings): SavedAiSearch? {
    val query = settings.getStringOrNull(KEY_AI_QUERY) ?: return null
    val error = settings.getStringOrNull(KEY_AI_ERROR)
    val matches =
      settings.getStringOrNull(KEY_AI_MATCHES)?.let { raw ->
        try {
          json.decodeFromString<AiMatches>(raw)
        } catch (e: IllegalArgumentException) {
          null
        }
      }
    if (matches == null && error == null) return null
    return SavedAiSearch(query, matches, error)
  }

  fun saveAi(settings: Settings, query: String, matches: AiMatches?, error: String?) {
    if (matches == null && error == null) {
      settings.remove(KEY_AI_QUERY)
      settings.remove(KEY_AI_MATCHES)
      settings.remove(KEY_AI_ERROR)
    } else {
      settings.putString(KEY_AI_QUERY, query)
      if (matches != null) settings.putString(KEY_AI_MATCHES, json.encodeToString(matches))
      if (error != null) settings.putString(KEY_AI_ERROR, error)
    }
  }
}

/** Bounded list of past searches, persisted as JSON in preferences (file "search_history"). */
object SearchHistory {
  const val PREFS = "search_history"
  private const val KEY = "entries"
  private const val MAX = 30
  private val json = Json { ignoreUnknownKeys = true }

  fun load(settings: Settings): List<SearchHistoryEntry> =
    try {
      json.decodeFromString<List<SearchHistoryEntry>>(
        settings.getString(KEY, "[]").orEmpty(),
      )
    } catch (e: IllegalArgumentException) {
      emptyList()
    }

  fun add(settings: Settings, entry: SearchHistoryEntry) {
    val rest =
      load(settings).filterNot {
        it.aiMode == entry.aiMode && it.query.trim().lowercase() == entry.query.trim().lowercase()
      }
    settings.putString(KEY, json.encodeToString((listOf(entry) + rest).take(MAX)))
  }

  /** Previous AI results for the same query, so no new API call is needed. */
  fun findAiCached(settings: Settings, query: String): SearchHistoryEntry? {
    val norm = query.trim().lowercase()
    return load(settings).firstOrNull { it.aiMode && it.matches != null && it.query.trim().lowercase() == norm }
  }

  fun clear(settings: Settings) {
    settings.remove(KEY)
  }
}
