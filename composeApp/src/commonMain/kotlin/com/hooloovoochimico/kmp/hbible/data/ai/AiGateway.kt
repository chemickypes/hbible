package com.hooloovoochimico.kmp.hbible.data.ai

import com.hooloovoochimico.kmp.hbible.appLog
import kotlinx.coroutines.CancellationException

/** System/user prompts shared by every provider. */
object AiPrompts {
  fun verseSearchSystem(bookCodes: List<String>): String =
    buildString {
      appendLine("Sei un esperto della Bibbia che aiuta a ritrovare versetti a partire da una descrizione approssimativa.")
      appendLine("L'utente descrive con parole sue un versetto o un brano biblico; individua i riferimenti più probabili nella Bibbia.")
      appendLine("Rispondi SOLO con un oggetto JSON, senza alcun testo prima o dopo, con questa struttura esatta:")
      appendLine("""{"corrispondenze_perfette": ["Sigla Cap:Vers"], "simili": ["Sigla Cap:Vers"]}""")
      appendLine("- \"corrispondenze_perfette\": versetti che corrispondono fedelmente alla descrizione (al massimo 5).")
      appendLine("- \"simili\": versetti collegati allo stesso tema o concetto (al massimo 5).")
      appendLine("- Ogni riferimento usa ESCLUSIVAMENTE queste sigle: ${bookCodes.joinToString(", ")}.")
      appendLine("- Formato: \"Sigla Cap:Vers\" (es. \"Gv 3:16\"), oppure \"Sigla Cap:VersA-VersB\" per un intervallo (es. \"1 Gv 1:3-5\").")
      appendLine("- Non inventare sigle fuori lista e non usare nomi completi dei libri.")
      appendLine("- Se non trovi nulla di sensato, restituisci liste vuote.")
    }

  fun glossSystem(): String =
    "Sei un lessicografo biblico. Rispondi SOLO con la traduzione richiesta, senza commenti né virgolette."

  fun glossUser(gloss: String, lang: String): String {
    val language = if (lang == "he") "ebraico" else "greco"
    return "Traduci in italiano questo significato da dizionario biblico (termine $language): \"$gloss\""
  }

  fun bookChatSystem(bookName: String, testament: String, chapters: Int, summary: String?): String =
    buildString {
      appendLine("Sei un esperto della Bibbia. L'utente sta leggendo il libro di $bookName ($testament, $chapters capitoli) e ti fa domande al riguardo.")
      summary?.let { appendLine("Introduzione del libro: $it") }
      appendLine("Rispondi in italiano, in modo chiaro, fedele al testo biblico e conciso (massimo 180 parole).")
      appendLine("Quando citi un versetto usa il formato \"Libro Cap:Vers\" (es. \"Gv 3:16\").")
    }

  fun verseChatSystem(reference: String, verseText: String, translationName: String): String =
    buildString {
      appendLine("Sei un esperto della Bibbia. L'utente sta leggendo $reference ($translationName):")
      appendLine("\"$verseText\"")
      appendLine("Rispondi in italiano alle sue domande su questo versetto: spiegazione del testo, contesto, riferimenti.")
      appendLine("Sii chiaro, fedele al testo e conciso (massimo 180 parole).")
      appendLine("Quando citi un versetto usa il formato \"Libro Cap:Vers\" (es. \"Gv 3:16\").")
    }
}

/**
 * Routes AI requests to the provider the user prioritized among the enabled ones.
 * If the top-priority service fails, the next one is tried (fallback); z.ai has
 * priority by default. The config is re-read on every call so settings changes
 * apply immediately.
 */
class AiGateway(
  private val configProvider: () -> AiConfig,
  private val clientFactory: (AiProviderConfig, AiConfig) -> AiChatClient =
    { p, c -> AiClientFactory.createForEntry(p, c) },
  /** Interruttore delle funzioni IA: spento = nessuna chiamata, catena vuota. */
  private val featuresEnabled: () -> Boolean = { true },
) {

  /** The usable chain: enabled providers with a key, in priority order (empty when the AI features are off). */
  val chain: List<AiChatClient>
    get() {
      if (!featuresEnabled()) return emptyList()
      val config = configProvider()
      return config.enabledChain().map { clientFactory(it, config) }
    }

  private fun requireChain(): List<AiChatClient> {
    if (chain.isEmpty()) {
      throw IllegalStateException(
        "Nessun servizio AI configurato: aggiungi una chiave in Impostazioni → Assistente AI",
      )
    }
    return chain
  }

  /**
   * Asks the AI to map a user description to Bible references, trying providers
   * in priority order until one succeeds.
   */
  suspend fun search(prompt: String, bookCodes: List<String>): AiMatches {
    var lastError: Throwable? = null
    for (client in requireChain()) {
      try {
        val content = client.chat(AiPrompts.verseSearchSystem(bookCodes), prompt, MAX_SEARCH_TOKENS)
        return AiSearchResponseParser.parse(content)
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
        appLog.w(t) { "AI search: provider ${client.company.label} fallito, provo il successivo" }
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("Nessun servizio AI disponibile")
  }

  /** Translates a Strong's dictionary gloss into Italian. Returns the bare translation. */
  suspend fun translateGloss(gloss: String, lang: String): String {
    var lastError: Throwable? = null
    for (client in requireChain()) {
      try {
        val content =
          client.chat(AiPrompts.glossSystem(), AiPrompts.glossUser(gloss, lang), MAX_GLOSS_TOKENS)
        if (content.isNotBlank()) return content
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
        appLog.w(t) { "AI translateGloss: provider ${client.company.label} fallito, provo il successivo" }
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("Nessun servizio AI disponibile")
  }

  /** Multi-turn chat with the AI, trying providers in priority order. */
  suspend fun chat(system: String, messages: List<AiChatMessage>, maxTokens: Int = MAX_CHAT_TOKENS): String {
    var lastError: Throwable? = null
    for (client in requireChain()) {
      try {
        val content = client.chat(system, messages, maxTokens)
        if (content.isNotBlank()) return content
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
        appLog.w(t) { "AI chat: provider ${client.company.label} fallito, provo il successivo" }
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("Nessun servizio AI disponibile")
  }

  private companion object {
    const val MAX_SEARCH_TOKENS = 2048
    const val MAX_GLOSS_TOKENS = 200
    const val MAX_CHAT_TOKENS = 1024
  }
}
