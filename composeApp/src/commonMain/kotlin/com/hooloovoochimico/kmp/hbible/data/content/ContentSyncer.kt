package com.hooloovoochimico.kmp.hbible.data.content

import com.hooloovoochimico.kmp.hbible.appLog
import com.hooloovoochimico.kmp.hbible.data.BibleDoc
import com.hooloovoochimico.kmp.hbible.data.BookDto
import com.hooloovoochimico.kmp.hbible.data.CrossRefDoc
import com.hooloovoochimico.kmp.hbible.data.LexiconDoc
import com.hooloovoochimico.kmp.hbible.data.OriginalDoc
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.TRANSLATION_NAMES
import com.hooloovoochimico.kmp.hbible.data.local.BibleDao
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.data.local.ContentStateEntity
import com.hooloovoochimico.kmp.hbible.data.local.CrossReferenceEntity
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalAlignmentEntity
import com.hooloovoochimico.kmp.hbible.data.local.OriginalVerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.local.TranslationMetaEntity
import com.hooloovoochimico.kmp.hbible.data.local.VerseEntity
import com.hooloovoochimico.kmp.hbible.data.local.VotdEntity
import com.hooloovoochimico.kmp.hbible.data.local.withTransaction
import io.ktor.client.HttpClient
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
  client: HttpClient,
  /** versionCode of this app, compared with the manifest's `minApp`. */
  private val appVersionCode: Int,
  /** Debug builds only: the content URL saved in Settings → Avanzate replaces the default. */
  private val allowUrlOverride: Boolean,
  private val downloader: ContentDownloader = ContentDownloader(client),
) {
  private val json = Json { ignoreUnknownKeys = true }

  /** Content site base URL (without trailing slash): [DEFAULT_CONTENT_URL] or the debug override. */
  fun baseUrl(): String {
    val saved = if (allowUrlOverride) settings.loadCmsBaseUrl().trim().trimEnd('/') else ""
    return saved.ifBlank { DEFAULT_CONTENT_URL }
  }

  fun isConfigured(): Boolean = baseUrl().isNotBlank()

  /** Fetches the manifest and diffs it against the locally applied packages. */
  suspend fun checkForUpdates(): UpdateCheck {
    val base = baseUrl()
    if (base.isBlank()) return UpdateCheck(null, emptyList(), "URL dei contenuti non configurato")
    return try {
      val manifest = downloader.manifest(base)
      if (manifest.requiresNewerApp(appVersionCode)) {
        return UpdateCheck(manifest.version, emptyList(), null, appUpdateRequired = true)
      }
      val local = dao.contentState().associate { it.packageId to it.hash }
      UpdateCheck(manifest.version, manifest.packagesToApply(local), null)
    } catch (e: CancellationException) {
      throw e
    } catch (t: Throwable) {
      appLog.w(t) { "Controllo aggiornamenti CMS fallito" }
      UpdateCheck(null, emptyList(), t.message ?: "Errore di rete")
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
      require(base.isNotBlank()) { "URL dei contenuti non configurato" }

      val manifest = downloader.manifest(base)
      if (manifest.requiresNewerApp(appVersionCode)) {
        return@withContext SyncResult(manifest.version, emptyList(), emptyMap(), appUpdateRequired = true)
      }
      val local = dao.contentState().associate { it.packageId to it.hash }
      // State is saved package by package: an interrupted run resumes from what is left.
      val todo = manifest.packagesToApply(local, changed)

      val applied = mutableListOf<String>()
      val failed = mutableMapOf<String, String>()
      for (path in manifest.removed) {
        try {
          if (removePackage(path)) applied += path
        } catch (e: CancellationException) {
          throw e
        } catch (t: Throwable) {
          appLog.e(t) { "Rimozione pacchetto CMS fallita: $path" }
          failed[path] = t.message ?: "errore sconosciuto"
        }
      }
      todo.forEachIndexed { index, path ->
        onProgress("[${index + 1}/${todo.size}] $path")
        try {
          val bytes = downloader.pkg(base, path, manifest.packages[path]?.hash)
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

  /**
   * Applies a package removed from the CMS. Only translations are removable:
   * their verses, metadata and sync state are deleted. Returns false when
   * there was nothing to remove.
   */
  private suspend fun removePackage(path: String): Boolean {
    if (!path.startsWith("bible/")) return false
    val abbr = path.removePrefix("bible/").removeSuffix(".json")
    if (abbr in TRANSLATION_NAMES) return false // bundled: never removed by a sync
    if (dao.translationVerseCount(abbr) == 0 && dao.contentStateFor(path) == null) return false
    db.withTransaction {
      dao.deleteVersesForTranslation(abbr)
      dao.deleteTranslationMeta(abbr)
      dao.deleteContentState(path)
    }
    return true
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
          chunk.map { VerseEntity(abbr, it.b, it.c, it.v, it.t, it.x, paragraph = it.p == 1) },
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
        license = doc.meta.license,
        licenseUrl = doc.meta.license_url,
        sourceUrl = doc.meta.source_url,
        attribution = doc.meta.attribution,
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
      dao.deleteAlignmentsForBook(book)
      dao.insertAlignments(
        doc.verses.flatMap { v ->
          v.al.map { (abbr, indices) -> OriginalAlignmentEntity(v.b, v.c, v.v, abbr, indices.joinToString(",")) }
        },
      )
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
  private fun epochMillis(): Long = GMTDate().timestamp
}
