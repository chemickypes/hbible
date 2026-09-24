package com.hooloovoochimico.kmp.hbible.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
  val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
    directory = NSDocumentDirectory,
    inDomain = NSUserDomainMask,
    appropriateForURL = null,
    create = false,
    error = null,
  )
  return requireNotNull(documentDirectory?.path) { "Document directory unavailable" }
}

actual fun bibleDatabaseBuilder(): RoomDatabase.Builder<BibleDatabase> =
  Room.databaseBuilder<BibleDatabase>(name = documentsDirectory() + "/" + BibleDatabase.DB_NAME)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
