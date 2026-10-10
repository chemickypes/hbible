package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GroupOuterCorner = 22.dp
private val GroupInnerCorner = 4.dp

/**
 * Shape of the [index]-th of [count] rows drawn as one segmented card (same look as the
 * settings groups): only the outer corners of the group are fully rounded. Useful in
 * lazy lists, where every row is a separate item.
 */
fun groupedShape(index: Int, count: Int): Shape {
  val top = if (index == 0) GroupOuterCorner else GroupInnerCorner
  val bottom = if (index == count - 1) GroupOuterCorner else GroupInnerCorner
  return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Section label in the accent color with an optional action at the end ("Cancella", ...). */
@Composable
fun GroupHeader(
  title: String,
  modifier: Modifier = Modifier,
  action: @Composable RowScope.() -> Unit = {},
) {
  Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(
      title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
    )
    action()
  }
}

/** Decorative icon in a tonal circle. */
@Composable
fun TonalIcon(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  container: Color = MaterialTheme.colorScheme.primaryContainer,
  content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
  Surface(shape = CircleShape, color = container, modifier = modifier.size(size)) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size / 2))
    }
  }
}

/** Centered empty state: icon in a tonal circle, title and an optional hint. */
@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  modifier: Modifier = Modifier,
  hint: String? = null,
) {
  Column(
    modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    TonalIcon(
      icon,
      size = 64.dp,
      container = MaterialTheme.colorScheme.surfaceContainerHigh,
      content = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      title,
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 16.dp),
    )
    if (hint != null) {
      Text(
        hint,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}
