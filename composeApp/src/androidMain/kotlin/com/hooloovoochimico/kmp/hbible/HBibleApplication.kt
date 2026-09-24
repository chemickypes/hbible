package com.hooloovoochimico.kmp.hbible

import android.app.Application
import com.hooloovoochimico.kmp.hbible.platform.AndroidAppContext

class HBibleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Registrazione del context per gli actual platform (db builder, settings).
        AndroidAppContext.appContext = this
    }
}
