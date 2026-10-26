package com.hooloovoochimico.kmp.hbible.ui.explore

import com.hooloovoochimico.kmp.hbible.data.content.PackageFetcher
import com.hooloovoochimico.kmp.hbible.data.local.FirstAidEntity
import com.hooloovoochimico.kmp.hbible.data.local.TopicEntity
import com.hooloovoochimico.kmp.hbible.testing.FakeBibleRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Pagina di una voce curata (pronto soccorso o tema): la stessa VM legge la
 * tabella della sezione richiesta e scarica il pacchetto giusto se la voce manca.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstAidViewModelTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(repo: FakeBibleRepository, fetched: MutableList<String>) =
    FirstAidViewModel(repo, PackageFetcher { path -> fetched += path; false })

  @Test
  fun topic_present_showsItWithoutDownloading() = runTest {
    val topic = TopicEntity(slug = "preghiera", title = "La preghiera", subtitle = "Parlare con Dio", body = "Riflessione")
    val fetched = mutableListOf<String>()
    val vm = viewModel(FakeBibleRepository(topics = listOf(topic)), fetched)
    vm.open(CuratedSection.TOPICS, "preghiera", "NTA")
    val state = vm.state.first { it.entry != null }
    assertEquals("La preghiera", state.entry?.title)
    assertEquals("Parlare con Dio", state.entry?.subtitle)
    assertEquals("Riflessione", state.entry?.body)
    assertFalse(state.loading)
    assertTrue(fetched.isEmpty())
  }

  @Test
  fun topic_missing_triesTopicsPackageThenUnavailable() = runTest {
    val fetched = mutableListOf<String>()
    val vm = viewModel(FakeBibleRepository(), fetched)
    vm.open(CuratedSection.TOPICS, "assente", "NTA")
    val state = vm.state.first { it.unavailable }
    assertEquals(listOf(TOPICS_PACKAGE), fetched)
    assertFalse(state.loading)
    assertNull(state.entry)
  }

  @Test
  fun firstAid_readsItsOwnTable_notTopics() = runTest {
    val topic = TopicEntity(slug = "stesso", title = "Tema")
    val firstAid = FirstAidEntity(slug = "stesso", title = "Pronto soccorso")
    val fetched = mutableListOf<String>()
    val vm = viewModel(FakeBibleRepository(topics = listOf(topic), firstAid = listOf(firstAid)), fetched)
    vm.open(CuratedSection.FIRST_AID, "stesso", "NTA")
    val state = vm.state.first { it.entry != null }
    assertEquals("Pronto soccorso", state.entry?.title)
    assertTrue(fetched.isEmpty())
  }
}
