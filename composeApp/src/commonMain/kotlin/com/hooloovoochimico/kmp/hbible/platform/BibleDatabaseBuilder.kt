package com.hooloovoochimico.kmp.hbible.platform

import androidx.room.RoomDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase

/**
 * Seam multiplatform (PLAN §9): costruisce il builder Room per la piattaforma
 * corrente. Le migrazioni NON sono incluse: il modulo Koin le aggiunge con
 * `addMigrations(*BibleDatabase.ALL_MIGRATIONS)`.
 */
expect fun bibleDatabaseBuilder(): RoomDatabase.Builder<BibleDatabase>
