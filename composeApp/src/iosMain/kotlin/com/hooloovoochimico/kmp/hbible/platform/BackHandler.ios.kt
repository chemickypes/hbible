package com.hooloovoochimico.kmp.hbible.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler as CmBackHandler

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    CmBackHandler(enabled = enabled, onBack = onBack)
}
