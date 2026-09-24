package com.hooloovoochimico.kmp.hbible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Come nel sorgente: edge-to-edge, gli screen gestiscono i propri inset.
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
