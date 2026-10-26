package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography

/**
 * Compact row of a translation: abbr chip + name on the first line, verse text
 * below. The translation in use is highlighted like the current book in the
 * reader's book sheet (primaryContainer + badge). Shared by the interlinear
 * "versioni" list and by the verse detail "altre traduzioni" list.
 */
@Composable
fun TranslationVersionRow(
  shape: Shape,
  abbr: String,
  name: String,
  text: String,
  current: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = shape,
    color =
      if (current) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceContainer,
    contentColor =
      if (current) MaterialTheme.colorScheme.onPrimaryContainer
      else MaterialTheme.colorScheme.onSurface,
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.dp),
  ) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = MaterialTheme.shapes.extraLarge,
          color =
            if (current) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor =
            if (current) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          Text(
            abbr,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Text(
          name,
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        if (current) {
          Text(
            "In lettura",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
      Text(
        text,
        style = ScriptureTypography.body.copy(fontSize = 16.sp, lineHeight = 24.sp),
        modifier = Modifier.padding(top = 8.dp),
      )
    }
  }
}
