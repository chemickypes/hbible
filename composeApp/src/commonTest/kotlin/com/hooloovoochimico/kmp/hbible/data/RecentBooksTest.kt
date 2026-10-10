package com.hooloovoochimico.kmp.hbible.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RecentBooksTest {

  @Test
  fun newBookGoesFirst() {
    val recents = listOf(RecentBook(19, 23), RecentBook(1, 1))
    assertEquals(
      listOf(RecentBook(43, 3), RecentBook(19, 23), RecentBook(1, 1)),
      updateRecentBooks(recents, 43, 3),
    )
  }

  @Test
  fun sameBookMovesToFrontWithLatestChapter() {
    val recents = listOf(RecentBook(19, 23), RecentBook(43, 3), RecentBook(1, 1))
    assertEquals(
      listOf(RecentBook(43, 4), RecentBook(19, 23), RecentBook(1, 1)),
      updateRecentBooks(recents, 43, 4),
    )
  }

  @Test
  fun listIsCapped() {
    val recents = (1..RECENT_BOOKS_MAX).map { RecentBook(it, 1) }
    val updated = updateRecentBooks(recents, 40, 5)
    assertEquals(RECENT_BOOKS_MAX, updated.size)
    assertEquals(RecentBook(40, 5), updated.first())
    assertEquals(RecentBook(RECENT_BOOKS_MAX - 1, 1), updated.last())
  }
}
