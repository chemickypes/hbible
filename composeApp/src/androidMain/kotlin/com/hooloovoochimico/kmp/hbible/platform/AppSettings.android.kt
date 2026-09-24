package com.hooloovoochimico.kmp.hbible.platform

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings

actual fun createSettings(name: String): Settings =
  SharedPreferencesSettings(
    AndroidAppContext.appContext.getSharedPreferences(name, Context.MODE_PRIVATE),
  )
