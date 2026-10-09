package com.hooloovoochimico.kmp.hbible.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.kmp.hbible.ui.settings.SettingsIcons

/** What a [SettingsItem] shows at its end. */
enum class SettingsTrailing { NONE, CHEVRON, EXTERNAL }

private val OuterCorner = 22.dp
private val InnerCorner = 4.dp

/**
 * Titled group of settings drawn as one rounded card split into segments (variant A of the
 * R05 mockup): each item gets its own surface, only the outer corners are fully rounded.
 */
@Composable
fun SettingsGroup(
  title: String,
  modifier: Modifier = Modifier,
  items: List<@Composable () -> Unit>,
) {
  Column(modifier.padding(horizontal = 12.dp).padding(bottom = 20.dp)) {
    Text(
      title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      items.forEachIndexed { index, item ->
        val top = if (index == 0) OuterCorner else InnerCorner
        val bottom = if (index == items.lastIndex) OuterCorner else InnerCorner
        Surface(
          shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
          color = MaterialTheme.colorScheme.surfaceContainer,
        ) { item() }
      }
    }
  }
}

/** Leading icon in a tonal circle. Decorative: the row text describes the item. */
@Composable
fun SettingsIcon(icon: ImageVector, modifier: Modifier = Modifier) {
  Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(40.dp)) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

/** Common row layout: icon, title and subtitle, optional end content. At least 56 dp tall. */
@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String?,
  modifier: Modifier = Modifier,
  subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  end: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SettingsIcon(icon)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (!subtitle.isNullOrBlank()) {
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
      }
    }
    end()
  }
}

/**
 * Clickable (or static, when [onClick] is null) setting: [value] is the current choice shown in
 * the accent color, [trailing] the chevron for sub-pages or the icon for external links.
 */
@Composable
fun SettingsItem(
  icon: ImageVector,
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  value: String? = null,
  trailing: SettingsTrailing = SettingsTrailing.NONE,
  selected: Boolean = false,
  onClick: (() -> Unit)? = null,
  end: @Composable RowScope.() -> Unit = {},
) {
  val rowModifier =
    if (onClick != null) modifier.clickable(role = Role.Button, onClick = onClick) else modifier
  Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent) {
    SettingsRow(icon, title, subtitle, rowModifier, subtitleColor) {
      if (value != null) {
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
      }
      end()
      when (trailing) {
        SettingsTrailing.NONE -> Unit
        SettingsTrailing.CHEVRON ->
          Icon(SettingsIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsTrailing.EXTERNAL ->
          Icon(
            SettingsIcons.OpenInNew,
            contentDescription = "Apre il browser",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
      }
    }
  }
}

/** Setting with a switch: the whole row toggles (one TalkBack target with the switch role). */
@Composable
fun SwitchItem(
  icon: ImageVector,
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
) {
  SettingsRow(
    icon,
    title,
    subtitle,
    modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
  ) {
    Switch(checked = checked, onCheckedChange = null)
  }
}

/** Setting with a single choice among a few [options], shown as segmented buttons under the title. */
@Composable
fun <T> SegmentedItem(
  icon: ImageVector,
  title: String,
  options: List<T>,
  selected: T,
  label: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  below: @Composable ColumnScope.() -> Unit = {},
) {
  Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
    SettingsRow(icon, title, subtitle = null)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      options.forEachIndexed { index, option ->
        SegmentedButton(
          selected = option == selected,
          onClick = { onSelect(option) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
        ) { Text(label(option), maxLines = 1) }
      }
    }
    below()
  }
}
