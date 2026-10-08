package com.hooloovoochimico.kmp.hbible.platform

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

private const val KEYCHAIN_SERVICE_PREFIX = "com.hooloovoochimico.kmp.hbible."

@OptIn(ExperimentalSettingsImplementation::class)
actual fun createSecretStore(name: String): SecretStore =
  KeychainSecretStore(
    KeychainSettings(service = "$KEYCHAIN_SERVICE_PREFIX$name"),
  )

/** Keychain (generic password, service dedicato) via KeychainSettings. */
private class KeychainSecretStore(private val settings: Settings) : SecretStore {
  override fun get(key: String): String? = settings.getStringOrNull(key)
  override fun put(key: String, value: String) = settings.putString(key, value)
  override fun remove(key: String) = settings.remove(key)
}
