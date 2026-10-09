package com.hooloovoochimico.kmp.hbible.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLinksTest {
  @Test
  fun aiReport_opensPrefilledIssueOfPublicRepo() {
    val url = aiReportUrl("Risposta & con\nun a capo", "chat")
    assertTrue(url.startsWith("$FEEDBACK_URL?title="))
    assertTrue("&body=" in url)
    // Testo codificato: niente spazi, & o a capo non codificati nella query.
    assertFalse(url.substringAfter("?").contains(' '))
    assertFalse(url.substringAfter("&body=").contains('&'))
    assertFalse(url.contains('\n'))
  }

  @Test
  fun aiReport_truncatesLongContent() {
    assertTrue(aiReportUrl("x".repeat(10_000), "chat").length < 6_000)
  }
}
