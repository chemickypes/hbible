package com.hooloovoochimico.kmp.hbible.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hooloovoochimico.kmp.hbible.data.NotesRepository
import com.hooloovoochimico.kmp.hbible.data.local.NoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class NotesViewModel(
  private val notesRepository: NotesRepository,
) : ViewModel() {

  /** All notes, newest first. */
  val notes: StateFlow<List<NoteEntity>> =
    notesRepository.notes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  suspend fun note(id: Long): NoteEntity? = notesRepository.note(id)

  suspend fun searchNotes(query: String): List<NoteEntity> = notesRepository.searchNotes(query)

  suspend fun saveNote(note: NoteEntity): Long = notesRepository.saveNote(note)

  fun deleteNote(id: Long) {
    viewModelScope.launch { notesRepository.deleteNote(id) }
  }

  /** Creates a note prefilled from an AI chat reply, optionally linked to a verse/book. */
  suspend fun createNoteFromChat(
    title: String,
    content: String,
    book: Int = 0,
    chapter: Int = 0,
    verse: Int = 0,
  ): Long {
    val now = Clock.System.now().toEpochMilliseconds()
    return notesRepository.saveNote(
      NoteEntity(
        title = title,
        content = content,
        styles = "[]",
        book = book,
        chapter = chapter,
        verse = verse,
        createdAt = now,
        updatedAt = now,
      ),
    )
  }
}
