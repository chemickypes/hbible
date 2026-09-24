package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isUnspecified
import com.hooloovoochimico.kmp.hbible.theme.Spacing

/**
 * Standard card of the app: a tonal Surface with the expressive shapes and a
 * uniform inner padding. Optionally clickable.
 */
@Composable
fun HBibleCard(
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.large,
  color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = Color.Unspecified,
  contentPadding: PaddingValues = PaddingValues(Spacing.md),
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val resolvedContentColor =
    if (contentColor.isUnspecified) contentColorFor(color) else contentColor
  if (onClick != null) {
    Surface(
      onClick = onClick,
      shape = shape,
      color = color,
      contentColor = resolvedContentColor,
      modifier = modifier,
    ) {
      Column(Modifier.padding(contentPadding), content = content)
    }
  } else {
    Surface(
      shape = shape,
      color = color,
      contentColor = resolvedContentColor,
      modifier = modifier,
    ) {
      Column(Modifier.padding(contentPadding), content = content)
    }
  }
}
