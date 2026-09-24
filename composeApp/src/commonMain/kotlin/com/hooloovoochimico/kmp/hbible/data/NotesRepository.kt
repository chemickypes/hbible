package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow

/** Personal notes stored on the device. */
interface NotesRepository {
  fun notes(): Flow<List<NoteEntity>>

  suspend fun note(id: Long): NoteEntity?

  suspend fun searchNotes(query: String): List<NoteEntity>

  suspend fun saveNote(note: NoteEntity): Long

  suspend fun deleteNote(id: Long)
}

class DefaultNotesRepository(
  private val db: BibleDatabase,
) : NotesRepository {

  override fun notes(): Flow<List<NoteEntity>> = db.bibleDao().notes()

  override suspend fun note(id: Long): NoteEntity? = db.bibleDao().note(id)

  override suspend fun searchNotes(query: String): List<NoteEntity> {
    val escaped = query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    if (escaped.isEmpty()) return emptyList()
    return db.bibleDao().searchNotes(escaped)
  }

  override suspend fun saveNote(note: NoteEntity): Long = db.bibleDao().insertNote(note)

  override suspend fun deleteNote(id: Long) {
    db.bibleDao().deleteNote(id)
  }
}
