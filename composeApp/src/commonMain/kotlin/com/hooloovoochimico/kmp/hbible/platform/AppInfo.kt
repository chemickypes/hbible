package com.hooloovoochimico.kmp.hbible.platform

/** Version shown in Settings, e.g. "1.0 (1)" (versionName + build number). */
expect fun appVersion(): String

/** Build number (Android versionCode, iOS CFBundleVersion), compared with the content `minApp`. */
expect fun appVersionCode(): Int

/** True in debug builds: developer-only settings (content server URL) are shown only here. */
expect fun isDebugBuild(): Boolean

/** Store page of the app for "Valuta l'app"; null where there is none (iOS for now). */
expect fun storePageUrl(): String?
