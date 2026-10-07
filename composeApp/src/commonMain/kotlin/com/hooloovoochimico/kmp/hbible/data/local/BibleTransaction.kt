package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.useWriterConnection

/**
 * Transazione sospesa di scrittura, unica per tutte le piattaforme (entrambe
 * usano BundledSQLiteDriver): `RoomDatabase.withTransaction` di room-ktx esiste
 * solo su Android e solo senza driver, l'API multiplatform è
 * `useWriterConnection { transactor -> transactor.withTransaction(type) { … } }`.
 *
 * Attenzione: la chiamata interna DEVE avere receiver `transactor` (membro di
 * Transactor). Un `withTransaction { }` non qualificato con receiver
 * BibleDatabase risolverebbe su questa stessa extension → ricorsione infinita
 * (stack overflow visto al primo avvio iOS, PLAN §12 2026-10-02).
 *
 * IMMEDIATE: le transazioni sono bulk-write (import e sync), il lock di
 * scrittura si prende subito, senza upgrade read→write.
 */
suspend fun <R> BibleDatabase.withTransaction(block: suspend () -> R): R =
  useWriterConnection { transactor ->
    transactor.withTransaction(SQLiteTransactionType.IMMEDIATE) { block() }
  }
