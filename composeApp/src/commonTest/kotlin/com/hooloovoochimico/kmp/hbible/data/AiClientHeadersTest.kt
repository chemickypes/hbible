package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiClientFactory
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Ogni provider riceve la chiave solo nella sua intestazione (issue Gemini 401). */
class AiClientHeadersTest {

  private val replies =
    mapOf(
      AiCompany.GEMINI to """{"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}""",
      AiCompany.ANTHROPIC to """{"content":[{"type":"text","text":"ok"}]}""",
      AiCompany.OPENAI to """{"choices":[{"message":{"content":"ok"}}]}""",
      AiCompany.Z_AI to """{"choices":[{"message":{"content":"ok"}}]}""",
    )

  private suspend fun sentHeaders(company: AiCompany): Headers {
    var sent: Headers? = null
    val engine =
      MockEngine { request ->
        sent = request.headers
        respond(replies.getValue(company), headers = headersOf(HttpHeaders.ContentType, "application/json"))
      }
    val reply =
      AiClientFactory.create(company, "chiave", company.defaultModel, HttpClient(engine))
        .chat("sistema", listOf(AiChatMessage("user", "ciao")), 100)
    assertEquals("ok", reply)
    return sent!!
  }

  @Test
  fun geminiUsesOnlyGoogApiKey() = runTest {
    val headers = sentHeaders(AiCompany.GEMINI)
    assertEquals("chiave", headers["x-goog-api-key"])
    assertNull(headers[HttpHeaders.Authorization])
  }

  @Test
  fun anthropicUsesOnlyXApiKey() = runTest {
    val headers = sentHeaders(AiCompany.ANTHROPIC)
    assertEquals("chiave", headers["x-api-key"])
    assertNull(headers[HttpHeaders.Authorization])
  }

  @Test
  fun openAiCompatibleUseBearer() = runTest {
    for (company in listOf(AiCompany.OPENAI, AiCompany.Z_AI)) {
      assertEquals("Bearer chiave", sentHeaders(company).get(HttpHeaders.Authorization))
    }
  }
}
