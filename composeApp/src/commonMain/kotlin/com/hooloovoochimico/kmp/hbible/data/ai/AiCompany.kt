package com.hooloovoochimico.kmp.hbible.data.ai

/**
 * AI services supported by the app, in default priority order (z.ai first).
 * Models are kept inexpensive on purpose: the app only needs short JSON replies.
 */
enum class AiCompany(
  val label: String,
  val defaultModel: String,
  val modelPresets: List<String>,
  val endpoint: String,
) {
  Z_AI(
    "z.ai (GLM)",
    "glm-4.6",
    listOf("glm-4.6", "glm-4.5-air", "glm-4.5-flash"),
    "https://api.z.ai/api/paas/v4/chat/completions",
  ),
  OPENAI(
    "OpenAI (ChatGPT)",
    "gpt-5-mini",
    listOf("gpt-5-mini", "gpt-5-nano", "gpt-4o-mini"),
    "https://api.openai.com/v1/chat/completions",
  ),
  ANTHROPIC(
    "Anthropic (Claude)",
    "claude-haiku-4-5",
    listOf("claude-haiku-4-5", "claude-3-5-haiku-latest", "claude-sonnet-4-5"),
    "https://api.anthropic.com/v1/messages",
  ),
  GEMINI(
    "Google (Gemini)",
    "gemini-2.5-flash",
    listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash"),
    "https://generativelanguage.googleapis.com/v1beta/models",
  ),
}
