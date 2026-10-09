package com.hooloovoochimico.kmp.hbible.platform

import android.content.pm.ApplicationInfo
import android.os.Build

private fun packageInfo() =
    AndroidAppContext.appContext.let { it.packageManager.getPackageInfo(it.packageName, 0) }

actual fun appVersionCode(): Int {
    val info = packageInfo()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
    else @Suppress("DEPRECATION") info.versionCode
}

actual fun appVersion(): String = "${packageInfo().versionName} (${appVersionCode()})"

actual fun isDebugBuild(): Boolean =
    (AndroidAppContext.appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

actual fun storePageUrl(): String? =
    "https://play.google.com/store/apps/details?id=${AndroidAppContext.appContext.packageName}"
