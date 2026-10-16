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

const val NTA_REPO_URL = "https://github.com/NTA-nuova-traduzione-aperta/bibbia"
const val NTA_CHANGES_URL = "https://github.com/NTA-nuova-traduzione-aperta/bibbia/tree/main/modifiche"
const val NTA_SITE_URL = "https://nta-nuova-traduzione-aperta.github.io"
const val CC_BY_SA_4_URL = "https://creativecommons.org/licenses/by-sa/4.0/deed.it"

/** Metadata of the bundled translations, in the app's display order (same as TRANSLATION_NAMES). */
val TRANSLATION_META: List<TranslationMeta> =
  listOf(
    TranslationMeta(
      abbr = "NTA",
      name = "Nuova Traduzione Aperta",
      fullName = "Nuova Traduzione Aperta (NTA)",
      year = "2026",
      license = "CC BY-SA 4.0",
      source = NTA_SITE_URL,
      licenseUrl = CC_BY_SA_4_URL,
      note =
        "Traduzione italiana aperta: si può copiare, adattare e ridistribuire con la stessa licenza. " +
          "Nasce dalla revisione della Open Translation Bible (openbible.uk), edizione italiana, " +
          "confrontata con i testi originali; testo, note e storico delle modifiche in $NTA_REPO_URL.",
      attribution =
        "Nuova Traduzione Aperta (NTA), CC BY-SA 4.0, derivata dalla Open Translation Bible (openbible.uk). " +
          "Modifiche in $NTA_CHANGES_URL",
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
