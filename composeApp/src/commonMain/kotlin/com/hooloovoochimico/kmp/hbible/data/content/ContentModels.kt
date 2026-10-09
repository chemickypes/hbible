package com.hooloovoochimico.kmp.hbible.data.content

import kotlinx.serialization.Serializable

/** `/content/manifest.json` served by the CMS (see bibbia-interlineare-project). */
@Serializable
data class ContentManifest(
  val version: String = "",
  val generatedAt: String = "",
  val packages: Map<String, PackageInfo> = emptyMap(),
  /** Package format versions (originals 2 = alignments in the generic `al` field). */
  val formats: Map<String, Int> = emptyMap(),
  /** Packages no longer published (e.g. translations moved to the CMS "cassetto"). */
  val removed: List<String> = emptyList(),
  /** Lowest app versionCode that can read these packages (older apps skip the update). */
  val minApp: Int = 0,
)

/** Format of `originals/{nn}.json` this app understands (see [ContentManifest.formats]). */
const val ORIGINALS_FORMAT = 2

/** One package entry in the manifest: SHA-256 of the file content. */
@Serializable
data class PackageInfo(
  val hash: String,
  val size: Long = 0,
)

/** Result of a manifest diff against the locally applied packages. */
data class UpdateCheck(
  /** Remote manifest version, null when the check failed. */
  val version: String?,
  /** Package paths whose remote hash differs from the local state. */
  val changed: List<String>,
  /** Non-null when the check could not complete. */
  val error: String?,
  /** The published packages need a newer app version: nothing is downloaded. */
  val appUpdateRequired: Boolean = false,
)

/** Result of a full sync run. */
data class SyncResult(
  val version: String,
  val applied: List<String>,
  val failed: Map<String, String>,
  val appUpdateRequired: Boolean = false,
)
