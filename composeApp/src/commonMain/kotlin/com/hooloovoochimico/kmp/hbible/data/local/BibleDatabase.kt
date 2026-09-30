package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration

/**
 * Il [RoomDatabaseConstructor] è implementato dal compilatore Room per i target
 * native (pattern KMP ufficiale: nessun actual manuale, vedi NO_ACTUAL_FOR_EXPECT).
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BibleDatabaseConstructor : RoomDatabaseConstructor<BibleDatabase> {
    override fun initialize(): BibleDatabase
}

@ConstructedBy(BibleDatabaseConstructor::class)
@Database(
  entities = [
    BookEntity::class,
    VerseEntity::class,
    OriginalVerseEntity::class,
    CrossReferenceEntity::class,
    LexemeEntity::class,
    NoteEntity::class,
    BookInfoEntity::class,
  ],
  version = 9,
  exportSchema = false,
)
abstract class BibleDatabase : RoomDatabase() {

  abstract fun bibleDao(): BibleDao

  companion object {
    /** File name on Android (Documents/bible.db path on iOS), as in the source app. */
    const val DB_NAME = "bible.db"
  }
}

/** SQL di creazione tabelle condiviso dalle migrazioni (identico al sorgente). */
internal object BibleMigrationSql {
  const val CREATE_ORIGINAL_VERSES =
    "CREATE TABLE IF NOT EXISTS `original_verses` (" +
      "`book` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse` INTEGER NOT NULL, " +
      "`lang` TEXT NOT NULL, `text` TEXT NOT NULL, `transliteration` TEXT NOT NULL, " +
      "`lemmas` TEXT NOT NULL, `it_nr` TEXT NOT NULL, `it_r2` TEXT NOT NULL, `it_r27` TEXT NOT NULL, " +
      "PRIMARY KEY(`book`, `chapter`, `verse`))"
}

/**
 * Tutte le migrazioni 1→9 in ordine; l'SQL è identico al sorgente, ma il tipo del
 * callback `migrate` è platform-specific (SupportSQLiteDatabase su Android,
 * SQLiteConnection su native) → definizioni in androidMain/iosMain.
 */
expect val ALL_MIGRATIONS: Array<Migration>
