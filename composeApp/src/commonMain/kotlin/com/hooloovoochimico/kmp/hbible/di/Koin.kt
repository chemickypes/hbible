package com.hooloovoochimico.kmp.hbible.di

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/**
 * Avvio del grafo Koin a livello di processo (Application.onCreate su Android,
 * MainViewController su iOS), NON dentro la composizione: un grafo legato alla
 * UI verrebbe ricreato a ogni ricreazione dell'Activity (rotazione, resize,
 * multi-window), riaprendo il DB. Idempotente: una seconda chiamata non fa nulla.
 */
fun initKoin() {
  if (KoinPlatform.getKoinOrNull() != null) return
  startKoin { modules(coreModule, platformModule, viewModelModule) }
}
