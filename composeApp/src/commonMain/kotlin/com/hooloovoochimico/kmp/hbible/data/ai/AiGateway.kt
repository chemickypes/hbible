package com.hooloovoochimico.kmp.hbible.data.ai

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

  fun firstAidSystem(bookCodes: List<String>): String =
    buildString {
      appendLine("Sei un esperto della Bibbia che porta conforto tramite le Scritture a chi attraversa un momento difficile.")
      appendLine("L'utente indica la sua difficoltà; individua i versetti più adatti a confortare e aggiungi una breve riflessione.")
      appendLine("Rispondi SOLO con un oggetto JSON, senza alcun testo prima o dopo, con questa struttura esatta:")
      appendLine("""{"riflessione": "...", "corrispondenze_perfette": ["Sigla Cap:Vers"], "simili": ["Sigla Cap:Vers"]}""")
      appendLine("- \"riflessione\": in italiano, breve (max 120 parole), tono caldo e sobrio: riconosci la fatica della persona,")
      appendLine("  offri speranza senza minimizzare e cita al massimo un versetto senza elencarne altri.")
      appendLine("- \"corrispondenze_perfette\": versetti che parlano direttamente di questa difficoltà (al massimo 5).")
      appendLine("- \"simili\": versetti di speranza e consolazione collegati al tema (al massimo 5).")
      appendLine("- Ogni riferimento usa ESCLUSIVAMENTE queste sigle: ${bookCodes.joinToString(", ")}.")
      appendLine("- Formato: \"Sigla Cap:Vers\" (es. \"Gv 3:16\"), oppure \"Sigla Cap:VersA-VersB\" per un intervallo (es. \"1 Gv 1:3-5\").")
      appendLine("- Non inventare sigle fuori lista e non usare nomi completi dei libri.")
    }

  fun firstAidUser(theme: String): String = "Pronto soccorso per: $theme."

  fun glossSystem(): String =
    "Sei un lessicografo biblico. Rispondi SOLO con la traduzione richiesta, senza commenti né virgolette."

  fun glossUser(gloss: String, lang: String): String {
    val language = if (lang == "he") "ebraico" else "greco"
    return "Traduci in italiano questo significato da dizionario biblico (termine $language): \"$gloss\""
  }

  fun bookInfoSystem(): String =
    buildString {
      appendLine("Sei un esperto di Bibbia che scrive introduzioni ai libri biblici per lettori italiani.")
      appendLine("Rispondi SOLO con un oggetto JSON, senza alcun testo prima o dopo, con questa struttura esatta:")
      appendLine("""{"contesto": "...", "protagonisti": "...", "cristocentrico": "..."}""")
      appendLine("- \"contesto\": autore, data probabile, destinatari, genere letterario e situazione storica (un paragrafo, max 90 parole).")
      appendLine("- \"protagonisti\": i personaggi principali del libro, ciascuno con una descrizione di una riga (max 6 personaggi).")
      appendLine("- \"cristocentrico\": come il libro rivela e punta a Gesù Cristo nella storia della salvezza (un paragrafo, max 90 parole).")
      appendLine("- Scrivi in italiano, con tono chiaro e fedele al testo biblico; non inventare dati discutibili.")
    }

  fun bookInfoUser(bookName: String, testament: String, chapters: Int): String =
    "Scrivi l'introduzione del libro di $bookName ($testament, $chapters capitoli) nel formato JSON richiesto."

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
    { p, c -> AiClientFactory.create(p, c.effectiveModel(AiCompany.valueOf(p.company))) },
) {

  /** The usable chain: enabled providers with a key, in priority order. */
  val chain: List<AiChatClient>
    get() {
      val config = configProvider()
      return config.enabledChain().map { clientFactory(it, config) }
    }

  private fun requireChain(): List<AiChatClient> {
    if (chain.isEmpty()) {
      throw IllegalStateException(
        "Nessun servizio AI configurato: aggiungi una chiave in Impostazioni → Ricerca AI",
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
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("Nessun servizio AI disponibile")
  }

  /**
   * "Pronto soccorso": versetti di conforto e una breve riflessione per una
   * difficoltà personale (ansia, lutto, ...), con fallback sul provider successivo.
   */
  suspend fun firstAid(theme: String, bookCodes: List<String>): AiMatches {
    var lastError: Throwable? = null
    for (client in requireChain()) {
      try {
        val content =
          client.chat(AiPrompts.firstAidSystem(bookCodes), AiPrompts.firstAidUser(theme), MAX_FIRST_AID_TOKENS)
        return AiSearchResponseParser.parse(content)
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
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
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("Nessun servizio AI disponibile")
  }

  /** Generates the introduction (context, protagonists, Christocentric vision) of a book. */
  suspend fun bookInfo(bookName: String, testament: String, chapters: Int): Pair<AiBookInfo, String> {
    var lastError: Throwable? = null
    for (client in requireChain()) {
      try {
        val content =
          client.chat(
            AiPrompts.bookInfoSystem(),
            AiPrompts.bookInfoUser(bookName, testament, chapters),
            MAX_BOOK_INFO_TOKENS,
          )
        if (content.isNotBlank()) return AiBookInfoParser.parse(content) to client.company.label
      } catch (e: CancellationException) {
        throw e
      } catch (t: Throwable) {
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
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("Nessun servizio AI disponibile")
  }

  private companion object {
    const val MAX_SEARCH_TOKENS = 2048
    const val MAX_FIRST_AID_TOKENS = 2048
    const val MAX_GLOSS_TOKENS = 200
    const val MAX_BOOK_INFO_TOKENS = 2048
    const val MAX_CHAT_TOKENS = 1024
  }
}
