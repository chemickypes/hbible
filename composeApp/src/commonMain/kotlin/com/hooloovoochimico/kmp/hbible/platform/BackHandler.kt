package com.hooloovoochimico.kmp.hbible.platform

import androidx.compose.runtime.Composable

/**
 * Back handler multipiattaforma. CM 1.12.1 espone androidx.compose.ui.backhandler
 * solo sui target non-Android (il modulo ui-backhandler non entra nel classpath
 * Android di compose.ui): expect/actual — Android usa androidx.activity.compose,
 * iOS usa l'API CM (scelta registrata nel Log decisioni, PLAN §12).
 */
@Composable
expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)
