package com.memeory.app.ui.detail

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.memeory.app.R
import com.memeory.core.data.db.MemeDetail
import com.memeory.core.data.db.Stage
import com.memeory.core.data.db.StorageKind
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeDetailScreen(
    onBack: () -> Unit,
    viewModel: MemeDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // If the meme disappears (e.g. deleted), go back.
    LaunchedEffect(state.loaded, state.detail) {
        if (state.loaded && state.detail == null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val detail = state.detail ?: return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            BigImage(state, detail)
            Spacer(Modifier.height(16.dp))
            if (detail.meme.isMissing) {
                MissingNotice()
                Spacer(Modifier.height(12.dp))
            }
            TextCard(detail)
            Spacer(Modifier.height(12.dp))
            InfoCard(detail)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColumnScope.BigImage(state: DetailUiState, detail: MemeDetail) {
    val meme = detail.meme
    // Keep the meme's shape, within sensible limits for very tall or wide images.
    val ratio = if (meme.width > 0 && meme.height > 0) {
        (meme.width.toFloat() / meme.height).coerceIn(0.4f, 2.5f)
    } else {
        1f
    }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.65f).dp

    // If the full original can't be opened, fall back to the preview.
    var useThumbnail by remember(state.image) { mutableStateOf(false) }

    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .heightIn(max = maxHeight)
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = if (useThumbnail) state.thumbnail else state.image,
            contentDescription = stringResource(R.string.meme_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onError = { if (!useThumbnail) useThumbnail = true },
        )
    }
}

@Composable
private fun MissingNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.missing_explained),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun TextCard(detail: MemeDetail) {
    val meme = detail.meme
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.text_in_meme), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            when {
                meme.stage < Stage.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.text_reading), style = MaterialTheme.typography.bodyMedium)
                }

                meme.ocrText.isNullOrBlank() -> Text(
                    stringResource(R.string.text_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Long-press to select and copy words.
                else -> SelectionContainer {
                    Text(meme.ocrText.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(detail: MemeDetail) {
    val meme = detail.meme
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow(
                stringResource(R.string.info_kept),
                if (meme.storage == StorageKind.COPIED) {
                    stringResource(R.string.info_kept_copy)
                } else {
                    stringResource(R.string.info_kept_folder, detail.folderName ?: "?")
                },
            )
            if (meme.width > 0) InfoRow(stringResource(R.string.info_size), "${meme.width} × ${meme.height}")
            if (meme.sizeBytes > 0) {
                InfoRow(stringResource(R.string.info_file), Formatter.formatShortFileSize(context, meme.sizeBytes))
            }
            InfoRow(
                stringResource(R.string.info_added),
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(meme.addedAt)),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
