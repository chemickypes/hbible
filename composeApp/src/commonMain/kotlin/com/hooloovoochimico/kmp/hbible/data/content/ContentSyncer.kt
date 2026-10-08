package com.hooloovoochimico.kmp.hbible.data.content

import com.hooloovoochimico.kmp.hbible.appLog
import com.hooloovoochimico.kmp.hbible.data.BibleDoc
import com.hooloovoochimico.kmp.hbible.data.BookDto
import com.hooloovoochimico.kmp.hbible.data.CrossRefDoc
import com.hooloovoochimico.kmp.hbible.data.LexiconDoc
import com.hooloovoochimico.kmp.hbible.data.OriginalDoc
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ai.CmsAiSettings
import com.hooloovoochimico.kmp.hbible.data.local.BibleDao
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.ContentStateEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.local.TranslationMetaEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VotdEntity
import com.hooloovoochimico.kmp.hbible.data.local.withTransaction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `/content/votd.json`: overrides per date + rotation pool. */
@Serializable
data class VotdDoc(
  val overrides: Map<String, VotdEntryDto> = emptyMap(),
  val pool: List<VotdEntryDto> = emptyList(),
)

@Serializable
data class VotdEntryDto(
  val book: Int,
  val chapter: Int,
  val verse: Int,
  val translation: String? = null,
  val note: String? = null,
  val order: Int = 0,
)

/** `/content/posts.json`: curated feed items. */
@Serializable
data class PostsDoc(
  val posts: List<PostDto> = emptyList(),
)

/** Body of GET /api/ai/settings (CMS). */
@Serializable
data class CmsAiSettingsDto(
  val configured: Boolean = false,
  val provider: String = "",
  val token: String = "",
  val model: String = "",
)

@Serializable
data class PostDto(
  val slug: String,
  val title: String,
  val body: String,
  val book: Int? = null,
  val chapter: Int? = null,
  val verse: Int? = null,
  val publishedAt: String? = null,
)

/**
 * Result of GET /api/ai/settings from the CMS.
 * `reachable=false` → network error (keep local state); reachable with
 * `available=false` → the CMS publishes no default AI (drop local state).
 */
data class AiSettingsFetch(
  val reachable: Boolean,
  val available: Boolean,
  val settings: CmsAiSettings? = null,
)

/**
 * Downloads content packages from the CMS (`/content/…`) and applies them to
 * Room with replace semantics. The applied state lives in `content_state`
 * (manifest path → hash): a package is re-downloaded only when its remote
 * hash differs from the locally applied one.
 *
 * Supported packages (the same formats `ensureImported()` consumes):
 *  - `bible/{abbr}.json`   → verses (+ books on first import) + translation meta
 *  - `originals/{nn}.json` → original verses for that book (per-book replace)
 *  - `lexicon.json`        → full lexemes replace
 *  - `crossrefs.json`      → full cross-references replace
 *
 * Other packages (`book_info.json`, `votd.json`, `posts.json`) are recognized
 * but not yet applied — they ship with their app-side feature.
 */
