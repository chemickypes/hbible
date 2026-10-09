package com.hooloovoochimico.kmp.hbible.data

import io.ktor.http.encodeURLParameter

/** Public repository of the app (GPL-3.0), generated from the private development repo. */
const val SOURCE_CODE_URL = "https://github.com/chemickypes/hbible"

/** Feedback goes through the issues of the public repository. */
const val FEEDBACK_URL = "$SOURCE_CODE_URL/issues/new"

/** Privacy policy published with the content site on GitHub Pages (task R08). */
const val PRIVACY_POLICY_URL = "https://chemickypes.github.io/hbible/privacy/"

/** License of the app code. */
const val APP_LICENSE = "GPL-3.0"
const val APP_LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * Issue of the public repo, already filled in, to report AI-generated content (Google Play
 * policy on generative AI). The user reviews and sends it from the browser: nothing is sent
 * by the app. GitHub issues are public, the privacy policy says so.
 */
fun aiReportUrl(content: String, where: String): String {
  val quoted = content.trim().take(1500).lines().joinToString("\n") { "> $it" }
  val body =
    "Segnalo questo contenuto generato dall'AI ($where) come inappropriato o sbagliato.\n\n" +
      "$quoted\n\nMotivo: "
  return "$FEEDBACK_URL?title=${"Segnalazione contenuto AI".encodeURLParameter()}&body=${body.encodeURLParameter()}"
}
