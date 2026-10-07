package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.local.BibleDao
import com.hooloovoochimico.kmp.hbible.data.local.PostEntity
import com.hooloovoochimico.kmp.hbible.data.local.VotdEntity
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * CMS-authored content already applied to the local DB (verse of the day and
 * the curated feed). Everything reads offline; the tables update whenever the
 * content syncer applies new packages.
 */
interface CmsRepository {
  /**
   * Today's verse: a date override wins, otherwise the rotation pool picks a
   * deterministic entry by day of year (same verse all day, works offline,
   * empty pool → null).
   */
  fun votdForToday(): Flow<VotdEntity?>

  /** Published feed items, newest first (max 20). */
  fun publishedPosts(): Flow<List<PostEntity>>
}

class DefaultCmsRepository(
  private val dao: BibleDao,
) : CmsRepository {

  override fun votdForToday(): Flow<VotdEntity?> =
    combine(dao.votdOverrides(), dao.votdPool()) { overrides, pool ->
      if (pool.isEmpty() && overrides.isEmpty()) {
        null
      } else {
        val today = todayIso()
        overrides.firstOrNull { it.date == today }
          ?: pool.takeIf { it.isNotEmpty() }?.let { it[dayOfYear() % it.size] }
      }
    }

  override fun publishedPosts(): Flow<List<PostEntity>> = dao.publishedPosts()

  private fun todayIso(): String {
    val d = GMTDate()
    // String.format è solo JVM: padding manuale per compilare anche su native.
    fun pad(n: Int, len: Int) = n.toString().padStart(len, '0')
    return "${pad(d.year, 4)}-${pad(d.month.ordinal + 1, 2)}-${pad(d.dayOfMonth, 2)}"
  }

  private fun dayOfYear(): Int = GMTDate().dayOfYear
}
