package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiSettingsStore
import com.hooloovoochimico.kmp.hbible.platform.SecretStore
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

fun ThemeMode.label(): String =
  when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Chiaro"
    ThemeMode.DARK -> "Scuro"
  }

/** Reading text size for Bible verses. */
enum class ReaderFontSize(val fontSizeSp: Float, val lineHeightSp: Float) {
  SMALL(15f, 22f),
  NORMAL(17f, 25f),
  LARGE(20f, 29f),
}

fun ReaderFontSize.label(): String =
  when (this) {
    ReaderFontSize.SMALL -> "Piccolo"
    ReaderFontSize.NORMAL -> "Normale"
    ReaderFontSize.LARGE -> "Grande"
  }

/** Last reading position persisted between sessions. */
data class LastPosition(val translation: String, val book: Int, val chapter: Int)

/** Libro letto di recente con l'ultimo capitolo aperto (foglio "Scegli un libro"). */
data class RecentBook(val book: Int, val chapter: Int)

/** Libri recenti tenuti in memoria (il foglio ne mostra i primi, escluso quello in lettura). */
const val RECENT_BOOKS_MAX = 6

/**
 * Porta [book] in testa ai recenti con il capitolo [chapter]; un libro compare una volta
 * sola e la lista non supera [max] voci.
 */
fun updateRecentBooks(
  recents: List<RecentBook>,
  book: Int,
  chapter: Int,
  max: Int = RECENT_BOOKS_MAX,
): List<RecentBook> = (listOf(RecentBook(book, chapter)) + recents.filterNot { it.book == book }).take(max)

/** Last position of the interlinear tab (with verse), persisted between sessions. */
data class InterlinearPosition(val book: Int, val chapter: Int, val verse: Int)

/** User preferences: theme, reading state and AI provider configuration. */
interface SettingsRepository {
  fun loadThemeMode(): ThemeMode

  fun saveThemeMode(mode: ThemeMode)

  fun loadDynamicColor(): Boolean

  fun saveDynamicColor(enabled: Boolean)

  fun loadFontSize(): ReaderFontSize

  fun saveFontSize(size: ReaderFontSize)

  fun loadLastPosition(): LastPosition

  fun saveLastPosition(position: LastPosition)

  fun loadRecentBooks(): List<RecentBook>

  fun saveRecentBooks(recents: List<RecentBook>)

  fun loadInterlinearPosition(): InterlinearPosition

  fun saveInterlinearPosition(position: InterlinearPosition)

  // --- Content sync (CMS) ---

  /** CMS base URL, e.g. "http://192.168.1.10:3001". Empty = sync disabled. */
  fun loadCmsBaseUrl(): String

  fun saveCmsBaseUrl(url: String)

  /** When true, updates are checked (and downloaded) on app open. */
  fun loadAutoUpdateCheck(): Boolean

  fun saveAutoUpdateCheck(enabled: Boolean)

  /** Time of the last successful content check (epoch millis, 0 = never). */
  fun loadLastContentCheck(): Long

  fun saveLastContentCheck(epochMillis: Long)

  // --- AI configuration ---

  /**
   * AI configuration as a reactive stream: emits again after every
   * [saveAiConfig], so the UI can react while a screen is already open.
   */
  val aiConfig: StateFlow<AiConfig>

  fun loadAiConfig(): AiConfig

  fun saveAiConfig(config: AiConfig)

  /** True when at least one AI provider is enabled with a key set. */
  fun isAiConfigured(): Boolean
}

