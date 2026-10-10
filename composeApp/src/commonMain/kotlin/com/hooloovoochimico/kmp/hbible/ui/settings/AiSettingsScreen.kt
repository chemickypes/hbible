package com.hooloovoochimico.kmp.hbible.ui.settings

import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.platform.readClipboardText
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

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons

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
        Icon(AppIcons.ArrowBack, contentDescription = "Torna alle impostazioni")
      }
      Text("Assistente AI", style = MaterialTheme.typography.titleLarge)
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
        "Se il primo servizio fallisce si prova il successivo. Le chiavi restano solo su questo dispositivo, cifrate, e le richieste vanno direttamente al servizio scelto.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
      )

      val ordered = config.ordered()
      ordered.forEach { provider ->
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
          Icon(AppIcons.KeyboardArrowUp, contentDescription = "Alza priorità ${company.label}")
        }
        IconButton(onClick = onMoveDown, enabled = priority < totalProviders) {
          Icon(AppIcons.KeyboardArrowDown, contentDescription = "Abbassa priorità ${company.label}")
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
        // Non KeyboardType.Password: Android lo tratta come una password (autofill e
        // "suggerisci password" al posto di "Incolla"), mentre qui si incolla una chiave.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
        trailingIcon = {
          TextButton(onClick = { readClipboardText()?.trim()?.takeIf { it.isNotEmpty() }?.let(onApiKeyChange) }) {
            Text("Incolla")
          }
        },
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

/** Display name of a chain entry. */
private fun providerLabel(provider: AiProviderConfig): String = AiCompany.valueOf(provider.company).label