class ContentSyncer(
  private val db: BibleDatabase,
  private val dao: BibleDao,
  private val settings: SettingsRepository,
  private val client: HttpClient,
) {
  private val json = Json { ignoreUnknownKeys = true }

  /** CMS base URL from settings, without trailing slash. Empty = sync disabled. */
  fun baseUrl(): String = settings.loadCmsBaseUrl().trim().trimEnd('/')

  fun isConfigured(): Boolean = baseUrl().isNotBlank()

  /** Fetches the manifest and diffs it against the locally applied packages. */
  suspend fun checkForUpdates(): UpdateCheck {
    val base = baseUrl()
    if (base.isBlank()) return UpdateCheck(null, emptyList(), "URL del CMS non configurato")
    return try {
      val manifest = fetchManifest(base)
      val local = dao.contentState().associate { it.packageId to it.hash }
      val changed =
        manifest.packages.entries
          .filter { (path, info) -> local[path] != info.hash }
          .map { it.key }
          .sorted()
      UpdateCheck(manifest.version, changed, null)
    } catch (e: CancellationException) {
      throw e
    } catch (t: Throwable) {
      appLog.w(t) { "Controllo aggiornamenti CMS fallito" }
      UpdateCheck(null, emptyList(), t.message ?: "Errore di rete")
    }
  }

  /** Plain-text fetch + manual parse: no ContentNegotiation needed on the client. */
  private suspend fun fetchManifest(base: String): ContentManifest {
    val text: String = client.get("$base/content/manifest.json").body()
    return json.decodeFromString<ContentManifest>(text)
  }

  /**
   * Fetches the CMS-published default AI ("casa madre" + token + modello).
   * Never throws: network failures yield `reachable=false` so callers keep
   * the last known state.
   */
  suspend fun fetchAiSettings(): AiSettingsFetch {
    val base = baseUrl()
    if (base.isBlank()) return AiSettingsFetch(reachable = false, available = false)
    return try {
      val text: String = client.get("$base/api/ai/settings").body()
      val dto = json.decodeFromString<CmsAiSettingsDto>(text)
      if (dto.configured && dto.token.isNotBlank() && dto.provider.isNotBlank()) {
        AiSettingsFetch(
          reachable = true,
          available = true,
          settings = CmsAiSettings(provider = dto.provider, apiKey = dto.token, model = dto.model),
        )
      } else {
        AiSettingsFetch(reachable = true, available = false)
      }
    } catch (t: Throwable) {
      if (t is CancellationException) throw t
      appLog.w(t) { "Impostazioni AI del CMS non raggiungibili" }
      AiSettingsFetch(reachable = false, available = false)
    }
  }

  /**
   * Downloads and applies the given packages (default: all changed ones), in
   * dependency order: bibles, originals, then single-file packages.
   */
  suspend fun sync(
    changed: List<String>? = null,
    onProgress: suspend (String) -> Unit = {},
  ): SyncResult =
    withContext(Dispatchers.Default) {
      val base = baseUrl()
      require(base.isNotBlank()) { "URL del CMS non configurato" }

      val manifest = fetchManifest(base)
      val local = dao.contentState().associate { it.packageId to it.hash }
      val todo =
        (changed ?: manifest.packages.entries.filter { (path, info) -> local[path] != info.hash }.map { it.key })
          .filter { isSupported(it) }
          .sortedWith(compareBy({ importRank(it) }, { it }))

      val applied = mutableListOf<String>()
      val failed = mutableMapOf<String, String>()
      todo.forEachIndexed { index, path ->
        onProgress("[${index + 1}/${todo.size}] $path")
        try {
          val bytes: ByteArray = client.get("$base/content/$path").body()
          verifyHash(bytes, manifest.packages[path]?.hash)
          importPackage(path, bytes, manifest.version)
          applied += path
        } catch (e: CancellationException) {
          throw e
        } catch (t: Throwable) {
          appLog.e(t) { "Sync pacchetto CMS fallito: $path" }
          failed[path] = t.message ?: "errore sconosciuto"
        }
      }
      SyncResult(manifest.version, applied, failed)
    }

  /** Packages with an app-side consumer; unknown entries are reported but skipped. */
  private fun isSupported(path: String): Boolean =
    path.startsWith("bible/") ||
      path.startsWith("originals/") ||
      path == "lexicon.json" ||
      path == "crossrefs.json" ||
      path == "votd.json" ||
      path == "posts.json"

  private fun importRank(path: String): Int =
    when {
      path.startsWith("bible/") -> 0
      path.startsWith("originals/") -> 1
      else -> 2
    }

  private suspend fun importPackage(path: String, bytes: ByteArray, version: String) {
    when {
      path.startsWith("bible/") -> importBible(bytes, version)
      path.startsWith("originals/") -> importOriginals(path, bytes, version)
      path == "lexicon.json" -> importLexicon(bytes, version)
      path == "crossrefs.json" -> importCrossrefs(bytes, version)
      path == "votd.json" -> importVotd(bytes, version)
      path == "posts.json" -> importPosts(bytes, version)
    }
  }

  private suspend fun importBible(bytes: ByteArray, version: String) {
    val doc = json.decodeFromString<BibleDoc>(bytes.decodeToString())
    val abbr = doc.meta.abbr
    db.withTransaction {
      // Shared canon: insert books once, from the first imported package.
      if (dao.bookCount() == 0) {
        dao.insertBooks(doc.books.map { BookEntity(it.n, it.name, it.abbr, it.chapters) })
      }
      // Replace semantics: re-import always rebuilds the translation's verses.
      dao.deleteVersesForTranslation(abbr)
      doc.verses.chunked(2000).forEach { chunk ->
        dao.insertVerses(
          chunk.map { VerseEntity(abbr, it.b, it.c, it.v, it.t, it.x) },
        )
      }
    }
    dao.insertTranslationMeta(
      TranslationMetaEntity(
        abbr = abbr,
        name = doc.meta.name,
        description = doc.meta.description,
        publisher = doc.meta.publisher,
        year = doc.meta.year,
        copyright = doc.meta.copyright,
      ),
    )
    dao.upsertContentState(ContentStateEntity("bible/$abbr.json", Sha256.hexOf(bytes), version, epochMillis()))
  }

  private suspend fun importOriginals(path: String, bytes: ByteArray, version: String) {
    val book = path.removePrefix("originals/").removeSuffix(".json").toIntOrNull()
      ?: throw IllegalArgumentException("Nome pacchetto originals non valido: '$path'")
    val doc = json.decodeFromString<OriginalDoc>(bytes.decodeToString())
    db.withTransaction {
      dao.deleteOriginalVersesForBook(book)
      doc.verses.chunked(2000).forEach { chunk ->
        dao.insertOriginalVerses(
          chunk.map {
            OriginalVerseEntity(
              book = it.b,
              chapter = it.c,
              verse = it.v,
              lang = it.lang,
              text = it.text,
              transliteration = it.tr,
              lemmas = it.lm,
              italianNr = it.anr.joinToString(","),
              italianR2 = it.ar2.joinToString(","),
              italianR27 = it.ar27.joinToString(","),
              italianDio = it.ar_dio.joinToString(","),
              italianNd = it.ar_nd.joinToString(","),
              italianCei = it.ar_cei.joinToString(","),
              italianRic = it.ar_ric.joinToString(","),
              italianMar = it.ar_mar.joinToString(","),
              glosses = it.ge.joinToString("\t"),
              glossesIt = it.gi.joinToString("\t"),
            )
          },
        )
      }
    }
    dao.upsertContentState(ContentStateEntity(path, Sha256.hexOf(bytes), version, epochMillis()))
  }

  private suspend fun importLexicon(bytes: ByteArray, version: String) {
    val doc = json.decodeFromString<LexiconDoc>(bytes.decodeToString())
    db.withTransaction {
      // Full replace: the CMS lexicon is complete, on-demand gloss_it entries
      // are re-derived from it (same policy as migration 10→11).
      dao.deleteAllLexemes()
      doc.he.entries.chunked(2000).forEach { chunk ->
        dao.insertLexemes(chunk.map { (number, l) -> LexemeEntity("he", number, l.tr, l.g, l.gi) })
      }
      doc.el.entries.chunked(2000).forEach { chunk ->
        dao.insertLexemes(chunk.map { (number, l) -> LexemeEntity("el", number, l.tr, l.g, l.gi) })
      }
    }
    dao.upsertContentState(ContentStateEntity("lexicon.json", Sha256.hexOf(bytes), version, epochMillis()))
  }

  private suspend fun importCrossrefs(bytes: ByteArray, version: String) {
    val doc = json.decodeFromString<CrossRefDoc>(bytes.decodeToString())
    db.withTransaction {
      dao.deleteAllCrossReferences()
      doc.refs.chunked(4000).forEach { chunk ->
        dao.insertCrossReferences(
          chunk.map {
            CrossReferenceEntity(it[0], it[1], it[2], it[3], it[4], it[5])
          },
        )
      }
    }
    dao.upsertContentState(ContentStateEntity("crossrefs.json", Sha256.hexOf(bytes), version, epochMillis()))
  }

  private suspend fun importVotd(bytes: ByteArray, version: String) {
    val doc = json.decodeFromString<VotdDoc>(bytes.decodeToString())
    val entries =
      buildList {
        doc.overrides.forEach { (date, e) ->
          add(VotdEntity(date = date, book = e.book, chapter = e.chapter, verse = e.verse, translation = e.translation, note = e.note))
        }
        doc.pool.forEach { e ->
          add(VotdEntity(date = null, book = e.book, chapter = e.chapter, verse = e.verse, translation = e.translation, note = e.note, orderIndex = e.order))
        }
      }
    db.withTransaction {
      dao.deleteAllVotd()
      dao.insertVotdEntries(entries)
    }
    dao.upsertContentState(ContentStateEntity("votd.json", Sha256.hexOf(bytes), version, epochMillis()))
  }

  private suspend fun importPosts(bytes: ByteArray, version: String) {
    val doc = json.decodeFromString<PostsDoc>(bytes.decodeToString())
    val entities =
      doc.posts.map { p ->
        PostEntity(
          slug = p.slug,
          title = p.title,
          body = p.body,
          book = p.book ?: 0,
          chapter = p.chapter ?: 0,
          verse = p.verse ?: 0,
          publishedAt = p.publishedAt,
        )
      }
    db.withTransaction {
      dao.deleteAllPosts()
      dao.insertPosts(entities)
    }
    dao.upsertContentState(ContentStateEntity("posts.json", Sha256.hexOf(bytes), version, epochMillis()))
  }

  /** Verifies the downloaded bytes against the manifest hash when provided. */
  private fun verifyHash(bytes: ByteArray, expected: String?) {
    if (expected != null && Sha256.hexOf(bytes) != expected) {
      throw IllegalStateException("Hash del pacchetto non corrisponde: contenuto corrotto?")
    }
  }

  private fun epochMillis(): Long = GMTDate().timestamp
}
