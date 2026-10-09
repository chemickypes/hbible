package com.hooloovoochimico.kmp.hbible.platform

import kotlin.experimental.ExperimentalNativeApi
import platform.Foundation.NSBundle

actual fun appVersion(): String {
    val info = NSBundle.mainBundle.infoDictionary
    val name = info?.get("CFBundleShortVersionString") as? String ?: "?"
    val build = info?.get("CFBundleVersion") as? String ?: "?"
    return "$name ($build)"
}

actual fun appVersionCode(): Int =
    (NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String)?.toIntOrNull() ?: 0

@OptIn(ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean = Platform.isDebugBinary

// Nessuna scheda App Store per ora: la voce "Valuta l'app" non compare.
actual fun storePageUrl(): String? = null
