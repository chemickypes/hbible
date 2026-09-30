package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Migrazioni 1→12: SQL identico al sorgente (native: callback su SQLiteConnection).


private val MIGRATION_1_2 =
  object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
      connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `cross_references` (" +
          "`fromBook` INTEGER NOT NULL, `fromChapter` INTEGER NOT NULL, `fromVerse` INTEGER NOT NULL, " +
          "`toBook` INTEGER NOT NULL, `toChapter` INTEGER NOT NULL, `toVerse` INTEGER NOT NULL, " +
          "PRIMARY KEY(`fromBook`, `fromChapter`, `fromVerse`, `toBook`, `toChapter`, `toVerse`))",
      )
      connection.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_cross_references_from` " +
          "ON `cross_references` (`fromBook`, `fromChapter`, `fromVerse`)",
      )
    }
  }

/** Transliteration scheme revision: regenerate the original-text table. */
private val MIGRATION_2_3 =
  object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

/** Lemmas per word added to original_verses: regenerate the table. */
private val MIGRATION_3_4 =
  object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

private val MIGRATION_4_5 =
  object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL(BibleMigrationSql.CREATE_LEXEMES)
    }
  }

/** Original↔Italian word alignment columns: regenerate the original-text table. */
private val MIGRATION_5_6 =
  object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

/** Personal notes and cached AI book introductions. */
private val MIGRATION_6_7 =
  object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `notes` (" +
          "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
          "`title` TEXT NOT NULL, `content` TEXT NOT NULL, `styles` TEXT NOT NULL, " +
          "`book` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse` INTEGER NOT NULL, " +
          "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
      )
      connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
      connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `book_info` (" +
          "`book` INTEGER NOT NULL PRIMARY KEY, " +
          "`context_text` TEXT NOT NULL, `protagonists` TEXT NOT NULL, " +
          "`christocentric` TEXT NOT NULL, `model` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)",
      )
    }
  }

/**
 * Psalm versification realigned to the Italian text (MT superscriptions no
 * longer occupy a numbered verse) and book names fixed: regenerate the
 * original-text table and refresh the renamed books.
 */
private val MIGRATION_7_8 =
  object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
      connection.execSQL("UPDATE `books` SET `name` = 'Abdia' WHERE `n` = 31 AND `name` = 'Obadia'")
      connection.execSQL("UPDATE `books` SET `name` = '1 Corinzi' WHERE `n` = 46 AND `name` = '1 Corinti'")
      connection.execSQL("UPDATE `books` SET `name` = '2 Corinzi' WHERE `n` = 47 AND `name` = '2 Corinti'")
    }
  }

/**
 * Riveduta 1927 word-alignment column (it_r27): regenerate the original-text
 * table so ensureImported() re-imports it with the third alignment channel.
 */
private val MIGRATION_8_9 =
  object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

/**
 * Contextual interlinear gloss columns (glosses/glosses_it): regenerate the
 * original-text table so ensureImported() re-imports it with the gloss channels.
 */
private val MIGRATION_9_10 =
  object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

/**
 * Lexicon upgrade: clean OpenScriptures English definitions + Italian glosses
 * pre-generated offline. Regenerate the lexemes table (see the Android actual
 * for the rationale); ensureImported() re-imports because the count gate sees 0.
 */
private val MIGRATION_10_11 =
  object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `lexemes`")
      connection.execSQL(BibleMigrationSql.CREATE_LEXEMES)
    }
  }

/**
 * Italian quality pass: NT-wide per-verse Italian glosses (gi channel from the
 * interlinear dump) + fully pre-generated Italian lexicon. Regenerate both the
 * original-text table and the lexemes table (see the Android actual for the
 * rationale); ensureImported() re-imports because the count gates see 0.
 */
private val MIGRATION_11_12 =
  object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
      connection.execSQL("DROP TABLE IF EXISTS `original_verses`")
      connection.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
      connection.execSQL("DROP TABLE IF EXISTS `lexemes`")
      connection.execSQL(BibleMigrationSql.CREATE_LEXEMES)
    }
  }

actual val ALL_MIGRATIONS: Array<Migration> =
  arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
  )
