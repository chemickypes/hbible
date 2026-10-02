package com.hooloovoochimico.kmp.hbible.di

import com.hooloovoochimico.kmp.hbible.ui.explore.ExploreViewModel
import com.hooloovoochimico.kmp.hbible.ui.interlineare.InterlineareViewModel
import com.hooloovoochimico.kmp.hbible.ui.notes.NotesViewModel
import com.hooloovoochimico.kmp.hbible.ui.reader.ReaderViewModel
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel condivisi dell'app (debito differito da Fase 3). Nel sorgente erano
 * VM Hilt activity-scoped: qui vengono risolti con koinViewModel() a livello di
 * guscio App() (owner radice) e passati alle destinazioni, così Reader,
 * VerseDetail, BookInfo, Note, Editor e Impostazioni condividono la STESSA
 * istanza, con lo scoping del sorgente.
 */
val viewModelModule = module {
    viewModel { ReaderViewModel(get(), get(), get(), get()) }
    viewModel { NotesViewModel(get()) }
    viewModel { ExploreViewModel(get(), get(), get(), get()) }
    viewModel { InterlineareViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
