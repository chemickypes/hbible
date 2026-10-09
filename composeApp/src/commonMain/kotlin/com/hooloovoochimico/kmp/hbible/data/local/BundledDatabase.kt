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

/** Riga di `content_state` con l'impronta dei contenuti del DB incluso (tools/build_bible_db.py). */
internal const val BUNDLED_STATE_ID = "bundled"

/** Tabelle dei contenuti che vengono dal DB incluso (le note, il VOTD, i post e le info libro no). */
private val BUNDLED_CONTENT_TABLES =
  listOf("books", "original_verses", "original_alignments", "lexemes", "cross_references")

/**
 * Se il DB incluso nell'APK ha contenuti diversi da quelli già applicati
 * (impronta in `content_state`, riga [BUNDLED_STATE_ID]), ricopia dal DB incluso
 * libri, traduzioni incluse [translations], originali, allineamenti, lessico e
 * rimandi. Serve perché un aggiornamento dell'app porti i contenuti corretti nel
 * CMS anche a chi l'aveva già installata. Note, versetto del giorno, post e info
 * libro non si toccano. Lo stato di sync dei pacchetti ricopiati si azzera, così
 * un CMS configurato li ricontrolla. Ritorna true se ha ricopiato.
 */
internal suspend fun BibleDatabase.refreshFromBundledIfChanged(
  bundledPath: String,
  translations: List<String>,
): Boolean =
  useWriterConnection { connection ->
    connection.usePrepared("ATTACH DATABASE ? AS bundled") { statement ->
      statement.bindText(1, bundledPath)
      statement.step()
    }
    try {
      suspend fun query(schema: String): String? =
        connection.usePrepared("SELECT hash FROM $schema.content_state WHERE package_id = ?") {
          it.bindText(1, BUNDLED_STATE_ID)
          if (it.step()) it.getText(0) else null
        }
      val bundledHash = query("bundled") ?: return@useWriterConnection false
      if (bundledHash == query("main")) return@useWriterConnection false
      connection.withTransaction(SQLiteTransactionType.IMMEDIATE) {
        for (table in BUNDLED_CONTENT_TABLES) {
          execSQL("DELETE FROM main.$table")
          execSQL("INSERT INTO main.$table SELECT * FROM bundled.$table")
        }
        for (code in translations) {
          usePrepared("DELETE FROM main.verses WHERE translation = ?") {
            it.bindText(1, code)
            it.step()
          }
          usePrepared("INSERT INTO main.verses SELECT * FROM bundled.verses WHERE translation = ?") {
            it.bindText(1, code)
            it.step()
          }
        }
        execSQL(
          "DELETE FROM main.content_state WHERE package_id LIKE 'bible/%' OR package_id LIKE 'originals/%' " +
            "OR package_id IN ('lexicon.json', 'crossrefs.json')",
        )
        execSQL(
          "INSERT OR REPLACE INTO main.content_state SELECT * FROM bundled.content_state WHERE package_id = '$BUNDLED_STATE_ID'",
        )
      }
      true
    } finally {
      connection.execSQL("DETACH DATABASE bundled")
    }
  }
