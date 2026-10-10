package com.hooloovoochimico.kmp.hbible.platform

/** Condivisione di testo con il chooser di sistema. */
expect fun shareText(text: String)

/** Copia negli appunti di sistema. */
expect fun copyToClipboard(text: String)

/** Testo negli appunti di sistema (null se vuoti o non testuali). */
expect fun readClipboardText(): String?

/** Apre un URL esterno (browser/mappa/mail secondo lo schema). */
expect fun openUrl(url: String)

/** Messaggio breve transitorio (Toast su Android, no-op su iOS: piano §9). */
expect fun toast(message: String)

/**
 * Data formattata secondo il pattern (sintassi compatibile SimpleDateFormat /
 * NSDateFormatter) e locale italiano, come nel sorgente. Il pattern è
 * parametro perché gli schermi del sorgente usano formati diversi.
 */
expect fun formatDate(epochMillis: Long, pattern: String): String
