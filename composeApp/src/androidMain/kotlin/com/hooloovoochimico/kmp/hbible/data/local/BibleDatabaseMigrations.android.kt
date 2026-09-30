package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migrazioni 1→9: SQL identico al sorgente (Android, SupportSQLiteDatabase).


private val MIGRATION_1_2 =
  object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `cross_references` (" +
          "`fromBook` INTEGER NOT NULL, `fromChapter` INTEGER NOT NULL, `fromVerse` INTEGER NOT NULL, " +
          "`toBook` INTEGER NOT NULL, `toChapter` INTEGER NOT NULL, `toVerse` INTEGER NOT NULL, " +
          "PRIMARY KEY(`fromBook`, `fromChapter`, `fromVerse`, `toBook`, `toChapter`, `toVerse`))",
      )
      db.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_cross_references_from` " +
          "ON `cross_references` (`fromBook`, `fromChapter`, `fromVerse`)",
      )
    }
  }

/** Transliteration scheme revision: regenerate the original-text table. */
private val MIGRATION_2_3 =
  object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DROP TABLE IF EXISTS `original_verses`")
      db.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

/** Lemmas per word added to original_verses: regenerate the table. */
private val MIGRATION_3_4 =
  object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DROP TABLE IF EXISTS `original_verses`")
      db.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

private val MIGRATION_4_5 =
  object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `lexemes` (" +
          "`lang` TEXT NOT NULL, `number` TEXT NOT NULL, `romanized` TEXT NOT NULL, " +
          "`gloss` TEXT NOT NULL, `gloss_it` TEXT NOT NULL, " +
          "PRIMARY KEY(`lang`, `number`))",
      )
    }
  }

/** Original↔Italian word alignment columns: regenerate the original-text table. */
private val MIGRATION_5_6 =
  object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DROP TABLE IF EXISTS `original_verses`")
      db.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
    }
  }

/** Personal notes and cached AI book introductions. */
private val MIGRATION_6_7 =
  object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS `notes` (" +
          "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
          "`title` TEXT NOT NULL, `content` TEXT NOT NULL, `styles` TEXT NOT NULL, " +
          "`book` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `verse` INTEGER NOT NULL, " +
          "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
      db.execSQL(
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
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DROP TABLE IF EXISTS `original_verses`")
      db.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
      db.execSQL("UPDATE `books` SET `name` = 'Abdia' WHERE `n` = 31 AND `name` = 'Obadia'")
      db.execSQL("UPDATE `books` SET `name` = '1 Corinzi' WHERE `n` = 46 AND `name` = '1 Corinti'")
      db.execSQL("UPDATE `books` SET `name` = '2 Corinzi' WHERE `n` = 47 AND `name` = '2 Corinti'")
    }
  }

/**
 * Riveduta 1927 word-alignment column (it_r27): regenerate the original-text
 * table so ensureImported() re-imports it with the third alignment channel.
 */
private val MIGRATION_8_9 =
  object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DROP TABLE IF EXISTS `original_verses`")
      db.execSQL(BibleMigrationSql.CREATE_ORIGINAL_VERSES)
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
  )
