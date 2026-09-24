package com.hooloovoochimico.kmp.hbible.platform

import android.content.Context

/**
 * Holder del context Android, registrato da [com.hooloovoochimico.kmp.hbible.HBibleApplication].
 * I soli actual androidMain lo leggono: il Context non entra mai in commonMain.
 */
object AndroidAppContext {
  lateinit var appContext: Context
}
