package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.ai.AiBookInfo
import com.hooloovoochimico.kmp.hbible.data.ai.AiBookInfoParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.IOException

class AiBookInfoParserTest {

  @Test
  fun parsesPlainJson() {
    val content =
      """{"contesto": "Scritto da Paolo.", "protagonisti": "Paolo, Timoteo.", """ +
        """"cristocentrico": "Cristo è il capo della Chiesa."}"""
    val info: AiBookInfo = AiBookInfoParser.parse(content)
    assertEquals("Scritto da Paolo.", info.contesto)
    assertEquals("Paolo, Timoteo.", info.protagonisti)
    assertEquals("Cristo è il capo della Chiesa.", info.cristocentrico)
  }

  @Test
  fun parsesJsonInsideMarkdownFences() {
    val content =
      "Ecco l'introduzione:\n```json\n" +
        """{"contesto": "Genere profetico.", "protagonisti": "Osea.", "cristocentrico": "Amore fedele."}""" +
        "\n```"
    val info = AiBookInfoParser.parse(content)
    assertEquals("Genere profetico.", info.contesto)
    assertEquals("Osea.", info.protagonisti)
    assertEquals("Amore fedele.", info.cristocentrico)
  }

  @Test
  fun toleratesUnknownKeys() {
    val content =
      """{"contesto": "Testo.", "protagonisti": "Mosè.", "cristocentrico": "Promesse.", "extra": 42}"""
    val info = AiBookInfoParser.parse(content)
    assertEquals("Testo.", info.contesto)
    assertEquals("Mosè.", info.protagonisti)
    assertEquals("Promesse.", info.cristocentrico)
  }

  @Test
  fun rejectsContentWithoutJson() {
    assertFailsWith<IOException> {
      AiBookInfoParser.parse("Non ho capito la domanda.")
    }
  }

  @Test
  fun rejectsEmptyObject() {
    assertFailsWith<IOException> {
      AiBookInfoParser.parse("{}")
    }
  }
}
