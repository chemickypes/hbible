package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.execSQL
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hooloovoochimico.kmp.hbible.platform.installBundledDatabase

/** DB pre-costruito fra le risorse dell'app (generato da tools/build_bible_db.py). */
internal const val BUNDLED_DB_RESOURCE = "files/bible.db"

/**
 * Driver dell'app: SQLite bundled (stessa versione su Android e iOS) che, alla
 * prima apertura di un DB non ancora esistente, ci copia il DB pre-costruito.
 *
 * Room non supporta createFromAsset insieme a un SQLiteDriver; farlo qui è
 * equivalente e senza gare: Room chiama open() sul proprio thread di query
 * (mai il main thread) e la prima apertura configura il DB sotto il suo lock,
 * quindi nessuna query vede il file prima che la copia sia completa.
 */
class PrepackagedSQLiteDriver(
  private val delegate: SQLiteDriver = BundledSQLiteDriver(),
  private val install: (dbPath: String) -> Unit = ::installBundledDatabase,
) : SQLiteDriver {
  override val hasConnectionPool: Boolean
    get() = delegate.hasConnectionPool

  override fun open(fileName: String): SQLiteConnection {
    install(fileName)
    return delegate.open(fileName)
  }
}

/**
 * Copia nel DB utente le righe incluse nell'app che mancano: le traduzioni
 * [translations] e le tabelle [tables] vuote (es. una traduzione nuova arrivata
 * con un aggiornamento, o il lessico svuotato da una migrazione). Il DB incluso
 * ha lo stesso schema dell'app (generato dall'ultimo schema Room), quindi
 * `SELECT *` allinea le colonne.
 */
internal suspend fun BibleDatabase.copyFromBundled(
  bundledPath: String,
  translations: List<String>,
  tables: List<String>,
) {
  useWriterConnection { connection ->
    // ATTACH/DETACH non sono ammessi dentro una transazione.
    connection.usePrepared("ATTACH DATABASE ? AS bundled") { statement ->
      statement.bindText(1, bundledPath)
      statement.step()
    }
    try {
      connection.withTransaction(SQLiteTransactionType.IMMEDIATE) {
        for (code in translations) {
          usePrepared("INSERT OR REPLACE INTO main.verses SELECT * FROM bundled.verses WHERE translation = ?") {
            it.bindText(1, code)
            it.step()
          }
        }
        for (table in tables) {
          execSQL("INSERT OR REPLACE INTO main.$table SELECT * FROM bundled.$table")
        }
      }
    } finally {
      connection.execSQL("DETACH DATABASE bundled")
    }
  }
}
