package com.hooloovoochimico.kmp.hbible.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.PrepackagedSQLiteDriver
import kotlinx.coroutines.Dispatchers

// Stesso driver di iOS: SQLite bundled (versione identica su tutte le piattaforme,
// indipendente da quella del dispositivo) → migrazioni e transazioni in commonMain.
// Al primo avvio il driver installa il DB pre-costruito; i DB già presenti si
// aprono così come sono.
actual fun bibleDatabaseBuilder(): RoomDatabase.Builder<BibleDatabase> =
  Room.databaseBuilder(AndroidAppContext.appContext, BibleDatabase::class.java, BibleDatabase.DB_NAME)
    .setDriver(PrepackagedSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
