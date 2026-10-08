package com.hooloovoochimico.kmp.hbible.ui.interlineare

import com.hooloovoochimico.kmp.hbible.data.InterlinearPosition
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.testing.FakeBibleRepository
import com.hooloovoochimico.kmp.hbible.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class InterlineareViewModelTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun moveTo_clampsBookChapterVerseToValidBounds() = runTest {
    val viewModel =
      InterlineareViewModel(
        FakeBibleRepository(books = (1..66).map { BookEntity(it, "Libro $it", "L$it", 50) }),
        FakeSettingsRepository(),
      )
    // Subscribes the stateIn flows so the books list is loaded before moving.
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
    }
    advanceUntilIdle()

    viewModel.moveTo(1, 60, 10)
    advanceUntilIdle()
    assertEquals(InterlinearPosition(1, 50, 1), viewModel.uiState.value.position)

    viewModel.moveTo(99, 200, 7)
    advanceUntilIdle()
    assertEquals(InterlinearPosition(66, 50, 1), viewModel.uiState.value.position)
  }
}
