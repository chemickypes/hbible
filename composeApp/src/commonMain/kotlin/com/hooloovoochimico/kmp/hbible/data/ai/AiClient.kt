package com.hooloovoochimico.kmp.hbible.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val CONNECT_TIMEOUT_MS = 20_000L
private const val REQUEST_TIMEOUT_MS = 120_000L

/**
 * HTTP client per i provider AI (Ktor; engine OkHttp su Android, Darwin su iOS,
 * auto-selezionato per target). Timeout come il client OkHttp del sorgente:
 * 20s connect / 120s request.
 */
fun aiHttpClient(): HttpClient =
  HttpClient {
    install(HttpTimeout) {
      connectTimeoutMillis = CONNECT_TIMEOUT_MS
      requestTimeoutMillis = REQUEST_TIMEOUT_MS
    }
  }

/** Shared HTTP plumbing for the provider clients. */
abstract class BaseAiClient(
  final override val company: AiCompany,
  protected val apiKey: String,
  protected val model: String,
  private val client: HttpClient,
) : AiChatClient {
  protected val json = Json { ignoreUnknownKeys = true }

  protected abstract fun endpointUrl(): String

  protected abstract fun buildRequestBody(system: String, messages: List<AiChatMessage>, maxTokens: Int): String

  protected abstract fun extractContent(body: String): String

  protected open fun headers(builder: HttpRequestBuilder) {}

  final override suspend fun chat(system: String, messages: List<AiChatMessage>, maxTokens: Int): String {
    val response =
      client.post(endpointUrl()) {
        headers(this)
        header("Authorization", "Bearer $apiKey")
        contentType(ContentType.Application.Json)
        setBody(buildRequestBody(system, messages, maxTokens))
      }
    val text = response.bodyAsText()
    if (!response.status.isSuccess()) {
      throw IOException(
        "Errore ${company.label} (${response.status.value}): ${errorMessage(text)}",
      )
    }
    return extractContent(text).trim()
  }

  protected fun errorMessage(body: String): String =
    try {
      json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        ?: body.take(200)
    } catch (e: IllegalArgumentException) {
      body.take(200)
    }
}

/**
 * OpenAI-compatible chat completions: used by z.ai and OpenAI.
 * [thinkingDisabled] adds z.ai's `thinking: {type: disabled}` hint.
 */
open class OpenAiCompatibleClient(
  company: AiCompany,
  apiKey: String,
  model: String,
  private val endpoint: String,
  private val thinkingDisabled: Boolean = false,
  client: HttpClient = aiHttpClient(),
) : BaseAiClient(company, apiKey, model, client) {
  override fun endpointUrl(): String = endpoint

  override fun buildRequestBody(system: String, messages: List<AiChatMessage>, maxTokens: Int): String =
    buildJsonObject {
      put("model", model)
      put("temperature", 0.2)
      put("max_tokens", maxTokens)
      if (thinkingDisabled) {
        putJsonObject("thinking") { put("type", "disabled") }
      }
      putJsonArray("messages") {
        add(message("system", system))
        messages.forEach { add(message(it.role, it.content)) }
      }
    }.toString()

  override fun extractContent(body: String): String =
    json.parseToJsonElement(body)
      .jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject
      ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
      ?: throw IOException("Risposta inattesa dal modello")

  private fun message(role: String, content: String): JsonObject =
    buildJsonObject {
      put("role", role)
      put("content", content)
    }
}

/** Anthropic Messages API (Claude). */
class AnthropicClient(
  company: AiCompany,
  apiKey: String,
  model: String,
  private val endpoint: String = company.endpoint,
  client: HttpClient = aiHttpClient(),
) : BaseAiClient(company, apiKey, model, client) {
  override fun endpointUrl(): String = endpoint

  override fun headers(builder: HttpRequestBuilder) {
    builder.header("x-api-key", apiKey)
    builder.header("anthropic-version", "2023-06-01")
  }

  override fun buildRequestBody(system: String, messages: List<AiChatMessage>, maxTokens: Int): String =
    buildJsonObject {
      put("model", model)
      put("temperature", 0.2)
      put("max_tokens", maxTokens)
      put("system", system)
      putJsonArray("messages") {
        messages.forEach { m ->
          add(
            buildJsonObject {
              put("role", m.role)
              put("content", m.content)
            },
          )
        }
      }
    }.toString()

  override fun extractContent(body: String): String =
    json.parseToJsonElement(body)
      .jsonObject["content"]?.jsonArray
      ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }
      ?.jsonObject?.get("text")?.jsonPrimitive?.content
      ?: throw IOException("Risposta inattesa dal modello")
}

/** Google Gemini generateContent API. */
class GeminiClient(
  company: AiCompany,
  apiKey: String,
  model: String,
  private val endpointBase: String = company.endpoint,
  client: HttpClient = aiHttpClient(),
) : BaseAiClient(company, apiKey, model, client) {
  override fun endpointUrl(): String = "$endpointBase/$model:generateContent"

  override fun headers(builder: HttpRequestBuilder) {
    builder.header("x-goog-api-key", apiKey)
  }

  override fun buildRequestBody(system: String, messages: List<AiChatMessage>, maxTokens: Int): String =
    buildJsonObject {
      putJsonObject("systemInstruction") {
        putJsonArray("parts") { add(part(system)) }
      }
      putJsonArray("contents") {
        messages.forEach { m ->
          add(
            buildJsonObject {
              // Gemini uses "model" instead of "assistant".
              put("role", if (m.role == "assistant") "model" else "user")
              putJsonArray("parts") { add(part(m.content)) }
            },
          )
        }
      }
      putJsonObject("generationConfig") {
        put("temperature", 0.2)
        put("maxOutputTokens", maxTokens)
      }
    }.toString()

  override fun extractContent(body: String): String =
    json.parseToJsonElement(body)
      .jsonObject["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
      ?.get("content")?.jsonObject?.get("parts")?.jsonArray
      ?.mapNotNull { part ->
        part.jsonObject["text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
      }
      ?.joinToString("")
      ?.takeIf { it.isNotBlank() }
      ?: throw IOException("Risposta inattesa dal modello")

  private fun part(text: String): JsonObject =
    buildJsonObject { put("text", text) }
}

/** Builds the right client for a configured provider. */
object AiClientFactory {
  fun create(company: AiCompany, apiKey: String, model: String, client: HttpClient = aiHttpClient()): AiChatClient =
    when (company) {
      AiCompany.Z_AI ->
        OpenAiCompatibleClient(company, apiKey, model, company.endpoint, thinkingDisabled = true, client = client)
      AiCompany.OPENAI -> OpenAiCompatibleClient(company, apiKey, model, company.endpoint, client = client)
      AiCompany.ANTHROPIC -> AnthropicClient(company, apiKey, model, client = client)
      AiCompany.GEMINI -> GeminiClient(company, apiKey, model, client = client)
    }

  fun create(config: AiProviderConfig, fallbackModel: String, client: HttpClient = aiHttpClient()): AiChatClient {
    val company = AiCompany.valueOf(config.company)
    return create(company, config.apiKey.trim(), config.model.trim().ifBlank { fallbackModel }, client)
  }

  /** Builds the client for a chain entry with the user's key and effective model. */
  fun createForEntry(
    entry: AiProviderConfig,
    config: AiConfig,
    client: HttpClient = aiHttpClient(),
  ): AiChatClient = create(entry, config.effectiveModel(AiCompany.valueOf(entry.company)), client)
}
