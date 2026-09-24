package com.hooloovoochimico.kmp.hbible.data.local

// Su native withTransaction è un membro di RoomDatabase (con SQLiteTransactionType
// opzionale): la chiamata come membro vince sull'extension actual dichiarata qui.
actual suspend fun <R> BibleDatabase.withTransaction(block: suspend () -> R): R =
    withTransaction { block() }
