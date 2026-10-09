package com.hooloovoochimico.kmp.hbible.platform

import android.content.pm.ApplicationInfo
import android.os.Build

actual fun appVersion(): String {
    val context = AndroidAppContext.appContext
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    return "${info.versionName} ($code)"
}

actual fun isDebugBuild(): Boolean =
    (AndroidAppContext.appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

actual fun storePageUrl(): String? =
    "https://play.google.com/store/apps/details?id=${AndroidAppContext.appContext.packageName}"
