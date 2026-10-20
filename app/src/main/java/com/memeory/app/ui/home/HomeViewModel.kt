package com.memeory.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memeory.app.R
import com.memeory.app.ui.ImportMessages
import com.memeory.core.data.MemeoryCore
import com.memeory.core.data.db.GridMeme
import com.memeory.core.data.db.PendingCounts
import com.memeory.core.data.search.SearchQuery
import com.memeory.core.data.work.MemeWork
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiState(
    val memes: List<GridMeme> = emptyList(),
    /** True when the grid shows search results rather than all memes. */
    val searching: Boolean = false,
    val pending: PendingCounts = PendingCounts(thumbnails = 0, text = 0),
    val scanningFolders: Boolean = false,
    /** False until the database has answered, so the empty screen doesn't flash. */
    val loaded: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val core = MemeoryCore.get(app)
    private val query = MutableStateFlow("")

    // Search as you type: wait a moment between keystrokes (none when clearing),
    // then swap in fresh results. The database keeps them live.
    private val gridMemes: Flow<Pair<Boolean, List<GridMeme>>> = query
        .debounce { if (it.isBlank()) 0L else 120L }
        .map { SearchQuery.toFtsMatch(it) }
        .distinctUntilChanged()
        .flatMapLatest { match ->
            if (match == null) core.memes.observeGrid().map { false to it }
            else core.memes.search(match).map { true to it }
        }

    val state: StateFlow<HomeUiState> = combine(
        gridMemes,
        core.memes.observePendingCounts(),
        MemeWork.observeFolderScanning(app),
    ) { (searching, memes), pending, scanning ->
        HomeUiState(memes, searching, pending, scanning, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val messageChannel = Channel<String>(Channel.BUFFERED)

    /** One-off messages for the snackbar at the bottom of the screen. */
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    fun setQuery(text: String) {
        query.value = text
    }

    fun thumbnailFile(memeId: Long): File = core.files.thumbnailFile(memeId)

    fun importFromGallery(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            // The copy runs in the app-wide scope, so it finishes even if the
            // user leaves this screen. We only wait here to show the result.
            val result = core.appScope.async { core.importer.importCopies(uris) }.await()
            messageChannel.send(ImportMessages.describe(getApplication<Application>().resources, result))
        }
    }

    fun addFolder(treeUri: Uri) {
        viewModelScope.launch {
            val outcome = runCatching { core.appScope.async { core.folderScanner.addFolder(treeUri) }.await() }
            val res = getApplication<Application>().resources
            val message = when (outcome.getOrNull()) {
                true -> res.getString(R.string.folder_added)
                false -> res.getString(R.string.folder_already_added)
                null -> res.getString(R.string.folder_failed)
            }
            messageChannel.send(message)
        }
    }
}
