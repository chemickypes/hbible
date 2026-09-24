package com.hooloovoochimico.kmp.hbible.data.local

/**
 * Transazione sospesa con accesso al db (come il sorgente usa
 * `androidx.room.withTransaction`). Su Android è un extension top-level di
 * room-ktx/room-runtime; su native l'API è un membro con firma diversa →
 * seam expect/actual.
 */
expect suspend fun <R> BibleDatabase.withTransaction(block: suspend () -> R): R
