package com.hooloovoochimico.kmp.hbible.ui.common

import com.hooloovoochimico.kmp.hbible.data.BookRef

/** Identifies a verse independently of the current chapter shown. */
data class VerseRef(
  val book: Int,
  val chapter: Int,
  val verse: Int,
  val verseEnd: Int? = null,
)

/** Abbreviation shown to the user; Giobbe uses "Gb" instead of the DB code. */
val BookRef.displayAbbr: String
  get() = if (n == 18) "Gb" else abbr
