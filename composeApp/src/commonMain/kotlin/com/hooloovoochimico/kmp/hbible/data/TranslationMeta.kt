package com.hooloovoochimico.kmp.hbible.data

/**
 * Metadata of a bundled Italian translation, shown in Settings → "Versioni
 * della Bibbia" (name, year, license, source, note).
 */
data class TranslationMeta(
  val abbr: String,
  val name: String,
  val fullName: String,
  val year: String,
  val license: String,
  val source: String,
  val note: String,
  /** true = protected text included for personal use only (private repo, no distribution). */
  val personalUse: Boolean = false,
)

/** Metadata of the bundled translations, in the app's display order. */
val TRANSLATION_META: List<TranslationMeta> =
  listOf(
    TranslationMeta(
      abbr = "NR",
      name = "Nuova Riveduta",
      fullName = "Nuova Riveduta",
      year = "2006",
      license = "© Società Biblica di Ginevra",
      source = "http://www.laparola.net",
      note = "",
      personalUse = true,
    ),
    TranslationMeta(
      abbr = "R2",
      name = "Riveduta 2020",
      fullName = "Riveduta 2020",
      year = "2020",
      license = "© ADI-Media",
      source = "http://www.laparola.net",
      note = "",
      personalUse = true,
    ),
    TranslationMeta(
      abbr = "R27",
      name = "Riveduta 1927",
      fullName = "Riveduta 1927",
      year = "1927",
      license = "Pubblico dominio",
      source = "progetto bibbia-interlineare (github)",
      note = "",
    ),
    TranslationMeta(
      abbr = "DIO",
      name = "Diodati",
      fullName = "Giovanni Diodati 1607 (ed. 1877)",
      year = "1877",
      license = "Pubblico dominio",
      source = "http://www.laparola.net",
      note = "Testo integrale, versificazione standard.",
    ),
    TranslationMeta(
      abbr = "ND",
      name = "Nuova Diodati",
      fullName = "Nuova Diodati",
      year = "1991",
      license = "© La Buona Novella Inc.",
      source = "",
      note = "Revisione moderna del testo di Diodati.",
      personalUse = true,
    ),
    TranslationMeta(
      abbr = "CEI",
      name = "CEI 1974",
      fullName = "La Bibbia della CEI",
      year = "1974",
      license = "© CEI",
      source = "",
      note = "Versione ufficiale della Conferenza Episcopale Italiana; libri deuterocanonici non inclusi.",
      personalUse = true,
    ),
    TranslationMeta(
      abbr = "RIC",
      name = "Ricciotti",
      fullName = "Gioacchino Ricciotti e 7 traduttori",
      year = "1940",
      license = "© eredi Ricciotti (PD solo dal 2035)",
      source = "",
      note = "Libri deuterocanonici non inclusi.",
      personalUse = true,
    ),
    TranslationMeta(
      abbr = "MAR",
      name = "Martini",
      fullName = "Antonio Martini 1781",
      year = "1781",
      license = "Pubblico dominio",
      source = "",
      note =
        "Prima traduzione cattolica completa dall'originale; segue la versificazione della Vulgata: " +
          "i versetti sono RINUMERATI per allinearsi alla numerazione standard delle altre versioni " +
          "dell'app (mappa verificata in tools/mar_verse_map.json, audit in tools/mar_review.txt).",
    ),
  )
