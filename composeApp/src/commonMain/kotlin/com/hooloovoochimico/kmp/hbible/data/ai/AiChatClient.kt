package com.hooloovoochimico.kmp.hbible.data.ai

/**
 * Interfaccia chat pura estratta da AiClient.kt (le implementazioni HTTP su
 * OkHttp/Ktor arrivano in Fase 2): serve ad AiGateway e ai test.
 */

/** One turn of a conversation: "user" or "assistant". */
data class AiChatMessage(val role: String, val content: String)

/** Minimal chat interface every AI provider implements. */
interface AiChatClient {
  val company: AiCompany

  /** Single-turn chat: returns the bare model reply text. */
  suspend fun chat(system: String, user: String, maxTokens: Int): String =
    chat(system, listOf(AiChatMessage("user", user)), maxTokens)

  /** Multi-turn chat: returns the bare model reply text. */
  suspend fun chat(system: String, messages: List<AiChatMessage>, maxTokens: Int): String
}
