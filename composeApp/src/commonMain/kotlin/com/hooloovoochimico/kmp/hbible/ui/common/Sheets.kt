package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookSheetRow(
  name: String,
  abbr: String,
  selected: Boolean,
  onClick: () -> Unit,
  showReadingBadge: Boolean = true,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.extraLarge,
    color =
      if (selected) MaterialTheme.colorScheme.secondaryContainer
      else MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor =
      if (selected) MaterialTheme.colorScheme.onSecondaryContainer
      else MaterialTheme.colorScheme.onSurface,
    modifier =
      Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
  ) {
    Row(
      Modifier.padding(start = 32.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(name, style = MaterialTheme.typography.titleMedium)
      SheetChip(label = abbr)
      Spacer(Modifier.weight(1f))
      if (selected) {
        if (showReadingBadge) {
          SheetChip(label = "In lettura")
        }
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(28.dp),
        ) {
          Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.padding(6.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SheetChip(label: String) {
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
