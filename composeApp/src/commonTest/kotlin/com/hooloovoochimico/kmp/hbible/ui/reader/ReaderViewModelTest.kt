package com.hooloovoochimico.kmp.hbible.ui.reader

import com.hooloovoochimico.kmp.hbible.testing.FakeBibleRepository
import com.hooloovoochimico.kmp.hbible.testing.FakeCmsRepository
import com.hooloovoochimico.kmp.hbible.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_becomesReadyAfterImport() = runTest {
    val viewModel = ReaderViewModel(
      FakeBibleRepository(),
      FakeSettingsRepository(),
      FakeCmsRepository(),
    )
    val state = viewModel.uiState.first { it is ReaderUiState.Ready }
    assertEquals(1, (state as ReaderUiState.Ready).verses.size)
  }
}
