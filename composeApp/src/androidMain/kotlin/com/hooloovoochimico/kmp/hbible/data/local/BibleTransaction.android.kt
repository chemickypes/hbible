package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.migration.Migration
import androidx.room.withTransaction as roomWithTransaction

actual suspend fun <R> BibleDatabase.withTransaction(block: suspend () -> R): R =
  roomWithTransaction { block() }
