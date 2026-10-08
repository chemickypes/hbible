package com.hooloovoochimico.kmp.hbible.ui.settings

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.ai.AiCompany
import com.hooloovoochimico.kmp.hbible.data.ai.AiConfig
import com.hooloovoochimico.kmp.hbible.data.ai.AiProviderConfig
import com.hooloovoochimico.kmp.hbible.data.ai.CMS_COMPANY
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard

/**
 * Full-screen page for configuring AI providers: API keys, models, enable
 * switches and the priority order used when more than one is set up.
 */
@Composable
fun AiSettingsScreen(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val config = state.aiConfig

  Column(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .imePadding()
      .navigationBarsPadding(),
  ) {
    Row(
      Modifier.fillMaxWidth().padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onDismiss) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Torna alle impostazioni")
      }
      Text("Servizi AI", style = MaterialTheme.typography.titleLarge)
    }

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 24.dp),
    ) {
      val chain = config.enabledChain()
      Text(
        "Ordine di utilizzo",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      if (chain.isEmpty()) {
        Text(
          "Nessun servizio attivo: abilita un servizio e inserisci la sua chiave API.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
      } else {
        Text(
          chain.mapIndexed { index, provider ->
            "${index + 1}. ${providerLabel(provider)}"
          }.joinToString("  ·  "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
      }
      Text(
        "Se il primo servizio fallisce si prova il successivo. Le chiavi personali restano solo su questo dispositivo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
      )

      val ordered = config.ordered()
      ordered.forEach { provider ->
        if (provider.company == CMS_COMPANY) {
          val priority = ordered.indexOfFirst { it.company == provider.company } + 1
          AiCmsCard(
            config = config,
            provider = provider,
            priority = priority,
            onEnabledChange = { enabled ->
              viewModel.updateAiProviderNamed(CMS_COMPANY) { it.copy(enabled = enabled) }
            },
            onMoveUp = { viewModel.moveAiProviderNamed(CMS_COMPANY, -1) },
            onMoveDown = { viewModel.moveAiProviderNamed(CMS_COMPANY, +1) },
          )
        } else {
          val company = AiCompany.valueOf(provider.company)
          val priority = ordered.indexOfFirst { it.company == provider.company } + 1
          AiProviderCard(
            company = company,
            provider = provider,
            priority = priority,
            totalProviders = ordered.size,
            onEnabledChange = { enabled ->
              viewModel.updateAiProvider(company) { it.copy(enabled = enabled) }
            },
            onApiKeyChange = { key ->
              viewModel.updateAiProvider(company) { it.copy(apiKey = key) }
            },
            onModelChange = { model ->
              viewModel.updateAiProvider(company) { it.copy(model = model) }
            },
            onMoveUp = { viewModel.moveAiProvider(company, -1) },
            onMoveDown = { viewModel.moveAiProvider(company, +1) },
          )
        }
      }
    }
  }
}

@Composable
private fun AiProviderCard(
  company: AiCompany,
  provider: AiProviderConfig,
  priority: Int,
  totalProviders: Int,
  onEnabledChange: (Boolean) -> Unit,
  onApiKeyChange: (String) -> Unit,
  onModelChange: (String) -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
) {
  HBibleCard(
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(company.label, style = MaterialTheme.typography.titleMedium)
          Text(
            if (provider.enabled && provider.apiKey.isNotBlank()) "Priorità $priority"
            else "Disattivato",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onMoveUp, enabled = priority > 1) {
          Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Alza priorità ${company.label}")
        }
        IconButton(onClick = onMoveDown, enabled = priority < totalProviders) {
          Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Abbassa priorità ${company.label}")
        }
        Switch(checked = provider.enabled, onCheckedChange = onEnabledChange)
      }
      val hasKey = provider.apiKey.isNotBlank()
      Text(
        if (hasKey) "Chiave impostata" else "Nessuna chiave impostata",
        style = MaterialTheme.typography.labelSmall,
        color =
          if (hasKey) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.error,
      )
      OutlinedTextField(
        value = provider.apiKey,
        onValueChange = onApiKeyChange,
        label = { Text("Chiave API") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      )
      Text(
        "Modello",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
      )
      OutlinedTextField(
        value = provider.model,
        onValueChange = onModelChange,
        label = { Text("Nome del modello (vuoto = ${company.defaultModel})") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      @OptIn(ExperimentalLayoutApi::class)
      FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        company.modelPresets.forEach { preset ->
          FilterChip(
            selected = provider.model == preset,
            onClick = { onModelChange(preset) },
            label = { Text(preset) },
          )
        }
      }
    }
  }

/** Display name of a chain entry, resolving the CMS pseudo provider. */
private fun providerLabel(provider: AiProviderConfig): String =
  if (provider.company == CMS_COMPANY) "Default (CMS)"
  else AiCompany.valueOf(provider.company).label

/**
 * Card for the CMS-provided default service: no key or model fields — they
 * are managed on the CMS portal and refreshed automatically. The user only
 * chooses whether to use it and where in the priority order.
 */
@Composable
private fun AiCmsCard(
  config: AiConfig,
  provider: AiProviderConfig,
  priority: Int,
  onEnabledChange: (Boolean) -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
) {
  val cms = config.cms
  HBibleCard(
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Default (CMS)", style = MaterialTheme.typography.titleMedium)
        Text(
          when {
            cms == null -> "Non disponibile"
            provider.enabled -> "Priorità $priority"
            else -> "Disattivato"
          },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onMoveUp, enabled = priority > 1) {
        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Alza priorità Default (CMS)")
      }
      IconButton(onClick = onMoveDown) {
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Abbassa priorità Default (CMS)")
      }
      Switch(checked = provider.enabled, onCheckedChange = onEnabledChange)
    }
    if (cms != null) {
      val company: AiCompany? = runCatching { AiCompany.valueOf(cms.provider) }.getOrNull()
      Text(
        buildString {
          append(company?.label ?: cms.provider)
          append(" · ")
          append(cms.model.ifBlank { company?.defaultModel ?: "modello predefinito" })
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        "Chiave e modello sono gestiti dal CMS. Se imposti una chiave personale, il suo servizio ha la precedenza in base all'ordine scelto.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
      )
    } else {
      Text(
        "Il CMS non pubblica alcun servizio AI predefinito.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
