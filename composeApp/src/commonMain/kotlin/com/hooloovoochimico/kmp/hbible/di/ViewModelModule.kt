package com.hooloovoochimico.kmp.hbible.di

import com.hooloovoochimico.kmp.hbible.ui.explore.ExploreViewModel
import com.hooloovoochimico.kmp.hbible.ui.interlineare.InterlineareViewModel
import com.hooloovoochimico.kmp.hbible.ui.notes.NotesViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.BookInfoViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.ReaderViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.VerseDetailViewModel
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel dell'app. Quelli condivisi (Reader, VerseDetail, Note, Esplora,
 * Interlineare, Impostazioni) sono risolti con koinViewModel() nel guscio App()
 * (owner radice = Activity su Android) e passati alle destinazioni: il
 * dettaglio versetto, per esempio, è lo stesso come pagina e come pannello.
 * BookInfoViewModel invece è legato alla sua destinazione di navigazione.
 */
val viewModelModule = module {
    viewModel { ReaderViewModel(get(), get(), get()) }
    viewModel { VerseDetailViewModel(get(), get(), get()) }
    viewModel { BookInfoViewModel(get(), get(), get()) }
    viewModel { NotesViewModel(get()) }
    viewModel { ExploreViewModel(get(), get(), get(), get()) }
    viewModel { InterlineareViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
