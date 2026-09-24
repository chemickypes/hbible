package com.hooloovoochimico.kmp.hbible.data

/**
 * A Bible book as seen by the pure core (parser, UI helpers). Decouples the
 * parser from the Room entity: [com.hooloovoochimico.kmp.hbible.data.local.BookEntity]
 * will implement this in Fase 2.
 */
interface BookRef {
  /** Canonical book number (1-66). */
  val n: Int
  val name: String
  val abbr: String
  val chapters: Int
}
