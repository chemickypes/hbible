package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.ai.AiSearchResponseParser
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiSearchResponseParserTest {

  @Test
  fun `parses plain json`() {
    val content =
      """{"corrispondenze_perfette": ["1 Gv 1:3-5", "Gv 3:16"], "simili": ["Sal 23"]}"""
    val matches = AiSearchResponseParser.parse(content)
    assertEquals(listOf("1 Gv 1:3-5", "Gv 3:16"), matches.perfect)
    assertEquals(listOf("Sal 23"), matches.similar)
  }

  @Test
  fun `parses json fenced in markdown with extra text`() {
    val content =
      """
      Ecco i versetti che corrispondono:
      ```json
      {"corrispondenze_perfette": ["1Cor 13:4-7"], "simili": ["1 Gv 4:8", "Rm 5:5"]}
      ```
      Spero ti siano d'aiuto!
      """.trimIndent()
    val matches = AiSearchResponseParser.parse(content)
    assertEquals(listOf("1Cor 13:4-7"), matches.perfect)
    assertEquals(listOf("1 Gv 4:8", "Rm 5:5"), matches.similar)
  }

  @Test
  fun `missing lists default to empty`() {
    val matches = AiSearchResponseParser.parse("""{"corrispondenze_perfette": ["Sal 23:1"]}""")
    assertEquals(listOf("Sal 23:1"), matches.perfect)
    assertTrue(matches.similar.isEmpty())
  }

  @Test
  fun `non json content throws IOException`() {
    try {
      AiSearchResponseParser.parse("Mi dispiace, non ho capito la richiesta.")
      throw AssertionError("expected IOException")
    } catch (e: IOException) {
      assertEquals("Il modello non ha restituito un JSON valido", e.message)
    }
  }

  @Test
  fun `roundtrip through full z ai response envelope`() {
    val envelope =
      Json.parseToJsonElement(
        """{"choices":[{"message":{"role":"assistant","content":"{\"corrispondenze_perfette\":[\"Gv 3:16\"],\"simili\":[]}"}}]}""",
      )
    val content =
      envelope.jsonObject["choices"]!!
        .jsonArray[0].jsonObject["message"]!!
        .jsonObject["content"]!!
        .jsonPrimitive.content
    val matches = AiSearchResponseParser.parse(content)
    assertEquals(listOf("Gv 3:16"), matches.perfect)
  }
}
