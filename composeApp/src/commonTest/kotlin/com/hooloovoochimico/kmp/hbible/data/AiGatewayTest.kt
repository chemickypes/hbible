package com.hooloovoochimico.kmp.hbible.data

import com.hooloovoochimico.kmp.hbible.data.ai.AiChatClient
import com.hooloovoochimico.kmp.hbible.data.ai.AiChatMessage
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiGateway
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiSettingsStore
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiGatewayTest {

  private class FakeClient(
    override val company: AiCompany,
    private val reply: () -> String,
  ) : AiChatClient {
    val calls = mutableListOf<String>()

    override suspend fun chat(system: String, messages: List<AiChatMessage>, maxTokens: Int): String {
      calls.add(messages.last().content)
      return reply()
    }
  }

  private fun config(vararg providers: AiProviderConfig, order: List<String> = emptyList()) =
    AiConfig(
      providers = providers.toList(),
      order = order.ifEmpty { providers.map { it.company } },
    )

  /** Gateway whose clients are the given fakes, matched by company. */
  private fun gateway(cfg: AiConfig, vararg clients: AiChatClient) =
    AiGateway(
      configProvider = { cfg },
      clientFactory = { provider, _ ->
        clients.first { it.company == AiCompany.valueOf(provider.company) }
      },
    )

  @Test
  fun `enabledChain keeps priority order and skips disabled or keyless providers`() {
    val cfg =
      config(
        AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk-openai", enabled = true),
        AiProviderConfig(AiCompany.Z_AI.name, apiKey = "zai", enabled = true),
        AiProviderConfig(AiCompany.ANTHROPIC.name, apiKey = "claude", enabled = false),
        AiProviderConfig(AiCompany.GEMINI.name, enabled = true),
        order =
          listOf(
            AiCompany.ANTHROPIC.name,
            AiCompany.OPENAI.name,
            AiCompany.Z_AI.name,
            AiCompany.GEMINI.name,
          ),
      )
    assertEquals(
      listOf(AiCompany.OPENAI.name, AiCompany.Z_AI.name),
      cfg.enabledChain().map { it.company },
    )
  }

  @Test
  fun `move shifts provider in priority order`() {
    val cfg =
      config(
        AiProviderConfig(AiCompany.Z_AI.name),
        AiProviderConfig(AiCompany.OPENAI.name),
        AiProviderConfig(AiCompany.GEMINI.name),
        order = listOf(AiCompany.Z_AI.name, AiCompany.OPENAI.name, AiCompany.GEMINI.name),
      )
    val moved = cfg.move(AiCompany.GEMINI, -1)
    assertEquals(
      listOf(AiCompany.Z_AI.name, AiCompany.GEMINI.name, AiCompany.OPENAI.name, AiCompany.ANTHROPIC.name),
      moved.order,
    )
  }

  @Test
  fun `move up beyond top is a no-op`() {
    val cfg = config(AiProviderConfig(AiCompany.Z_AI.name), order = listOf(AiCompany.Z_AI.name))
    assertEquals(listOf(AiCompany.Z_AI.name), cfg.move(AiCompany.Z_AI, -1).order)
  }

  @Test
  fun `effectiveModel falls back to provider default`() {
    val cfg = config(AiProviderConfig(AiCompany.OPENAI.name, model = " "))
    assertEquals("gpt-5-mini", cfg.effectiveModel(AiCompany.OPENAI))
    assertEquals("glm-4.6", cfg.effectiveModel(AiCompany.Z_AI))
  }

  @Test
  fun `search returns first successful provider`() = runTest {
    val failing = FakeClient(AiCompany.Z_AI) { throw IOException("boom") }
    val working =
      FakeClient(AiCompany.OPENAI) {
        """{"corrispondenze_perfette": ["Gv 3:16"], "simili": []}"""
      }
    val cfg =
      config(
        AiProviderConfig(AiCompany.Z_AI.name, apiKey = "zai", enabled = true),
        AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk", enabled = true),
      )
    val matches = gateway(cfg, failing, working).search("descr", listOf("Gv"))
    assertEquals(listOf("Gv 3:16"), matches.perfect)
    assertEquals(1, failing.calls.size)
    assertEquals(1, working.calls.size)
  }

  @Test
  fun `search falls back in priority order`() = runTest {
    val first = FakeClient(AiCompany.Z_AI) { throw IOException("primo giu") }
    val second =
      FakeClient(AiCompany.OPENAI) {
        """{"corrispondenze_perfette": ["Sal 23"], "simili": []}"""
      }
    val cfg =
      config(
        AiProviderConfig(AiCompany.Z_AI.name, apiKey = "zai", enabled = true),
        AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk", enabled = true),
      )
    val matches = gateway(cfg, first, second).search("descr", listOf("Sal"))
    assertEquals(listOf("Sal 23"), matches.perfect)
    assertEquals(1, first.calls.size)
    assertEquals(1, second.calls.size)
  }

  @Test
  fun `search without any configured provider throws IllegalStateException`() = runTest {
    // Default factory ripristinata (AiClientFactory su Ktor, Fase 2): con chain
    // vuota requireChain() lancia PRIMA di invocare la factory.
    val gateway = AiGateway(configProvider = { AiConfig() })
    try {
      gateway.search("descr", listOf("Gv"))
      throw AssertionError("expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("Nessun servizio AI configurato"))
    }
  }

  @Test
  fun `move reorders the user providers`() {
    val moved = AiConfig().move(AiCompany.GEMINI, -1)
    assertEquals(
      listOf(AiCompany.Z_AI.name, AiCompany.OPENAI.name, AiCompany.GEMINI.name, AiCompany.ANTHROPIC.name),
      moved.order,
    )
  }

  @Test
  fun `firstAid returns reflection and matches from the first working provider`() = runTest {
    val failing = FakeClient(AiCompany.Z_AI) { throw IOException("boom") }
    val working =
      FakeClient(AiCompany.OPENAI) {
        """{"riflessione": "Non sei solo.", "corrispondenze_perfette": ["Sal 23"], "simili": ["Mt 11:28"]}"""
      }
    val cfg =
      config(
        AiProviderConfig(AiCompany.Z_AI.name, apiKey = "zai", enabled = true),
        AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk", enabled = true),
      )
    val matches = gateway(cfg, failing, working).firstAid("ansia", listOf("Sal", "Mt"))
    assertEquals("Non sei solo.", matches.riflessione)
    assertEquals(listOf("Sal 23"), matches.perfect)
    assertEquals(listOf("Mt 11:28"), matches.similar)
    assertEquals(1, failing.calls.size)
    assertEquals(1, working.calls.size)
  }

  @Test
  fun `firstAid falls back in priority order`() = runTest {
    val first = FakeClient(AiCompany.Z_AI) { throw IOException("primo giu") }
    val second =
      FakeClient(AiCompany.OPENAI) {
        """{"riflessione": "Coraggio.", "corrispondenze_perfette": [], "simili": ["Sal 27"]}"""
      }
    val cfg =
      config(
        AiProviderConfig(AiCompany.Z_AI.name, apiKey = "zai", enabled = true),
        AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk", enabled = true),
      )
    val matches = gateway(cfg, first, second).firstAid("paura", listOf("Sal"))
    assertEquals("Coraggio.", matches.riflessione)
    assertEquals(1, first.calls.size)
    assertEquals(1, second.calls.size)
  }

  @Test
  fun `verse search without riflessione defaults to empty`() = runTest {
    val working =
      FakeClient(AiCompany.OPENAI) {
        """{"corrispondenze_perfette": ["Gv 3:16"], "simili": []}"""
      }
    val cfg = config(AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk", enabled = true))
    val matches = gateway(cfg, working).search("descr", listOf("Gv"))
    assertEquals("", matches.riflessione)
  }

  @Test
  fun `translateGloss tries next provider when one fails`() = runTest {
    val first = FakeClient(AiCompany.Z_AI) { throw IOException("down") }
    val second = FakeClient(AiCompany.OPENAI) { "amore" }
    val cfg =
      config(
        AiProviderConfig(AiCompany.Z_AI.name, apiKey = "zai", enabled = true),
        AiProviderConfig(AiCompany.OPENAI.name, apiKey = "sk", enabled = true),
      )
    assertEquals("amore", gateway(cfg, first, second).translateGloss("love", "gr"))
    assertEquals(1, first.calls.size)
    assertEquals(1, second.calls.size)
  }
}
