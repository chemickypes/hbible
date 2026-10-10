package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val BOLD = SpanStyle(fontWeight = FontWeight.SemiBold)
private val ITALIC = SpanStyle(fontStyle = FontStyle.Italic)

/** `**grassetto**`, poi `*corsivo*` o `_corsivo_` (non dentro le parole, es. "all_uso"). */
private val INLINE = Regex("""\*\*(.+?)\*\*|(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])|(?<!\w)_(?!\s)(.+?)(?<!\s)_(?!\w)""")
private val BULLET = Regex("""^\s*[-*•]\s+""")
private val HEADING = Regex("""^\s*#{1,6}\s+""")

/**
 * Il Markdown essenziale delle risposte AI reso come testo con stili: titoli `#` e
 * grassetto, corsivo, elenchi puntati con "•". Il testo che resta (`.text`) è pulito dai
 * simboli, così si può anche salvare in una nota.
 */
fun chatMarkdown(text: String): AnnotatedString =
  buildAnnotatedString {
    text.lines().forEachIndexed { index, rawLine ->
      if (index > 0) append('\n')
      var line = rawLine
      val heading = HEADING.containsMatchIn(line)
      if (heading) line = line.replace(HEADING, "")
      if (BULLET.containsMatchIn(line)) line = line.replace(BULLET, "• ")
      val start = length
      var last = 0
      for (match in INLINE.findAll(line)) {
        append(line.substring(last, match.range.first))
        val (bold, star, underscore) = match.destructured
        when {
          bold.isNotEmpty() -> withStyle(BOLD) { append(bold) }
          star.isNotEmpty() -> withStyle(ITALIC) { append(star) }
          else -> withStyle(ITALIC) { append(underscore) }
        }
        last = match.range.last + 1
      }
      append(line.substring(last))
      if (heading) addStyle(BOLD, start, length)
    }
  }
