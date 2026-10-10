package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** Large serif title of a full-screen page ("Esplora", "Impostazioni", ...). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
  Text(text, style = MaterialTheme.typography.headlineMedium, modifier = modifier)
}

/** Small section label above a group of content. Padding is caller-driven. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier,
  )
}

/** Muted message for empty lists and placeholders. */
@Composable
fun EmptyMessage(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
  Text(text, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}
