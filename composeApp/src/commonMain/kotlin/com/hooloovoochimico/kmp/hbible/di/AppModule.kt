package com.hooloovoochimico.kmp.hbible.di

import com.hooloovoochimico.kmp.hbible.data.BibleRepository
import com.hooloovoochimico.kmp.hbible.data.DefaultBibleRepository
import com.hooloovoochimico.kmp.hbible.data.DefaultExploreRepository
import com.hooloovoochimico.kmp.hbible.data.DefaultNotesRepository
import com.hooloovoochimico.kmp.hbible.data.DefaultSettingsRepository
import com.hooloovoochimico.kmp.hbible.data.ExploreRepository
import com.hooloovoochimico.kmp.hbible.data.NotesRepository
import com.hooloovoochimico.kmp.hbible.data.SettingsRepository
import com.hooloovoochimico.kmp.hbible.data.SearchHistory
import com.hooloovoochimico.kmp.hbible.data.ThemePreferences
import com.hooloovoochimico.kmp.hbible.data.ai.AiClientFactory
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiSettingsStore
import com.hooloovoochimico.kmp.hbible.data.ai.aiHttpClient
import com.hooloovoochimico.kmp.hbible.data.local.ALL_MIGRATIONS
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.platform.bibleDatabaseBuilder
import com.hooloovoochimico.kmp.hbible.platform.createSettings
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Modulo core (commonMain): DB Room + migrazioni, DAO, settings, client HTTP,
 * repository e AiGateway. I nomi dei file prefs ("settings", "search_history",
 * "ai_settings") restano quelli del sorgente per compatibilità dati.
 */
val coreModule = module {
    // Database Room (builder = seam expect/actual; migrazioni 1→8 comuni).
    single { bibleDatabaseBuilder().addMigrations(*ALL_MIGRATIONS).build() }
    single { get<BibleDatabase>().bibleDao() }

    // Settings: file del sorgente, stessa identità dati.
    single { createSettings(ThemePreferences.PREFS) } // "settings"
    single(named(SearchHistory.PREFS)) { createSettings(SearchHistory.PREFS) } // "search_history"
    single(named(AiSettingsStore.PREFS)) { createSettings(AiSettingsStore.PREFS) } // "ai_settings"

    // HTTP client AI condiviso (Ktor, timeout 20s connect / 120s request).
    single<HttpClient> { aiHttpClient() }

    // Repository.
    single<BibleRepository> { DefaultBibleRepository(get()) }
    single<NotesRepository> { DefaultNotesRepository(get()) }
    single<ExploreRepository> { DefaultExploreRepository(get(), get(), get(named(SearchHistory.PREFS))) }
    single<SettingsRepository> { DefaultSettingsRepository(get(), get(named(AiSettingsStore.PREFS))) }

    /**
     * Single [AiGateway] per tutta l'app. La configurazione viene riletta a
     * ogni chiamata, quindi le modifiche nella schermata impostazioni hanno
     * effetto immediato (come nel sorgente).
     */
    single {
        AiGateway(
            configProvider = { get<SettingsRepository>().loadAiConfig() },
            clientFactory = { p, c ->
                AiClientFactory.create(p, c.effectiveModel(AiCompany.valueOf(p.company)), client = get())
            },
        )
    }
}
