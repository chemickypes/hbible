package com.hooloovoochimico.kmp.hbible.data.ai

import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** AI-generated introduction of a Bible book. */
@Serializable
data class AiBookInfo(
  val contesto: String = "",
  val protagonisti: String = "",
  val cristocentrico: String = "",
)

/** Extracts [AiBookInfo] from the model reply, tolerating markdown fences or extra text. */
object AiBookInfoParser {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  fun parse(content: String): AiBookInfo {
    val start = content.indexOf('{')
    val end = content.lastIndexOf('}')
    if (start < 0 || end <= start) {
      throw IOException("Il modello non ha restituito un JSON valido")
    }
    val info =
      try {
        json.decodeFromString<AiBookInfo>(content.substring(start, end + 1))
      } catch (e: SerializationException) {
        throw IOException("Risposta AI non interpretabile", e)
      } catch (e: IllegalArgumentException) {
        throw IOException("Risposta AI non interpretabile", e)
      }
    if (info.contesto.isBlank() && info.protagonisti.isBlank() && info.cristocentrico.isBlank()) {
      throw IOException("Risposta AI incompleta")
    }
    return info
  }
}
