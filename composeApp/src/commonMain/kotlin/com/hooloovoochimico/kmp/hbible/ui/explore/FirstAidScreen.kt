package com.hooloovoochimico.kmp.hbible.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hooloovoochimico.kmp.hbible.data.local.BookEntity
import com.hooloovoochimico.kmp.hbible.platform.BackHandler
import com.hooloovoochimico.kmp.hbible.theme.ScriptureTypography
import com.hooloovoochimico.kmp.hbible.ui.common.AppIcons
import com.hooloovoochimico.kmp.hbible.ui.common.EmptyState
import com.hooloovoochimico.kmp.hbible.ui.common.HBibleCard
import com.hooloovoochimico.kmp.hbible.ui.common.TonalIcon

/**
 * Voce del "Pronto soccorso": riflessione scritta nel CMS e versetti da leggere
 * nella traduzione del lettore; un tocco sul versetto apre il passo.
 */
@Composable
fun FirstAidScreen(
  books: List<BookEntity>,
  onGoToVerse: (Int, Int, Int) -> Unit,
  onSaveToNote: (title: String, body: String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: FirstAidViewModel,
) {
  BackHandler(enabled = true, onBack = onDismiss)
  val state by viewModel.state.collectAsStateWithLifecycle()
  val bookNames = books.associate { it.n to it.name }
  val colors = MaterialTheme.colorScheme

  Column(
    modifier
      .fillMaxSize()
      .background(colors.surface)
      .statusBarsPadding(),
  ) {
    IconButton(onClick = onDismiss) {
      Icon(AppIcons.Close, contentDescription = "Chiudi")
    }
    val entry = state.entry
    when {
      entry != null ->
        LazyColumn(
          Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          item(key = "header") {
            Column(
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(firstAidBrush())
                .padding(20.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                TonalIcon(AppIcons.Favorite, size = 32.dp, container = colors.secondary, content = colors.onSecondary)
                Text(
                  "PRONTO SOCCORSO",
                  style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                  color = colors.onSecondaryContainer,
                  modifier = Modifier.padding(start = 10.dp),
                )
              }
              Text(
                entry.title,
                style = MaterialTheme.typography.displaySmall,
                color = colors.onSecondaryContainer,
                modifier = Modifier.padding(top = 12.dp),
              )
              if (entry.subtitle.isNotBlank()) {
                Text(
                  entry.subtitle,
                  style = MaterialTheme.typography.bodyLarge,
                  color = colors.onSecondaryContainer.copy(alpha = 0.85f),
                  modifier = Modifier.padding(top = 4.dp),
                )
              }
            }
          }
          item(key = "body") {
            HBibleCard(
              color = colors.surfaceContainerLow,
              contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
              Text("Riflessione", style = MaterialTheme.typography.labelLarge, color = colors.primary)
              Text(entry.body, style = ScriptureTypography.intro, modifier = Modifier.padding(top = 8.dp))
              TextButton(
                onClick = { onSaveToNote("Pronto soccorso · ${entry.title}", entry.body) },
                modifier = Modifier.padding(top = 4.dp),
              ) {
                Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Crea nota dalla riflessione", modifier = Modifier.padding(start = 6.dp))
              }
            }
          }
          if (state.verses.isNotEmpty()) {
            item(key = "verses-header") {
              Text(
                "Versetti da leggere · ${state.verses.size}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
              )
            }
            itemsIndexed(state.verses, key = { i, _ -> "verse-$i" }) { _, match ->
              val r = match.ref
              Column(
                Modifier
                  .fillMaxWidth()
                  .clickable { onGoToVerse(r.book, r.chapter, r.verse ?: 1) }
                  .padding(horizontal = 24.dp, vertical = 10.dp),
              ) {
                Text(aiRangeLabel(match, bookNames), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                match.text?.let {
                  Text(it, style = ScriptureTypography.body.copy(fontSize = 16.sp, lineHeight = 24.sp), modifier = Modifier.padding(top = 4.dp))
                }
              }
            }
          }
        }
      state.loading ->
        Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
        }
      else ->
        EmptyState(
          AppIcons.Favorite,
          "Voce non disponibile",
          hint = "Collegati a internet per scaricare le voci del pronto soccorso.",
        )
    }
  }
}
