package com.hooloovoochimico.kmp.hbible.data

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hooloovoochimico.kmp.hbible.ui.common.chatMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatMarkdownTest {

  @Test
  fun boldAndItalicLoseTheirMarkers() {
    val out = chatMarkdown("In **2 Cor 10:1**, Paolo usa un tono *ironico*.")
    assertEquals("In 2 Cor 10:1, Paolo usa un tono ironico.", out.text)
    val bold = out.spanStyles.single { it.item.fontWeight == FontWeight.SemiBold }
    assertEquals("2 Cor 10:1", out.text.substring(bold.start, bold.end))
    val italic = out.spanStyles.single { it.item.fontStyle == FontStyle.Italic }
    assertEquals("ironico", out.text.substring(italic.start, italic.end))
  }

  @Test
  fun bulletsAndHeadings() {
    val out = chatMarkdown("## Contesto\n- primo\n* secondo")
    assertEquals("Contesto\n• primo\n• secondo", out.text)
    assertTrue(out.spanStyles.any { it.start == 0 && it.end == "Contesto".length })
  }

  @Test
  fun plainTextIsUnchanged() {
    val text = "Versetto 3:16, nessun simbolo; 2 * 3 = 6 e nome_file resta."
    assertEquals(text, chatMarkdown(text).text)
  }
}
