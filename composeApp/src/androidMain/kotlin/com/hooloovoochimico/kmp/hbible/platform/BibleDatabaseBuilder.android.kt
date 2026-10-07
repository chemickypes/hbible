package com.hooloovoochimico.kmp.hbible.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import kotlinx.coroutines.Dispatchers

// Stesso driver di iOS: SQLite bundled (versione identica su tutte le piattaforme,
// indipendente da quella del dispositivo) → migrazioni e transazioni in commonMain.
// Il file bible.db è lo stesso formato SQLite: i DB già installati si aprono così come sono.
actual fun bibleDatabaseBuilder(): RoomDatabase.Builder<BibleDatabase> =
  Room.databaseBuilder(AndroidAppContext.appContext, BibleDatabase::class.java, BibleDatabase.DB_NAME)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
