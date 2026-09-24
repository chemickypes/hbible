package com.hooloovoochimico.kmp.hbible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {

  @Query("SELECT * FROM books ORDER BY n")
  fun books(): Flow<List<BookEntity>>

  @Query(
    "SELECT * FROM verses WHERE translation = :translation AND book = :book AND chapter = :chapter ORDER BY verse",
  )
  fun chapter(translation: String, book: Int, chapter: Int): Flow<List<VerseEntity>>

  @RawQuery(observedEntities = [VerseEntity::class])
  suspend fun searchRaw(query: RoomRawQuery): List<VerseEntity>

  @Query(
    "SELECT * FROM verses WHERE translation = :translation AND book = :book AND chapter = :chapter AND verse = :verse LIMIT 1",
  )
  suspend fun verse(translation: String, book: Int, chapter: Int, verse: Int): VerseEntity?

  @Query("SELECT COUNT(*) FROM verses")
  suspend fun verseCount(): Int

  @Query(
    "SELECT * FROM original_verses WHERE book = :book AND chapter = :chapter AND verse = :verse LIMIT 1",
  )
  fun originalVerse(book: Int, chapter: Int, verse: Int): Flow<OriginalVerseEntity?>

  @Query(
    "SELECT * FROM cross_references WHERE fromBook = :book AND fromChapter = :chapter AND fromVerse = :verse " +
      "ORDER BY toBook, toChapter, toVerse",
  )
  fun crossReferences(book: Int, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>>

  @Query("SELECT COUNT(*) FROM original_verses")
  suspend fun originalVerseCount(): Int

  @Query("SELECT COUNT(*) FROM cross_references")
  suspend fun crossReferenceCount(): Int

  @Query("SELECT * FROM lexemes WHERE lang = :lang AND number = :number LIMIT 1")
  suspend fun lexeme(lang: String, number: String): LexemeEntity?

  @Query("UPDATE lexemes SET gloss_it = :glossIt WHERE lang = :lang AND number = :number")
  suspend fun updateGlossIt(lang: String, number: String, glossIt: String)

  @Query("SELECT COUNT(*) FROM lexemes")
  suspend fun lexemeCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLexemes(lexemes: List<LexemeEntity>)

  @Query(
    "SELECT book, chapter, verse FROM original_verses " +
      "WHERE lang = :lang AND lemmas LIKE :pattern " +
      "AND NOT (book = :exBook AND chapter = :exChapter AND verse = :exVerse) " +
      "ORDER BY book, chapter, verse LIMIT :limit",
  )
  suspend fun lemmaOccurrences(
    lang: String,
    pattern: String,
    exBook: Int,
    exChapter: Int,
    exVerse: Int,
    limit: Int,
  ): List<VerseRefRow>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBooks(books: List<BookEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVerses(verses: List<VerseEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOriginalVerses(verses: List<OriginalVerseEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCrossReferences(refs: List<CrossReferenceEntity>)

  // --- Notes ---

  @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
  fun notes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
  suspend fun note(id: Long): NoteEntity?

  @Query(
    "SELECT * FROM notes WHERE title LIKE '%' || :query || '%' ESCAPE '\\' OR content LIKE '%' || :query || '%' ESCAPE '\\' " +
      "ORDER BY updatedAt DESC LIMIT 100",
  )
  suspend fun searchNotes(query: String): List<NoteEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity): Long

  @Query("DELETE FROM notes WHERE id = :id")
  suspend fun deleteNote(id: Long)

  // --- Book info ---

  @Query("SELECT * FROM book_info WHERE book = :book LIMIT 1")
  fun bookInfo(book: Int): Flow<BookInfoEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookInfo(info: BookInfoEntity)
}
