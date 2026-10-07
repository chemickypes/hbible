package com.hooloovoochimico.kmp.hbible

import androidx.compose.ui.window.ComposeUIViewController
import com.hooloovoochimico.kmp.hbible.di.initKoin
import platform.UIKit.UIViewController

// Entry point iOS: Xcode (iosApp/iosApp/ContentView.swift) chiama
// MainViewControllerKt.MainViewController() dal framework ComposeApp.
// Koin si avvia qui (idempotente), prima della composizione: il grafo vive
// col processo, non con la UI (PLAN §12, 2026-10-07).
fun MainViewController(): UIViewController {
  initKoin()
  return ComposeUIViewController { App() }
}
