package com.hooloovoochimico.kmp.hbible.platform

/**
 * Archivio cifrato per piccoli segreti (chiavi API). Android: AES-256-GCM in
 * AndroidKeyStore con ciphertext in SharedPreferences; iOS: Keychain
 * (KeychainSettings di multiplatform-settings).
 */
interface SecretStore {
  fun get(key: String): String?
  fun put(key: String, value: String)
  fun remove(key: String)
}

expect fun createSecretStore(name: String): SecretStore
