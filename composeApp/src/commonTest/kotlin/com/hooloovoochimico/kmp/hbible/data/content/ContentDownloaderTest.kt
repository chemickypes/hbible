package com.hooloovoochimico.kmp.hbible.data.content

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentDownloaderTest {
  private val base = "https://example.org/hbible"
  private val otb = "OTB v2".encodeToByteArray()
  private val otbHash = Sha256.hexOf(otb)

  private val manifest =
    ContentManifest(
      version = "20261009T120000",
      formats = mapOf("originals" to ORIGINALS_FORMAT),
      minApp = 2,
      packages =
        mapOf(
          "bible/OTB.json" to PackageInfo(otbHash),
          "originals/01.json" to PackageInfo("o1"),
          "lexicon.json" to PackageInfo("lx"),
          "book_info.json" to PackageInfo("bi"),
        ),
    )

  @Test
  fun samePackagesLocally_nothingToApply() {
    val local = manifest.packages.mapValues { it.value.hash }
    assertEquals(emptyList(), manifest.packagesToApply(local))
  }

  @Test
  fun onlyChangedSupportedPackages_inImportOrder() {
    val local = mapOf("bible/OTB.json" to "old", "originals/01.json" to "o1")
    // book_info.json has no app-side consumer: never downloaded.
    assertEquals(listOf("bible/OTB.json", "lexicon.json"), manifest.packagesToApply(local))
  }

  @Test
  fun unknownOriginalsFormat_isSkipped() {
    val newer = manifest.copy(formats = mapOf("originals" to ORIGINALS_FORMAT + 1))
    assertFalse(newer.isSupported("originals/01.json"))
  }

  @Test
  fun minApp_aboveOurVersion_requiresNewerApp() {
    assertTrue(manifest.requiresNewerApp(appVersionCode = 1))
    assertFalse(manifest.requiresNewerApp(appVersionCode = 2))
  }

  @Test
  fun autoCheck_atMostOncePerDay() {
    val day = AUTO_CHECK_INTERVAL_MS
    assertTrue(shouldAutoCheck(enabled = true, nowMillis = day, lastCheckMillis = 0))
    assertFalse(shouldAutoCheck(enabled = true, nowMillis = day + 10, lastCheckMillis = 20))
    assertTrue(shouldAutoCheck(enabled = true, nowMillis = 2 * day + 20, lastCheckMillis = 20))
    assertFalse(shouldAutoCheck(enabled = false, nowMillis = 2 * day, lastCheckMillis = 0))
  }

  @Test
  fun manifest_isReadFromContentFolder() = runTest {
    val engine = MockEngine { request ->
      assertEquals("$base/content/manifest.json", request.url.toString())
      respond("""{"version":"v1","minApp":3,"packages":{"lexicon.json":{"hash":"h"}}}""")
    }
    val m = ContentDownloader(HttpClient(engine), retryDelayMs = 0).manifest(base)
    assertEquals("v1", m.version)
    assertEquals(3, m.minApp)
  }

  @Test
  fun networkErrors_areRetried() = runTest {
    var calls = 0
    val engine = MockEngine {
      calls++
      if (calls < 3) respond("", HttpStatusCode.ServiceUnavailable) else respond(otb)
    }
    val bytes = ContentDownloader(HttpClient(engine), retryDelayMs = 0).pkg(base, "bible/OTB.json", otbHash)
    assertContentEquals(otb, bytes)
    assertEquals(3, calls)
  }

  @Test
  fun wrongHash_isRejectedAfterRetries() = runTest {
    var calls = 0
    val engine = MockEngine {
      calls++
      respond("corrotto")
    }
    assertFailsWith<IllegalStateException> {
      ContentDownloader(HttpClient(engine), retryDelayMs = 0).pkg(base, "bible/OTB.json", otbHash)
    }
    assertEquals(3, calls)
  }
}
