package com.hooloovoochimico.kmp.hbible.platform

import com.hooloovoochimico.kmp.hbible.data.local.BUNDLED_DB_RESOURCE
import com.hooloovoochimico.kmp.hbible.resources.Res
import java.io.File
import java.io.InputStream
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val installLock = Any()

/** Stream del DB incluso: le Compose Resources su Android stanno negli asset. */
private fun openBundledDatabase(): InputStream {
  val uri = Res.getUri(BUNDLED_DB_RESOURCE)
  val assetPath = uri.substringAfter("file:///android_asset/", missingDelimiterValue = "")
  return if (assetPath.isNotEmpty()) {
    AndroidAppContext.appContext.assets.open(assetPath)
  } else {
    URI(uri).toURL().openStream()
  }
}

/** Copia atomica: file temporaneo nella stessa cartella, poi rename. */
private fun copyBundledDatabaseTo(target: File) {
  target.parentFile?.mkdirs()
  val tmp = File(target.parentFile, "${target.name}.bundled-tmp")
  openBundledDatabase().use { input -> tmp.outputStream().use { input.copyTo(it, 1 shl 16) } }
  check(tmp.renameTo(target)) { "Impossibile installare il DB incluso in ${target.path}" }
}

actual fun installBundledDatabase(dbPath: String) {
  synchronized(installLock) {
    val db = File(dbPath)
    if (db.exists()) return
    // WAL/SHM orfani di un DB cancellato non devono finire sul file nuovo.
    File("$dbPath-wal").delete()
    File("$dbPath-shm").delete()
    copyBundledDatabaseTo(db)
  }
}

actual suspend fun <R> withBundledDatabaseFile(block: suspend (path: String) -> R): R {
  val file = File(AndroidAppContext.appContext.cacheDir, "bundled-bible.db")
  withContext(Dispatchers.IO) {
    file.delete()
    copyBundledDatabaseTo(file)
  }
  return try {
    block(file.path)
  } finally {
    file.delete()
  }
}