class DefaultSettingsRepository(
  /** File "settings" (tema, lettura, ultima posizione), come nel sorgente. */
  private val settings: Settings,
  /** File "ai_settings" (chiavi legacy della config AI), come nel sorgente. */
  private val aiSettings: Settings,
  /** Storage cifrato per il JSON di config AI (Android Keystore / iOS Keychain). */
  private val secretStore: SecretStore,
) : SettingsRepository {

  private val aiConfigState = MutableStateFlow(AiSettingsStore.load(secretStore, aiSettings))
  override val aiConfig: StateFlow<AiConfig> = aiConfigState

  override fun loadThemeMode(): ThemeMode = ThemePreferences.load(settings)

  override fun saveThemeMode(mode: ThemeMode) {
    ThemePreferences.save(settings, mode)
  }

  override fun loadDynamicColor(): Boolean = ThemePreferences.loadDynamicColor(settings)

  override fun saveDynamicColor(enabled: Boolean) {
    ThemePreferences.saveDynamicColor(settings, enabled)
  }

  override fun loadFontSize(): ReaderFontSize = ThemePreferences.loadFontSize(settings)

  override fun saveFontSize(size: ReaderFontSize) {
    ThemePreferences.saveFontSize(settings, size)
  }

  override fun loadLastPosition(): LastPosition = ThemePreferences.loadLastPosition(settings)

  override fun saveLastPosition(position: LastPosition) {
    ThemePreferences.saveLastPosition(settings, position)
  }

  override fun loadRecentBooks(): List<RecentBook> = ThemePreferences.loadRecentBooks(settings)

  override fun saveRecentBooks(recents: List<RecentBook>) {
    ThemePreferences.saveRecentBooks(settings, recents)
  }

  override fun loadInterlinearPosition(): InterlinearPosition =
    ThemePreferences.loadInterlinearPosition(settings)

  override fun saveInterlinearPosition(position: InterlinearPosition) {
    ThemePreferences.saveInterlinearPosition(settings, position)
  }

  override fun loadCmsBaseUrl(): String = ThemePreferences.loadCmsBaseUrl(settings)

  override fun saveCmsBaseUrl(url: String) {
    ThemePreferences.saveCmsBaseUrl(settings, url)
  }

  override fun loadAutoUpdateCheck(): Boolean = ThemePreferences.loadAutoUpdateCheck(settings)

  override fun saveAutoUpdateCheck(enabled: Boolean) {
    ThemePreferences.saveAutoUpdateCheck(settings, enabled)
  }

  override fun loadLastContentCheck(): Long = ThemePreferences.loadLastContentCheck(settings)

  override fun saveLastContentCheck(epochMillis: Long) {
    ThemePreferences.saveLastContentCheck(settings, epochMillis)
  }

  override fun loadAiConfig(): AiConfig = AiSettingsStore.load(secretStore, aiSettings)

  override fun saveAiConfig(config: AiConfig) {
    AiSettingsStore.save(secretStore, config)
    aiConfigState.value = loadAiConfig()
  }

  override fun isAiConfigured(): Boolean = AiSettingsStore.isConfigured(secretStore, aiSettings)
}

/** Theme and reader preferences (file "settings", come nel sorgente). */
object ThemePreferences {
  const val PREFS = "settings"
  private const val KEY = "theme_mode"
  // Chiave nuova (v2, 2026-10-10): con la palette "Pergamena e oro" i colori dinamici
  // partono spenti per tutti, anche per chi li aveva attivi col default precedente.
  private const val DYNAMIC_COLOR_KEY = "dynamic_color_v2"
  private const val FONT_SIZE_KEY = "reader_font_size"
  private const val LAST_TRANSLATION_KEY = "last_translation"
  private const val LAST_BOOK_KEY = "last_book"
  private const val LAST_CHAPTER_KEY = "last_chapter"
  private const val RECENT_BOOKS_KEY = "recent_books"
  private const val LAST_INTERLINEAR_BOOK_KEY = "last_interlinear_book"
  private const val LAST_INTERLINEAR_CHAPTER_KEY = "last_interlinear_chapter"
  private const val LAST_INTERLINEAR_VERSE_KEY = "last_interlinear_verse"
  private const val CMS_BASE_URL_KEY = "cms_base_url"
  private const val AUTO_UPDATE_CHECK_KEY = "auto_update_check"
  private const val LAST_CONTENT_CHECK_KEY = "last_content_check"

  fun load(settings: Settings): ThemeMode {
    val name = settings.getStringOrNull(KEY)
    return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
  }

