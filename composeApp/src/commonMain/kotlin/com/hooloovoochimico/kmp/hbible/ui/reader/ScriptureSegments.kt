package com.hooloovoochimico.kmp.hbible.ui.reader

/**
 * Parts of a verse text that the reader styles differently:
 * - [Kind.SELAH]: the "[Pausa]" line of the Psalms (Hebrew "Selah"), in italics;
 * - [Kind.VARIANT]: a whole verse in square brackets (absent from the oldest
 *   manuscripts, e.g. Mt 17:21), dimmed;
 * - [Kind.NORMAL]: everything else (poetry lines stay separated by "\n").
 */
data class ScriptureSegment(val text: String, val kind: Kind) {
  enum class Kind { NORMAL, SELAH, VARIANT }
}

private const val SELAH = "[Pausa]"

fun scriptureSegments(text: String): List<ScriptureSegment> {
  val trimmed = text.trim()
  if (trimmed.length > 2 && trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed != SELAH &&
    trimmed.indexOf(']') == trimmed.lastIndex
  ) {
    return listOf(ScriptureSegment(text, ScriptureSegment.Kind.VARIANT))
  }
  if (!text.contains(SELAH)) return listOf(ScriptureSegment(text, ScriptureSegment.Kind.NORMAL))
  val out = mutableListOf<ScriptureSegment>()
  var rest = text
  while (rest.isNotEmpty()) {
    val i = rest.indexOf(SELAH)
    if (i < 0) {
      out += ScriptureSegment(rest, ScriptureSegment.Kind.NORMAL)
      break
    }
    if (i > 0) out += ScriptureSegment(rest.substring(0, i), ScriptureSegment.Kind.NORMAL)
    out += ScriptureSegment(SELAH, ScriptureSegment.Kind.SELAH)
    rest = rest.substring(i + SELAH.length)
  }
  return out
}
