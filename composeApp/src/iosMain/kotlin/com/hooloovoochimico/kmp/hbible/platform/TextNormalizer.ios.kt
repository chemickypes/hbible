@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hooloovoochimico.kmp.hbible.platform

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

// Equivalente iOS di java.text.Normalizer NFD + strip \p{Mn}: lowercase (stdlib,
// come nel sorgente JVM) → decomposizione NFD (decomposedStringWithCanonicalMapping)
// → rimozione dei combining marks (categoria Unicode Mn) via stdlib Char.category.
// Nota: i metodi di matching di NSRegularExpression NON sono esposti dal klib
// Foundation di Kotlin/Native 2.3.20 (SDK 26: presenti solo nella selector table
// ObjC, non tra i membri Kotlin) — vedi PLAN §12.
actual fun normalizeForRef(s: String): String {
  val decomposed = (s.lowercase() as NSString).decomposedStringWithCanonicalMapping
  return buildString {
    for (c in decomposed) {
      if (c.category != CharCategory.NON_SPACING_MARK) append(c)
    }
  }
}
