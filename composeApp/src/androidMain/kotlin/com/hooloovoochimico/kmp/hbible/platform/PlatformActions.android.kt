package com.hooloovoochimico.kmp.hbible.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun shareText(text: String) {
    val context = AndroidAppContext.appContext
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    // Il context è l'Application (non un'Activity): il flag NEW_TASK va sull'intent
    // effettivamente avviato, cioè il chooser (createChooser non copia i flag).
    val chooser = Intent.createChooser(intent, "Condividi").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

actual fun copyToClipboard(text: String) {
    val context = AndroidAppContext.appContext
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("versetti", text))
}

actual fun openUrl(url: String) {
    val context = AndroidAppContext.appContext
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

actual fun toast(message: String) {
    Toast.makeText(AndroidAppContext.appContext, message, Toast.LENGTH_SHORT).show()
}

actual fun formatDate(epochMillis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.ITALIAN).format(Date(epochMillis))
