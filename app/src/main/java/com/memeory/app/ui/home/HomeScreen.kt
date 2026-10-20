package com.memeory.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.memeory.app.R
import com.memeory.core.data.db.GridMeme
import com.memeory.core.data.db.PendingCounts
import com.memeory.core.data.db.Stage
import java.io.File

@Composable
fun HomeScreen(
    onOpenMeme: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    fun updateQuery(text: String) {
        query = text
        viewModel.setQuery(text)
    }

    // Keep the search after rotating the phone or coming back from a meme.
    LaunchedEffect(Unit) { viewModel.setQuery(query) }

    // Back button clears the search first, before leaving the app.
    BackHandler(enabled = query.isNotEmpty()) { updateQuery("") }

    // Android's own photo picker: no storage permission needed.
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> viewModel.importFromGallery(uris) }

    // Android's folder picker: the user grants access to one folder only.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.addFolder(uri) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = { Header(query = query, onQueryChange = ::updateQuery) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_memes)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ProgressBanner(pending = state.pending, scanning = state.scanningFolders)
            when {
                !state.loaded -> Unit
                state.memes.isEmpty() && state.searching -> NoResults(query, stillReading = state.pending.text > 0)
                state.memes.isEmpty() -> EmptyState()
                else -> MemeGrid(state.memes, viewModel::thumbnailFile, onOpenMeme)
            }
        }
    }

    if (showAddSheet) {
        AddMemesSheet(
            onDismiss = { showAddSheet = false },
            onGallery = {
                showAddSheet = false
                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onFolder = {
                showAddSheet = false
                folderPicker.launch(null)
            },
        )
    }
}

@Composable
private fun Header(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
    }
}

@Composable
private fun ColumnScope.ProgressBanner(pending: PendingCounts, scanning: Boolean) {
    val message = when {
        pending.thumbnails > 0 ->
            pluralStringResource(R.plurals.preparing_memes, pending.thumbnails, pending.thumbnails)
        pending.text > 0 ->
            pluralStringResource(R.plurals.reading_text, pending.text, pending.text)
        scanning -> stringResource(R.string.checking_folders)
        else -> null
    }
    AnimatedVisibility(visible = message != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EmptyState() {
    CenteredMessage(
        emoji = "🧠",
        title = stringResource(R.string.empty_title),
        body = stringResource(R.string.empty_body),
    )
}

@Composable
private fun NoResults(query: String, stillReading: Boolean) {
    CenteredMessage(
        emoji = "🤷",
        title = stringResource(R.string.no_results_title, query.trim()),
        body = stringResource(if (stillReading) R.string.no_results_still_reading else R.string.no_results_body),
    )
}

@Composable
private fun CenteredMessage(emoji: String, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MemeGrid(memes: List<GridMeme>, thumbnailFile: (Long) -> File, onOpenMeme: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(memes, key = { it.id }, contentType = { "meme" }) { meme ->
            MemeCell(meme, thumbnailFile, onClick = { onOpenMeme(meme.id) })
        }
    }
}

@Composable
private fun MemeCell(meme: GridMeme, thumbnailFile: (Long) -> File, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        // Until the thumbnail is ready, the cell shows a plain tile.
        if (meme.stage >= Stage.THUMBNAIL_READY) {
            AsyncImage(
                model = thumbnailFile(meme.id),
                contentDescription = stringResource(R.string.meme_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (meme.mimeType == "image/gif") {
            Text(
                text = stringResource(R.string.gif_badge),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
        if (meme.isMissing) {
            Icon(
                imageVector = Icons.Rounded.LinkOff,
                contentDescription = stringResource(R.string.missing_original),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .padding(3.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemesSheet(onDismiss: () -> Unit, onGallery: () -> Unit, onFolder: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.add_memes_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.from_gallery)) },
            supportingContent = { Text(stringResource(R.string.from_gallery_hint)) },
            leadingContent = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onGallery),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.from_folder)) },
            supportingContent = { Text(stringResource(R.string.from_folder_hint)) },
            leadingContent = { Icon(Icons.Rounded.Folder, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onFolder),
        )
        Spacer(Modifier.height(24.dp))
    }
}