  fun save(settings: Settings, mode: ThemeMode) {
    settings.putString(KEY, mode.name)
  }

  fun loadDynamicColor(settings: Settings): Boolean =
    settings.getBoolean(DYNAMIC_COLOR_KEY, false)

  fun saveDynamicColor(settings: Settings, enabled: Boolean) {
    settings.putBoolean(DYNAMIC_COLOR_KEY, enabled)
  }

  fun loadFontSize(settings: Settings): ReaderFontSize {
    val name = settings.getStringOrNull(FONT_SIZE_KEY)
    return ReaderFontSize.entries.firstOrNull { it.name == name } ?: ReaderFontSize.NORMAL
  }

  fun saveFontSize(settings: Settings, size: ReaderFontSize) {
    settings.putString(FONT_SIZE_KEY, size.name)
  }

  fun loadLastPosition(settings: Settings): LastPosition =
    LastPosition(
      translation = bundledTranslationOrDefault(settings.getStringOrNull(LAST_TRANSLATION_KEY)),
      book = settings.getInt(LAST_BOOK_KEY, 1).coerceIn(1, 66),
      chapter = settings.getInt(LAST_CHAPTER_KEY, 1).coerceAtLeast(1),
    )

  fun saveLastPosition(settings: Settings, position: LastPosition) {
    settings.putString(LAST_TRANSLATION_KEY, position.translation)
    settings.putInt(LAST_BOOK_KEY, position.book)
    settings.putInt(LAST_CHAPTER_KEY, position.chapter)
  }

  /** Formato "libro:capitolo" separati da virgola ("43:3,19:23"); voci illeggibili scartate. */
  fun loadRecentBooks(settings: Settings): List<RecentBook> =
    settings.getStringOrNull(RECENT_BOOKS_KEY).orEmpty()
      .split(',')
      .mapNotNull { entry ->
        val parts = entry.split(':')
        val book = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..66 }
        val chapter = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 1 }
        if (book != null && chapter != null) RecentBook(book, chapter) else null
      }
      .distinctBy { it.book }
      .take(RECENT_BOOKS_MAX)

  fun saveRecentBooks(settings: Settings, recents: List<RecentBook>) {
    settings.putString(RECENT_BOOKS_KEY, recents.joinToString(",") { "${it.book}:${it.chapter}" })
  }

  fun loadInterlinearPosition(settings: Settings): InterlinearPosition =
    InterlinearPosition(
      book = settings.getInt(LAST_INTERLINEAR_BOOK_KEY, 1).coerceIn(1, 66),
      chapter = settings.getInt(LAST_INTERLINEAR_CHAPTER_KEY, 1).coerceAtLeast(1),
      verse = settings.getInt(LAST_INTERLINEAR_VERSE_KEY, 1).coerceAtLeast(1),
    )

  fun saveInterlinearPosition(settings: Settings, position: InterlinearPosition) {
    settings.putInt(LAST_INTERLINEAR_BOOK_KEY, position.book)
    settings.putInt(LAST_INTERLINEAR_CHAPTER_KEY, position.chapter)
    settings.putInt(LAST_INTERLINEAR_VERSE_KEY, position.verse)
  }

  fun loadCmsBaseUrl(settings: Settings): String = settings.getStringOrNull(CMS_BASE_URL_KEY) ?: ""

  fun saveCmsBaseUrl(settings: Settings, url: String) {
    settings.putString(CMS_BASE_URL_KEY, url)
  }

  fun loadAutoUpdateCheck(settings: Settings): Boolean =
    settings.getBoolean(AUTO_UPDATE_CHECK_KEY, true)

  fun saveAutoUpdateCheck(settings: Settings, enabled: Boolean) {
    settings.putBoolean(AUTO_UPDATE_CHECK_KEY, enabled)
  }

  fun loadLastContentCheck(settings: Settings): Long = settings.getLong(LAST_CONTENT_CHECK_KEY, 0L)

  fun saveLastContentCheck(settings: Settings, epochMillis: Long) {
    settings.putLong(LAST_CONTENT_CHECK_KEY, epochMillis)
  }
}
