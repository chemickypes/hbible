package com.hooloovoochimico.kmp.hbible.platform

/**
 * Normalizzazione Unicode usata dal parser dei riferimenti biblici:
 * decomposizione NFD, rimozione dei combining marks e lowercase
 * ("Giosuè" → "giosue"). Estratta dal `normalize` privato di
 * BibleReferenceParser perché richiede API platform.
 */
expect fun normalizeForRef(s: String): String
