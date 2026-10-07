package com.hooloovoochimico.kmp.hbible.platform

import com.hooloovoochimico.kmp.hbible.data.local.BUNDLED_DB_RESOURCE
import com.hooloovoochimico.kmp.hbible.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSURL

private val installLock = NSLock()

/** Percorso del DB incluso nel bundle dell'app (Compose Resources). */
private fun bundledDatabasePath(): String =
  requireNotNull(NSURL(string = Res.getUri(BUNDLED_DB_RESOURCE)).path) { "DB incluso non trovato nel bundle" }

@OptIn(ExperimentalForeignApi::class)
actual fun installBundledDatabase(dbPath: String) {
  installLock.lock()
  try {
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(dbPath)) return
    fm.removeItemAtPath("$dbPath-wal", null)
    fm.removeItemAtPath("$dbPath-shm", null)
    // Sullo stesso volume APFS la copia è un clone: istantanea.
    val tmp = "$dbPath.bundled-tmp"
    fm.removeItemAtPath(tmp, null)
    check(fm.copyItemAtPath(bundledDatabasePath(), toPath = tmp, error = null)) {
      "Impossibile copiare il DB incluso"
    }
    check(fm.moveItemAtPath(tmp, toPath = dbPath, error = null)) { "Impossibile installare il DB incluso in $dbPath" }
  } finally {
    installLock.unlock()
  }
}

// Il file nel bundle è di sola lettura ma ATTACH lo apre comunque in lettura.
actual suspend fun <R> withBundledDatabaseFile(block: suspend (path: String) -> R): R = block(bundledDatabasePath())
