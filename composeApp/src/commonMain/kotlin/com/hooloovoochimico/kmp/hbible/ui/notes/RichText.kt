package com.hooloovoochimico.kmp.hbible.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Rich-text span types supported by the note editor. */
object NoteStyleType {
  const val BOLD = "B"
  const val ITALIC = "I"
  const val UNDERLINE = "U"
  const val STRIKETHROUGH = "S"
}

/** A styled half-open character range [s, e) of a note's plain text. */
@Serializable
data class StyleRange(val s: Int, val e: Int, val t: String)

/** Serialization of the style ranges stored in the `styles` column. */
object NoteStyles {
  private val json = Json { ignoreUnknownKeys = true }

  fun encode(ranges: List<StyleRange>): String = json.encodeToString(ranges)

  fun decode(raw: String): List<StyleRange> =
    try {
      json.decodeFromString<List<StyleRange>>(raw)
    } catch (e: SerializationException) {
      emptyList()
    } catch (e: IllegalArgumentException) {
      emptyList()
    }
}

fun spanStyleFor(type: String): SpanStyle =
  when (type) {
    NoteStyleType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    NoteStyleType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    NoteStyleType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    NoteStyleType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    else -> SpanStyle()
  }

/** True when [start, end) is fully covered by existing ranges of [type]. */
fun isStyled(ranges: List<StyleRange>, start: Int, end: Int, type: String): Boolean {
  if (end <= start) return false
  var cursor = start
  ranges
    .filter { it.t == type }
    .sortedBy { it.s }
    .forEach { r ->
      if (r.s > cursor) return false
      if (r.e > cursor) cursor = r.e
      if (cursor >= end) return true
    }
  return cursor >= end
}

/**
 * Toggles [type] on the selection [start, end): removes it when the whole
 * selection already carries the style, adds it otherwise. Ranges are kept
 * non-overlapping per type and merged when adjacent.
 */
fun toggleStyle(ranges: List<StyleRange>, start: Int, end: Int, type: String): List<StyleRange> {
  if (end <= start) return ranges
  val lo = minOf(start, end)
  val hi = maxOf(start, end)
  val others = ranges.filterNot { it.t == type }
  val same = ranges.filter { it.t == type }
  val updated =
    if (isStyled(same, lo, hi, type)) {
      subtract(same, lo, hi)
    } else {
      merge(same + StyleRange(lo, hi, type))
    }
  return others + updated
}

private fun subtract(ranges: List<StyleRange>, start: Int, end: Int): List<StyleRange> =
  ranges.flatMap { r ->
    buildList {
      if (r.s < start) add(StyleRange(r.s, minOf(r.e, start), r.t))
      if (r.e > end) add(StyleRange(maxOf(r.s, end), r.e, r.t))
    }
  }

private fun merge(ranges: List<StyleRange>): List<StyleRange> {
  val sorted = ranges.sortedBy { it.s }
  val out = mutableListOf<StyleRange>()
  for (r in sorted) {
    val last = out.lastOrNull()
    if (last != null && last.t == r.t && r.s <= last.e) {
      out[out.lastIndex] = last.copy(e = maxOf(last.e, r.e))
    } else {
      out.add(r)
    }
  }
  return out.filter { it.e > it.s }
}

/**
 * Shifts style ranges after a text edit. Finds the common prefix/suffix of the
 * old and new text and maps every range end accordingly (ranges fully inside
 * the replaced middle collapse or clip).
 */
fun adjustRanges(oldText: String, newText: String, ranges: List<StyleRange>): List<StyleRange> {
  if (oldText == newText) return ranges
  val minLen = minOf(oldText.length, newText.length)
  var prefix = 0
  while (prefix < minLen && oldText[prefix] == newText[prefix]) prefix++
  var suffix = 0
  while (
    suffix < minLen - prefix &&
    oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
  ) {
    suffix++
  }
  val oldMidEnd = oldText.length - suffix
  val delta = newText.length - oldText.length
  return ranges.mapNotNull { r ->
    // Positions at or after the replaced middle shift by delta; positions at or
    // before the common prefix stay; anything inside the middle clips to its edge.
    val s =
      when {
        r.s >= oldMidEnd -> r.s + delta
        r.s <= prefix -> r.s
        else -> prefix
      }
    val e =
      when {
        r.e >= oldMidEnd -> r.e + delta
        r.e <= prefix -> r.e
        else -> oldMidEnd + delta
      }
    val a = s.coerceIn(0, newText.length)
    val b = e.coerceIn(0, newText.length)
    if (b > a) StyleRange(a, b, r.t) else null
  }
}

/** Applies the stored styles (and reference highlights) for display; text is unchanged. */
class NoteStyleTransformation(
  private val ranges: List<StyleRange>,
  private val refRanges: List<IntRange> = emptyList(),
  private val refColor: Color = Color.Unspecified,
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val builder = AnnotatedString.Builder(text.text)
    val len = text.text.length
    ranges
      .filter { it.s < len }
      .forEach { r -> builder.addStyle(spanStyleFor(r.t), r.s, r.e.coerceAtMost(len)) }
    refRanges.forEach { r ->
      if (r.first < len) {
        builder.addStyle(SpanStyle(background = refColor), r.first, (r.last + 1).coerceAtMost(len))
      }
    }
    return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
  }
}
