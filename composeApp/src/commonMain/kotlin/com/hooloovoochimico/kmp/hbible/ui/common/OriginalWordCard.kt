package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.data.local.LexemeEntity
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Lemma concordance cap: the occurrences query returns at most this many verses. */
const val LEMMA_OCCURRENCES_LIMIT = 40

/**
 * Dati di una parola originale selezionata: parola, traslitterazione, glossa,
 * parola italiana allineata, lemma (numero Strong, null = non disponibile), voce
 * di lessico e occorrenze del lemma in altri versetti.
 */
data class OriginalWordInfo(
  val word: String,
  val transliteration: String,
  /** Glossa contestuale (italiano se curata, altrimenti inglese); "" = nessuna. */
  val gloss: String,
  /** Token italiano allineato alla parola ("" = nessun allineamento). */
  val italianWord: String,
  /** Numero Strong del lemma ("7225"), null se assente o "-". */
  val lemma: String?,
  val lexeme: LexemeEntity?,
  val occurrences: List<VerseRef> = emptyList(),
  val occurrencesLoading: Boolean = false,
)

/**
 * Scheda di una parola originale (lemma, Strong, glossa, occorrenze). Usata dalla
 * tab Interlineare, nel foglio che si apre toccando una colonna di parola.
 * [lang] "he" o "el" decide direzione e stile del lemma.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OriginalWordCard(
  info: OriginalWordInfo,
  lang: String,
  bookName: (Int) -> String,
  loadVerse: suspend (VerseRef) -> String?,
  onOpenReference: (VerseRef) -> Unit,
  onOpenDetail: ((VerseRef) -> Unit)?,
  /** Traduce la glossa del lessico in italiano (con AI) e la salva; null = fallito. */
  /** null = funzioni IA spente: niente pulsante di traduzione. */
  onTranslateGloss: (suspend (LexemeEntity) -> String?)?,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  // Il lessico può ricevere la traduzione italiana a schermo aperto: il valore
  // mostrato segue la copia locale finché la parola non cambia.
  var lexeme by remember(info.lexeme) { mutableStateOf(info.lexeme) }
  var translating by remember(info.lexeme) { mutableStateOf(false) }
  var translateFailed by remember(info.lexeme) { mutableStateOf(false) }

  HBibleCard(
    shape = MaterialTheme.shapes.extraLarge,
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        info.word,
        style = ScriptureTypography.original(lang).copy(lineHeight = TextUnit.Unspecified),
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onClose) {
        Icon(
          AppIcons.Close,
          contentDescription = "Chiudi la scheda della parola",
          modifier = Modifier.size(20.dp),
        )
      }
    }
    val strong = info.lemma?.let { if (lang == "he") "h$it" else "g$it" }
    if (strong != null) {
      Text(
        listOfNotNull(strong, lexeme?.romanized?.takeIf { it.isNotBlank() }).joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    if (info.transliteration.isNotEmpty()) {
      Text(
        info.transliteration,
        style = ScriptureTypography.originalGloss,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }
    if (info.gloss.isNotEmpty()) {
      Text(
        info.gloss,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
    if (info.italianWord.isNotEmpty()) {
      Text(
        "In italiano:",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
      )
      Text(
        info.italianWord,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
    lexeme?.let { lx ->
      Column(Modifier.padding(top = 6.dp)) {
        val italian = lx.glossIt
        if (italian.isNotBlank()) {
          Text(italian, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text(
            lx.gloss,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
          )
        } else {
          Text(lx.gloss, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
          if (onTranslateGloss != null) TextButton(onClick = {
            scope.launch {
              translating = true
              translateFailed = false
              try {
                val result = onTranslateGloss(lx)
                if (result != null) lexeme = lx.copy(glossIt = result) else translateFailed = true
              } catch (e: CancellationException) {
                throw e
              } finally {
                translating = false
              }
            }
          }) {
            Text(if (translating) "Traduzione in corso…" else "Traduci in italiano (AI)")
          }
          if (onTranslateGloss != null && translateFailed && !translating) {
            Text(
              "Traduzione non riuscita: configura un servizio AI in Impostazioni → Assistente AI.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }

    val count = info.occurrences.size
    val countLabel =
      when {
        info.occurrencesLoading -> "Ricerca in corso…"
        info.lemma == null -> "Lemma non disponibile"
        count == 0 -> "Nessuna altra occorrenza"
        count == 1 -> "Ricorre in 1 altro versetto"
        count == LEMMA_OCCURRENCES_LIMIT -> "Ricorre in $count+ altri versetti"
        else -> "Ricorre in $count altri versetti"
      }
    Text(
      countLabel,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
    if (!info.occurrencesLoading && info.occurrences.isNotEmpty()) {
      VerseChipRow(
        refs = info.occurrences,
        bookName = bookName,
        loadVerse = loadVerse,
        onOpenReference = onOpenReference,
        // Velatura dell'accento sopra il fondo della scheda: secondaryContainer
        // al buio è più scuro della scheda (verde su grigio-blu senza contrasto).
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
          .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentColor = MaterialTheme.colorScheme.onSurface,
        onOpenDetail = onOpenDetail,
      )
    }
  }
}
