package com.hooloovoochimico.kmp.hbible.data.content

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Public content site (GitHub Pages of the app's public repo, branch `gh-pages`): the CMS publishes
 * `content/manifest.json` and the packages there with `npm run deploy:content` (task R02).
 */
const val DEFAULT_CONTENT_URL = "https://chemickypes.github.io/hbible"

/** Minimum time between two automatic checks at app open. */
const val AUTO_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

/** True when the automatic check at app open should run now. */
fun shouldAutoCheck(enabled: Boolean, nowMillis: Long, lastCheckMillis: Long): Boolean =
  enabled && (lastCheckMillis <= 0 || nowMillis - lastCheckMillis >= AUTO_CHECK_INTERVAL_MS)

/** True when these packages need a newer app (manifest `minApp` above our versionCode). */
fun ContentManifest.requiresNewerApp(appVersionCode: Int): Boolean = minApp > appVersionCode

/** Packages with an app-side consumer; unknown entries are skipped. */
fun ContentManifest.isSupported(path: String): Boolean =
  path.startsWith("bible/") ||
    (path.startsWith("originals/") && formats["originals"] == ORIGINALS_FORMAT) ||
    path == "lexicon.json" ||
    path == "crossrefs.json" ||
    path == "votd.json" ||
    path == "posts.json"

/** Import order: bibles first (they create the books), then originals, then the rest. */
private fun importRank(path: String): Int =
  when {
    path.startsWith("bible/") -> 0
    path.startsWith("originals/") -> 1
    else -> 2
  }

/**
 * Packages to download: [only] when given, otherwise those whose remote hash differs from the
 * locally applied one ([local]: path → hash); unsupported ones dropped, in import order.
 */
fun ContentManifest.packagesToApply(local: Map<String, String>, only: List<String>? = null): List<String> =
  (only ?: packages.entries.filter { (path, info) -> local[path] != info.hash }.map { it.key })
    .filter { it in packages && isSupported(it) }
    .sortedWith(compareBy({ importRank(it) }, { it }))

/** Downloads the manifest and the packages, verifying hashes and retrying network failures. */
class ContentDownloader(
  private val client: HttpClient,
  private val attempts: Int = 3,
  /** Delay before the 2nd attempt, doubled at every retry. */
  private val retryDelayMs: Long = 1_000,
) {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun manifest(base: String): ContentManifest =
    withRetry { json.decodeFromString<ContentManifest>(fetch("$base/content/manifest.json").body<String>()) }

  /** Package bytes; fails when the SHA-256 differs from [expectedHash] (e.g. a stale CDN copy). */
  suspend fun pkg(base: String, path: String, expectedHash: String?): ByteArray =
    withRetry {
      val bytes: ByteArray = fetch("$base/content/$path").body()
      check(expectedHash == null || Sha256.hexOf(bytes) == expectedHash) {
        "Hash del pacchetto non corrisponde: contenuto corrotto?"
      }
      bytes
    }

  private suspend fun fetch(url: String): HttpResponse {
    val response = client.get(url)
    check(response.status.isSuccess()) { "HTTP ${response.status.value} per $url" }
    return response
  }

  private suspend fun <T> withRetry(block: suspend () -> T): T {
    var wait = retryDelayMs
    repeat(attempts - 1) {
      try {
        return block()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Throwable) {
        delay(wait)
        wait *= 2
      }
    }
    return block()
  }
}
