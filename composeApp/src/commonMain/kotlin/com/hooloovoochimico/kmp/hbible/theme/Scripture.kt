package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.hooloovoochimico.kmp.hbible.data.ReaderFontSize

/**
 * Typography of the biblical text (serif reading styles). Every screen that
 * shows scripture derives its style from here, so the reading experience is
 * consistent across screens and can be retuned in one place.
 */
object ScriptureTypography {
  private val serif = FontFamily.Serif

  /** Verse text in the reader; size follows the user setting. */
  fun reader(size: ReaderFontSize): TextStyle =
    TextStyle(fontFamily = serif, fontSize = size.fontSizeSp.sp, lineHeight = size.lineHeightSp.sp)

  /** Verse text in the verse detail page. */
  val detail = TextStyle(fontFamily = serif, fontSize = 20.sp, lineHeight = 29.sp)

  /** Original-language (Hebrew/Greek) words, right-to-left for Hebrew. */
  val original = TextStyle(fontFamily = serif, fontSize = 26.sp, lineHeight = 44.sp)

  fun original(lang: String): TextStyle =
    original.copy(textDirection = if (lang == "he") TextDirection.Rtl else TextDirection.Ltr)

  /** Transliteration shown under the selected original-language word. */
  val originalGloss = TextStyle(fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 20.sp)

  /** Full transliteration paragraph of a verse. */
  val transliteration =
    TextStyle(fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 25.sp)

  /** Serif body: notes (editor, preview, placeholder) and verse previews in chips. */
  val body = TextStyle(fontFamily = serif, fontSize = 17.sp, lineHeight = 26.sp)

  /** Introduction paragraphs of the book info page. */
  val intro = TextStyle(fontFamily = serif, fontSize = 16.sp, lineHeight = 24.sp)
}
