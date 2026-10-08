package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Copertura minima del [BibleDao] sullo stesso driver dell'app
 * (BundledSQLiteDriver): ordinamento capitolo, conteggi, occorrenze di lemma
 * e ciclo di vita delle note (NEXT_STEPS punto 5b).
 *
 * Esecuzione (sull'emulatore, cancella i dati dell'app):
 * `ANDROID_SERIAL=emulator-5554 ./gradlew :composeApp:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class BibleDaoTest {

  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val dbFile = instrumentation.targetContext.getDatabasePath(TEST_DB)

  @Before
  fun deleteLeftoverDatabase() {
    listOf("", "-wal", "-shm").forEach { suffix -> File(dbFile.path + suffix).delete() }
  }

  private fun buildDb(): BibleDatabase =
    Room.databaseBuilder(instrumentation.targetContext, BibleDatabase::class.java, TEST_DB)
      .setDriver(BundledSQLiteDriver())
      .build()

  @Test
  fun chapter_returnsVersesOrderedByVerse() = runBlocking {
    val db = buildDb()
    val dao = db.bibleDao()
    val verses =
      listOf(
        VerseEntity("NR", 1, 1, 3, null, "c"),
        VerseEntity("NR", 1, 1, 1, null, "a"),
        VerseEntity("NR", 1, 1, 2, null, "b"),
      )
    dao.insertVerses(verses)

    val chapter = dao.chapter("NR", 1, 1).first()

    assertEquals(listOf(1, 2, 3), chapter.map { it.verse })
    db.close()
  }

  @Test
  fun translationVerseCount_countsOnlyThatTranslation() = runBlocking {
    val db = buildDb()
    val dao = db.bibleDao()
    dao.insertVerses(
      listOf(
        VerseEntity("NR", 1, 1, 1, null, "a"),
        VerseEntity("NR", 1, 1, 2, null, "b"),
        VerseEntity("R2", 1, 1, 1, null, "c"),
      ),
    )

    assertEquals(2, dao.translationVerseCount("NR"))
    assertEquals(1, dao.translationVerseCount("R2"))
    db.close()
  }

  @Test
  fun lemmaOccurrences_matchesSpacePaddedNumbers() = runBlocking {
    val db = buildDb()
    val dao = db.bibleDao()
    dao.insertOriginalVerses(
      listOf(
        OriginalVerseEntity(
          book = 1,
          chapter = 1,
          verse = 1,
          lang = "he",
          text = "bereshit bara",
          transliteration = "bereshit bara",
          lemmas = " h430 h1254 ",
        ),
        OriginalVerseEntity(
          book = 1,
          chapter = 1,
          verse = 2,
          lang = "he",
          text = "altro",
          transliteration = "altro",
          lemmas = " h1254 ",
        ),
      ),
    )

    // h430 compare nel versetto 1 (non nel 2): pattern con spazi, come in app.
    val occurrences = dao.lemmaOccurrences(lang = "he", pattern = "% h430 %", 0, 0, 0, 300)

    assertEquals(listOf(VerseRefRow(1, 1, 1)), occurrences)
    db.close()
  }

  @Test
  fun notes_insertUpdateDeleteOrderedByUpdatedAt() = runBlocking {
    val db = buildDb()
    val dao = db.bibleDao()

    val first = dao.insertNote(NoteEntity(title = "Prima", content = "uno", createdAt = 90, updatedAt = 100))
    val second = dao.insertNote(NoteEntity(title = "Seconda", content = "due", createdAt = 190, updatedAt = 200))
    assertTrue(first > 0 && second > 0)

    // L'elenco è ordinato per updatedAt discendente: la più recente prima.
    assertEquals(listOf("Seconda", "Prima"), dao.notes().first().map { it.title })

    dao.insertNote(NoteEntity(id = first, title = "Prima", content = "uno aggiornato", createdAt = 90, updatedAt = 300))
    val updated = dao.note(first)
    assertEquals("uno aggiornato", updated?.content)
    assertEquals(listOf("Prima", "Seconda"), dao.notes().first().map { it.title })

    dao.deleteNote(first)
    assertNull(dao.note(first))
    assertEquals(listOf("Seconda"), dao.notes().first().map { it.title })
    db.close()
  }

  private companion object {
    const val TEST_DB = "dao-test.db"
  }
}
