package com.hooloovoochimico.kmp.hbible.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeIntervalSinceReferenceDate
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
actual fun shareText(text: String) {
    val controller = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    controller.presentOnKeyWindow()
}

@OptIn(ExperimentalForeignApi::class)
actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

@OptIn(ExperimentalForeignApi::class)
actual fun readClipboardText(): String? = UIPasteboard.generalPasteboard.string

@OptIn(ExperimentalForeignApi::class)
actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

// Toast: no-op su iOS (piano §9, HUD leggero rimandato a Fase 6 se serve).
actual fun toast(message: String) {
}

@OptIn(ExperimentalForeignApi::class)
actual fun formatDate(epochMillis: Long, pattern: String): String {
    val formatter = NSDateFormatter()
    // Locale italiano, identico al SimpleDateFormat(Locale.ITALIAN) del sorgente.
    formatter.locale = NSLocale.localeWithLocaleIdentifier("it_IT")
    formatter.dateFormat = pattern
    // NSTimeIntervalSince1970 = secondi tra il 1970 e la reference date (2001).
    val date = NSDate(timeIntervalSinceReferenceDate = epochMillis / 1000.0 - 978_307_200.0)
    return formatter.stringFromDate(date)
}

/**
 * Presenta un view controller sulla finestra chiave dell'app (interop UIKit
 * diretto). Nota klib K/N 2.3.20: `UIApplication.keyWindow` è tipizzato `Any?`
 * e `windows` è `List<*>` → cast espliciti; `rootViewController` e
 * `presentedViewController` risolvono sui tipi reali (lezione F1).
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIViewController.presentOnKeyWindow() {
    val window = UIApplication.sharedApplication.keyWindow as? UIWindow
    var presenter = window?.rootViewController
    // Risali al presenter per non coprire un eventuale view controller già presentato.
    while (presenter?.presentedViewController != null) {
        presenter = presenter.presentedViewController
    }
    presenter?.presentViewController(this, animated = true, completion = null)
}
