package com.hooloovoochimico.kmp.hbible.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE = "AndroidKeyStore"
private const val MASTER_KEY_ALIAS = "hbible_secrets"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_BYTES = 12
private const val GCM_TAG_BITS = 128

actual fun createSecretStore(name: String): SecretStore = AndroidSecretStore(name)

/**
 * AES-256-GCM con chiave non esportabile in [KEYSTORE] (alias `hbible_secrets`);
 * il payload `iv(12) + ciphertext` viaggia in Base64 in una SharedPreferences
 * dedicata. [get] non lancia mai: dopo un ripristino backup o un cambio telefono
 * la chiave Keystore non esiste più e i dati cifrati sono perduti (null).
 */
private class AndroidSecretStore(private val prefsName: String) : SecretStore {

  private val prefs by lazy {
    AndroidAppContext.appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
  }

  private val masterKey: SecretKey? by lazy {
    runCatching { getOrCreateMasterKey() }.getOrNull()
  }

  override fun get(key: String): String? {
    val stored = prefs.getString(key, null) ?: return null
    val master = masterKey ?: return null
    return runCatching {
      val bytes = Base64.decode(stored, Base64.NO_WRAP)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, master, GCMParameterSpec(GCM_TAG_BITS, bytes, 0, IV_BYTES))
      String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()
  }

  override fun put(key: String, value: String) {
    val master = masterKey ?: return
    runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, master)
      val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
      prefs.edit()
        .putString(key, Base64.encodeToString(payload, Base64.NO_WRAP))
        .apply()
    }
  }

  override fun remove(key: String) {
    prefs.edit().remove(key).apply()
  }

  private fun getOrCreateMasterKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    (keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
    generator.init(
      KeyGenParameterSpec.Builder(
        MASTER_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      )
        .setKeySize(256)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build(),
    )
    return generator.generateKey()
  }
}
