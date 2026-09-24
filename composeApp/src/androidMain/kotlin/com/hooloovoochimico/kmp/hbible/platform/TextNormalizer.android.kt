package com.hooloovoochimico.kmp.hbible.platform

import java.text.Normalizer

// Logica identica al normalize privato del sorgente: lowercase → NFD → strip \p{Mn}.
actual fun normalizeForRef(s: String): String =
  Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
