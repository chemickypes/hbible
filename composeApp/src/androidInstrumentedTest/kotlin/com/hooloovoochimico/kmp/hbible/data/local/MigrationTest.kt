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
 * Ogni versione committata (8, 9, 11, 12, 13) viene creata dal suo schema
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
    const val LATEST_VERSION = 15
  }
}
