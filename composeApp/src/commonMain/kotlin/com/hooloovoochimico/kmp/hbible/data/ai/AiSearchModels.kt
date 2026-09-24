package com.hooloovoochimico.kmp.hbible.data.ai

import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Result of an AI verse lookup: contracted references such as "1 Gv 1:3-5". */
@Serializable
data class AiMatches(
  @SerialName("corrispondenze_perfette") val perfect: List<String> = emptyList(),
  @SerialName("simili") val similar: List<String> = emptyList(),
  /** Breve riflessione di conforto, presente solo nelle ricerche "pronto soccorso". */
  val riflessione: String = "",
)

/** Extracts [AiMatches] from the model reply, tolerating markdown fences or extra text. */
object AiSearchResponseParser {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  fun parse(content: String): AiMatches {
    val start = content.indexOf('{')
    val end = content.lastIndexOf('}')
    if (start < 0 || end <= start) {
      throw IOException("Il modello non ha restituito un JSON valido")
    }
    return try {
      json.decodeFromString<AiMatches>(content.substring(start, end + 1))
    } catch (e: SerializationException) {
      throw IOException("Risposta AI non interpretabile", e)
    } catch (e: IllegalArgumentException) {
      throw IOException("Risposta AI non interpretabile", e)
    }
  }
}
