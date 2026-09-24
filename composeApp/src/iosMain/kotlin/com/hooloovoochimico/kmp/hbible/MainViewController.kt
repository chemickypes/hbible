package com.hooloovoochimico.kmp.hbible

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Entry point iOS: Xcode (iosApp/iosApp/ContentView.swift) chiama
// MainViewControllerKt.MainViewController() dal framework ComposeApp.
// Il bootstrap Koin avviene dentro App() (KoinApplication + koinConfiguration,
// opzione A di Fase 3 — PLAN §12): nessuna inizializzazione platform-side qui.
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
