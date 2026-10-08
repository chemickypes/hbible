package com.hooloovoochimico.kmp.hbible.data

/**
 * Credits of the non-translation content shipped with the app (Settings →
 * "Crediti e licenze"). The translations' credits are in [TRANSLATION_META].
 * Source of truth for licenses: tasks/T00-risultato-licenze.md.
 */
data class CreditEntry(
  val title: String,
  val license: String,
  val url: String,
  /** What the content is used for in the app. */
  val scope: String,
)

val CREDITS: List<CreditEntry> =
  listOf(
    CreditEntry(
      title = "Open Scriptures Hebrew Bible (Westminster Leningrad Codex)",
      license = "Testo: pubblico dominio · morfologia e lemmi: CC BY 4.0",
      url = "https://github.com/openscriptures/morphhb",
      scope = "Testo ebraico dell'Antico Testamento",
    ),
    CreditEntry(
      title = "Nestle 1904 (biblicalhumanities.org)",
      license = "Testo: pubblico dominio · morfologia: CC0",
      url = "https://github.com/biblicalhumanities/Nestle1904",
      scope = "Testo greco del Nuovo Testamento",
    ),
    CreditEntry(
      title = "Dizionario di Strong (Open Scriptures)",
      license = "Pubblico dominio",
      url = "https://github.com/openscriptures/strongs",
      scope = "Lessico ebraico e greco",
    ),
    CreditEntry(
      title = "Berean Standard Bible — interlineare (Bible Hub)",
      license = "Pubblico dominio",
      url = "https://berean.bible/licensing.htm",
      scope = "Gloss inglesi parola per parola",
    ),
    CreditEntry(
      title = "OpenBible.info — riferimenti incrociati",
      license = "CC BY 4.0",
      url = "https://www.openbible.info/labs/cross-references/",
      scope = "Riferimenti incrociati",
    ),
  )

/** Credits suffix appended to shared/copied verses of [translation] (empty when not required). */
fun shareAttribution(translation: String): String =
  when (translation) {
    "OTB" -> "Bibbia Aperta, CC BY-SA 4.0"
    else -> TRANSLATION_NAMES[translation].orEmpty()
  }
