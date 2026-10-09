package com.hooloovoochimico.kmp.hbible.data

/**
 * Metadata of a bundled Italian translation, shown in Settings → "Versioni
 * della Bibbia" and in the credits (name, year, license, source, note).
 * Only openly licensed texts are bundled.
 */
data class TranslationMeta(
  val abbr: String,
  val name: String,
  val fullName: String,
  val year: String,
  val license: String,
  val source: String,
  val note: String,
  /** License deed URL, when the license has one (CC). */
  val licenseUrl: String = "",
  /** Credits line required or suggested by the license. */
  val attribution: String = "",
)

const val OTB_FORK_URL = "https://github.com/chemickypes/open-bible"
const val OTB_CHANGES_URL = "https://github.com/chemickypes/open-bible/tree/hbible-correzioni"
const val CC_BY_SA_4_URL = "https://creativecommons.org/licenses/by-sa/4.0/deed.it"

/** Metadata of the bundled translations, in the app's display order (same as TRANSLATION_NAMES). */
val TRANSLATION_META: List<TranslationMeta> =
  listOf(
    TranslationMeta(
      abbr = "OTB",
      name = "Bibbia Aperta",
      fullName = "Open Translation Bible — edizione italiana",
      year = "2026",
      license = "CC BY-SA 4.0",
      source = "https://openbible.uk",
      licenseUrl = CC_BY_SA_4_URL,
      note =
        "La prima Bibbia open source: si può copiare, adattare e ridistribuire con la stessa licenza. " +
          "Numerazione dei versetti ricondotta a quella delle altre versioni dell'app; " +
          "testo adattato (correzioni pubblicate nel fork $OTB_FORK_URL).",
      attribution =
        "Bibbia Aperta © Open Translation Bible (openbible.uk), CC BY-SA 4.0. " +
          "Testo adattato da HBible: modifiche in $OTB_CHANGES_URL",
    ),
    TranslationMeta(
      abbr = "R27",
      name = "Riveduta 1927",
      fullName = "Riveduta 1927 (Giovanni Luzzi)",
      year = "1927",
      license = "Pubblico dominio",
      source = "Società Biblica Britannica e Forestiera",
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
      abbr = "MAR",
      name = "Martini",
      fullName = "Antonio Martini 1781",
      year = "1781",
      license = "Pubblico dominio",
      source = "http://www.laparola.net",
      // Rinumerazione dalla Vulgata: tools/mar_verse_map.json, verifica in tools/mar_review.txt.
      note =
        "Traduzione cattolica dalla Vulgata latina. La numerazione dei versetti, che segue la " +
          "Vulgata, è ricondotta a quella delle altre versioni dell'app.",
    ),
  )
