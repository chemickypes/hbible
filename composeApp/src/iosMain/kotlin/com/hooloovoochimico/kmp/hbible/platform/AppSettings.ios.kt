package com.hooloovoochimico.kmp.hbible.platform

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(name: String): Settings =
  NSUserDefaultsSettings(NSUserDefaults(suiteName = name) ?: NSUserDefaults.standardUserDefaults)
