package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrazioni Room sullo stesso driver dell'app (BundledSQLiteDriver).
 *
 * Ogni versione committata (8, 9, 11, 12, 13, 15) viene creata dal suo schema
 * esportato (composeApp/schemas/) e portata all'ultima con [ALL_MIGRATIONS];
 * `runMigrationsAndValidate` confronta il risultato con lo schema corrente.
 * È il controllo che avrebbe intercettato `published_at INTEGER` vs `String`
 * della 14→15 (PLAN §12, 2026-10-02).
 *
 * Esecuzione: `./gradlew :composeApp:connectedDebugAndroidTest` (serve un device).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val dbFile = instrumentation.targetContext.getDatabasePath(TEST_DB)

  @get:Rule
  val helper =
    MigrationTestHelper(
      instrumentation = instrumentation,
      file = dbFile,
      driver = BundledSQLiteDriver(),
      databaseClass = BibleDatabase::class,
    )

  /**
   * In modalità driver l'helper non elimina il file lasciato dal test precedente
   * (già all'ultima versione): senza pulizia createDatabase(n) chiederebbe una
   * migrazione 15→n. Si rimuovono anche i file WAL/SHM.
   */
  @Before
  fun deleteLeftoverDatabase() {
    listOf("", "-wal", "-shm").forEach { suffix -> java.io.File(dbFile.path + suffix).delete() }
  }

  @Test fun migrate8ToLatest() = migrateToLatest(8)

  @Test fun migrate9ToLatest() = migrateToLatest(9)

  @Test fun migrate11ToLatest() = migrateToLatest(11)

  @Test fun migrate12ToLatest() = migrateToLatest(12)

  @Test fun migrate13ToLatest() = migrateToLatest(13)

  @Test fun migrate15ToLatest() = migrateToLatest(15)

  /**
   * 15→16: via le traduzioni a licenza chiusa (versetti, metadati, stato di
   * sync), le aperte restano; original_verses/books svuotate (le ricopia
   * ensureImported dal DB incluso), stato di sync degli originali azzerato.
   */
  @Test
  fun migration16RemovesClosedTranslations() {
    helper.createDatabase(15).use { connection ->
      for (code in listOf("NR", "R2", "ND", "CEI", "RIC", "R27", "DIO")) {
        connection.execSQL(
          "INSERT INTO verses (translation, book, chapter, verse, title, text) VALUES ('$code', 1, 1, 1, NULL, 'x')",
        )
      }
      connection.execSQL(
        "INSERT INTO translation_meta (abbr, name, description, publisher, year, copyright) " +
          "VALUES ('NR', 'Nuova Riveduta', '', '', '', '')",
      )
      for (p in listOf("bible/NR.json", "bible/R27.json", "originals/01.json")) {
        connection.execSQL("INSERT INTO content_state (package_id, hash, version, synced_at) VALUES ('$p', 'h', 'v', 0)")
      }
      connection.execSQL("INSERT INTO books (n, name, abbr, chapters) VALUES (1, 'Genesi', 'Gn', 50)")
    }

    helper.runMigrationsAndValidate(16, ALL_MIGRATIONS.toList()).use { connection ->
      connection.prepare("SELECT translation FROM verses ORDER BY translation").use {
        val left = mutableListOf<String>()
        while (it.step()) left += it.getText(0)
        assertEquals(listOf("DIO", "R27"), left)
      }
      connection.prepare("SELECT paragraph FROM verses LIMIT 1").use {
        check(it.step())
        assertEquals(0L, it.getLong(0))
      }
      assertEquals(0L, count(connection, "SELECT COUNT(*) FROM translation_meta"))
      assertEquals(1L, count(connection, "SELECT COUNT(*) FROM content_state"))
      assertEquals(0L, count(connection, "SELECT COUNT(*) FROM books"))
      assertEquals(0L, count(connection, "SELECT COUNT(*) FROM original_alignments"))
    }
  }

  private fun count(connection: androidx.sqlite.SQLiteConnection, sql: String): Long =
    connection.prepare(sql).use {
      check(it.step())
      it.getLong(0)
    }

  /** Le note dell'utente devono sopravvivere all'intera catena di migrazioni. */
  @Test
  fun notesSurviveAllMigrations() {
    helper.createDatabase(OLDEST_VERSION).use { connection ->
      connection.execSQL(
        "INSERT INTO notes (id, title, content, styles, book, chapter, verse, createdAt, updatedAt) " +
          "VALUES (1, 'Titolo', 'Contenuto', '', 43, 3, 16, 100, 200)",
      )
      connection.execSQL(
        "INSERT INTO book_info (book, context_text, protagonists, christocentric, model, updatedAt) " +
          "VALUES (1, 'contesto', 'protagonisti', 'cristo', 'modello', 300)",
      )
    }

    helper.runMigrationsAndValidate(LATEST_VERSION, ALL_MIGRATIONS.toList()).use { connection ->
      connection.prepare("SELECT title, content, book, chapter, verse, updatedAt FROM notes WHERE id = 1").use {
        check(it.step()) { "Nota persa dopo la migrazione" }
        assertEquals("Titolo", it.getText(0))
        assertEquals("Contenuto", it.getText(1))
        assertEquals(43L, it.getLong(2))
        assertEquals(3L, it.getLong(3))
        assertEquals(16L, it.getLong(4))
        assertEquals(200L, it.getLong(5))
      }
      connection.prepare("SELECT context_text FROM book_info WHERE book = 1").use {
        check(it.step()) { "Info libro persa dopo la migrazione" }
        assertEquals("contesto", it.getText(0))
      }
    }
  }

  private fun migrateToLatest(from: Int) {
    helper.createDatabase(from).close()
    helper.runMigrationsAndValidate(LATEST_VERSION, ALL_MIGRATIONS.toList()).close()
  }

  private companion object {
    const val TEST_DB = "migration-test.db"
    const val OLDEST_VERSION = 8
    const val LATEST_VERSION = 16
  }
}
