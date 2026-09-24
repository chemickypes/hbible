package com.hooloovoochimico.kmp.hbible.platform

import com.russhwolf.settings.Settings

/**
 * Seam multiplatform per le impostazioni (PLAN §9). [name] identifica il dominio
 * di storage: su Android è il nome del file SharedPreferences (compatibilità dati
 * con l'app sorgente: "settings", "search_history", "ai_settings"), su iOS è una
 * suite NSUserDefaults.
 */
expect fun createSettings(name: String): Settings
