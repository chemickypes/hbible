package com.hooloovoochimico.kmp.hbible.data.content

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pacchetto `topics.json` ("Temi"): stesso formato di `first_aid.json`, ma può
 * mancare dal manifest del CMS (o restituire 404): in quel caso non deve
 * succedere nulla di grave, la sezione semplicemente non compare.
 */
class TopicsPackageTest {
  private val base = "https://example.org/hbible"

  private val topicsJson =
    """
    {
      "entries": [
        {
          "slug": "preghiera",
          "title": "La preghiera",
          "subtitle": "Parlare con Dio",
          "verses": ["Fil 4:6-7", "Mt 6:9"],
          "body": "Prima riflessione.\n\nSeconda riflessione.",
          "campo_futuro": 42
        },
        { "slug": "fede", "title": "La fede", "verses": ["Eb 11:1"] }
      ]
    }
    """.trimIndent()

  @Test
  fun decodesEntriesInOrder_ignoringUnknownFields() {
    val doc = decodeCuratedDoc(topicsJson.encodeToByteArray())
    val entities = doc.toTopicEntities()
    assertEquals(listOf("preghiera", "fede"), entities.map { it.slug })
    assertEquals(listOf(0, 1), entities.map { it.orderIndex })
    val first = entities.first()
    assertEquals("La preghiera", first.title)
    assertEquals("Parlare con Dio", first.subtitle)
    assertEquals("Fil 4:6-7\nMt 6:9", first.verses)
    assertEquals("Prima riflessione.\n\nSeconda riflessione.", first.body)
  }

  @Test
  fun missingOptionalFields_areEmpty() {
    val second = decodeCuratedDoc(topicsJson.encodeToByteArray()).toTopicEntities()[1]
    assertEquals("", second.subtitle)
    assertEquals("", second.body)
    assertEquals("Eb 11:1", second.verses)
  }

  @Test
  fun emptyDocument_meansNoTopics() {
    assertTrue(decodeCuratedDoc("""{"entries":[]}""".encodeToByteArray()).toTopicEntities().isEmpty())
    assertTrue(decodeCuratedDoc("{}".encodeToByteArray()).toTopicEntities().isEmpty())
  }

  @Test
  fun topicsAndFirstAid_shareTheSameShape() {
    val doc = decodeCuratedDoc(topicsJson.encodeToByteArray())
    val topics = doc.toTopicEntities()
    val firstAid = doc.toFirstAidEntities()
    assertEquals(topics.map { it.slug }, firstAid.map { it.slug })
    assertEquals(topics.map { it.verses }, firstAid.map { it.verses })
    assertEquals(topics.map { it.body }, firstAid.map { it.body })
  }

  @Test
  fun topicsPackage_isSupportedAndImportedAfterBibles() {
    val manifest =
      ContentManifest(
        version = "v1",
        minApp = 1,
        packages = mapOf("topics.json" to PackageInfo("t"), "bible/NTA.json" to PackageInfo("b")),
      )
    assertEquals(listOf("bible/NTA.json", "topics.json"), manifest.packagesToApply(emptyMap()))
  }

  @Test
  fun ensureOnDemand_withoutTopicsInManifest_downloadsNothing() {
    val manifest = ContentManifest(version = "v1", minApp = 1, packages = mapOf("lexicon.json" to PackageInfo("l")))
    // ensurePackage("topics.json") usa packagesToApply(local, only): un pacchetto assente dal manifest non si scarica.
    assertEquals(emptyList(), manifest.packagesToApply(emptyMap(), only = listOf("topics.json")))
  }

  @Test
  fun topicsJsonMissingOnServer_isAFailedDownloadNotACrash() = runTest {
    var calls = 0
    val engine = MockEngine {
      calls++
      respond("", HttpStatusCode.NotFound)
    }
    assertFailsWith<IllegalStateException> {
      ContentDownloader(HttpClient(engine), attempts = 1, retryDelayMs = 0).pkg(base, "topics.json", null)
    }
    assertEquals(1, calls)
  }
}
