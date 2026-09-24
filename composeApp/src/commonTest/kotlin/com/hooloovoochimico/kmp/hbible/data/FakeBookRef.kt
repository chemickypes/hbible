package com.hooloovoochimico.kmp.hbible.data

/**
 * Test double di BookRef: sostituisce BookEntity (Room) nei test del core
 * puro; l'entity reale arriverà in Fase 2 implementando l'interfaccia.
 */
data class FakeBookRef(
  override val n: Int,
  override val name: String,
  override val abbr: String,
  override val chapters: Int,
) : BookRef
