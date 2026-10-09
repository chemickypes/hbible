package com.hooloovoochimico.kmp.hbible.di

import com.hooloovoochimico.kmp.hbible.data.CmsRepository
import com.hooloovoochimico.kmp.hbible.data.DefaultCmsRepository
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
import com.hooloovoochimico.kmp.hbible.data.content.ContentSyncer
import com.hooloovoochimico.kmp.hbible.data.ai.AiClientFactory
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiSettingsStore
import com.hooloovoochimico.kmp.hbible.data.ai.aiHttpClient
import com.hooloovoochimico.kmp.hbible.data.local.ALL_MIGRATIONS
import com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase
import com.hooloovoochimico.kmp.hbible.platform.appVersionCode
import com.hooloovoochimico.kmp.hbible.platform.bibleDatabaseBuilder
import com.hooloovoochimico.kmp.hbible.platform.createSecretStore
import com.hooloovoochimico.kmp.hbible.platform.createSettings
import com.hooloovoochimico.kmp.hbible.platform.isDebugBuild
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

    // Segreti (chiavi API AI) cifrati: Android Keystore / iOS Keychain.
    single { createSecretStore("secrets") }

    // HTTP client AI condiviso (Ktor, timeout 20s connect / 120s request).
    single<HttpClient> { aiHttpClient() }

    // Repository.
    single<BibleRepository> { DefaultBibleRepository(get()) }
    single<NotesRepository> { DefaultNotesRepository(get()) }
    single<ExploreRepository> { DefaultExploreRepository(get(), get(), get(named(SearchHistory.PREFS))) }
    single<SettingsRepository> { DefaultSettingsRepository(get(), get(named(AiSettingsStore.PREFS)), get()) }

    // Contenuti curati dal CMS (VOTD + feed) già applicati al DB locale.
    single<CmsRepository> { DefaultCmsRepository(get()) }

    // Sync dei contenuti dal CMS (manifest + pacchetti /content/*).
    single {
      ContentSyncer(get(), get(), get(), get(), appVersionCode = appVersionCode(), allowUrlOverride = isDebugBuild())
    }

    /**
     * Single [AiGateway] per tutta l'app. La configurazione viene riletta a
     * ogni chiamata, quindi le modifiche nella schermata impostazioni hanno
     * effetto immediato (come nel sorgente).
     */
    single {
        AiGateway(
            configProvider = { get<SettingsRepository>().loadAiConfig() },
            clientFactory = { p, c ->
                AiClientFactory.createForEntry(p, c, client = get())
            },
        )
    }
}
