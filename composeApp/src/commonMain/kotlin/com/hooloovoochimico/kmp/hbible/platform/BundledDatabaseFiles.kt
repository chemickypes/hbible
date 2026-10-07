package com.hooloovoochimico.kmp.hbible.platform

/**
 * Se [dbPath] non esiste, ci copia il DB pre-costruito incluso nell'app
 * (risorsa files/bible.db). Sincrona e thread-safe: la chiama il driver SQLite
 * alla prima apertura, sul thread di Room. Un DB già esistente non viene mai
 * toccato (note e dati dell'utente).
 */
expect fun installBundledDatabase(dbPath: String)

/**
 * Esegue [block] con il percorso di un file leggibile del DB incluso: su
 * Android l'asset viene estratto in cache e cancellato dopo, su iOS è il file
 * nel bundle dell'app.
 */
expect suspend fun <R> withBundledDatabaseFile(block: suspend (path: String) -> R): R
