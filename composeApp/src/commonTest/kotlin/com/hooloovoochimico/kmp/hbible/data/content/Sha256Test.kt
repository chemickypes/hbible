package com.hooloovoochimico.kmp.hbible.data.content

import kotlin.test.Test
import kotlin.test.assertEquals

/** Known-answer tests for the pure-Kotlin SHA-256 (FIPS 180-4 test vectors). */
class Sha256Test {

  private fun hex(s: String): ByteArray =
    ByteArray(s.length) { s[it].code.toByte() }

  @Test
  fun emptyInput() {
    assertEquals(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      Sha256.hexOf(ByteArray(0)),
    )
  }

  @Test
  fun abc() {
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      Sha256.hexOf(hex("abc")),
    )
  }

  @Test
  fun twoBlockMessage() {
    assertEquals(
      "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
      Sha256.hexOf(hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")),
    )
  }

  @Test
  fun millionA() {
    // FIPS 180-4: one million repetitions of "a"
    assertEquals(
      "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
      Sha256.hexOf(ByteArray(1_000_000) { 'a'.code.toByte() }),
    )
  }
}
