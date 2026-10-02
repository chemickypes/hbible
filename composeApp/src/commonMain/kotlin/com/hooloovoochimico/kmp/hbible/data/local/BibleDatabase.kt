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
    ContentStateEntity::class,
    TranslationMetaEntity::class,
    VotdEntity::class,
    PostEntity::class,
  ],
  version = 15,
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
      "`it_dio` TEXT NOT NULL, `it_nd` TEXT NOT NULL, `it_cei` TEXT NOT NULL, " +
      "`it_ric` TEXT NOT NULL, `it_mar` TEXT NOT NULL, " +
      "`glosses` TEXT NOT NULL, `glosses_it` TEXT NOT NULL, " +
      "PRIMARY KEY(`book`, `chapter`, `verse`))"

  /** Lexemes table (identica alla Fase 2): rigenerata dalle migrazioni 10→11 e 11→12. */
  const val CREATE_LEXEMES =
    "CREATE TABLE IF NOT EXISTS `lexemes` (" +
      "`lang` TEXT NOT NULL, `number` TEXT NOT NULL, `romanized` TEXT NOT NULL, " +
      "`gloss` TEXT NOT NULL, `gloss_it` TEXT NOT NULL, " +
      "PRIMARY KEY(`lang`, `number`))"

  /** Content sync state (CMS packages already applied) — migration 13→14. */
  const val CREATE_CONTENT_STATE =
    "CREATE TABLE IF NOT EXISTS `content_state` (" +
      "`package_id` TEXT NOT NULL PRIMARY KEY, " +
      "`hash` TEXT NOT NULL, `version` TEXT NOT NULL, `synced_at` INTEGER NOT NULL)"

  /** Metadata of installed translations (from CMS package meta) — migration 13→14. */
  const val CREATE_TRANSLATION_META =
    "CREATE TABLE IF NOT EXISTS `translation_meta` (" +
      "`abbr` TEXT NOT NULL PRIMARY KEY, " +
      "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `publisher` TEXT NOT NULL, " +
      "`year` TEXT NOT NULL, `copyright` TEXT NOT NULL)"

  /** Verse-of-the-day entries (pool + overrides) — migration 14→15. */
  const val CREATE_VOTD =
    "CREATE TABLE IF NOT EXISTS `votd_entries` (" +
      "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
      "`date` TEXT, `book` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse` INTEGER NOT NULL, " +
      "`translation` TEXT, `note` TEXT, `order_index` INTEGER NOT NULL)"

  /** Curated feed items — migration 14→15. */
  const val CREATE_POSTS =
    "CREATE TABLE IF NOT EXISTS `posts` (" +
      "`slug` TEXT NOT NULL PRIMARY KEY, " +
      "`title` TEXT NOT NULL, `body` TEXT NOT NULL, " +
      "`book` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse` INTEGER NOT NULL, " +
      "`published_at` TEXT)"
}

/**
 * Tutte le migrazioni 1→13 in ordine; l'SQL è identico al sorgente, ma il tipo del
 * callback `migrate` è platform-specific (SupportSQLiteDatabase su Android,
 * SQLiteConnection su native) → definizioni in androidMain/iosMain.
 */
expect val ALL_MIGRATIONS: Array<Migration>
