package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.useWriterConnection

// Su native NON esiste `RoomDatabase.withTransaction` (quella è un'API room-ktx
// solo JVM/Android). L'API pubblica multiplatform per una transazione sospesa è:
//   db.useWriterConnection { transactor -> transactor.withTransaction(type) { ... } }
// dove `withTransaction` è un MEMBRO di Transactor (room-runtime 2.8.5, Transactor.kt).
//
// Attenzione: dentro questa extension il chiamante ha receiver BibleDatabase, quindi
// la chiamata su `transactor` (Transactor) risolve sempre sul membro di Transactor —
// mai su questa stessa extension. Un `withTransaction { }` non qualificato qui dentro
// risolverebbe invece su questa extension → ricorsione infinita → stack overflow
// (EXC_BAD_ACCESS code=2 sul thread DB, visto al primo avvio iOS).
//
// IMMEDIATE: le transazioni sono bulk-write (import chunked di ~30MB), nessun
// upgrade read→write dentro la transazione, lock di scrittura preso subito
// (equivalente semantico del beginTransaction di Room su Android).
actual suspend fun <R> BibleDatabase.withTransaction(block: suspend () -> R): R =
  useWriterConnection { transactor ->
    transactor.withTransaction(SQLiteTransactionType.IMMEDIATE) { block() }
  }
