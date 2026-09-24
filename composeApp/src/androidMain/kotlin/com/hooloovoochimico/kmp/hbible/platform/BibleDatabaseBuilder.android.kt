package com.hooloovoochimico.kmp.hbible.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase

actual fun bibleDatabaseBuilder(): RoomDatabase.Builder<BibleDatabase> =
  Room.databaseBuilder(AndroidAppContext.appContext, BibleDatabase::class.java, BibleDatabase.DB_NAME)
