package com.memeory.app.ui.share

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memeory.app.ui.ImportMessages
import com.memeory.core.data.MemeoryCore
import com.memeory.core.data.importing.ImportResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ShareState {
    data object Saving : ShareState
    data class Done(val message: String, val success: Boolean) : ShareState
}

/**
 * Saves the shared images once. Lives in a ViewModel so that rotating the
 * phone mid-save doesn't start a second save.
 */
class ShareViewModel(app: Application, uris: List<Uri>) : AndroidViewModel(app) {

    private val mutableState = MutableStateFlow<ShareState>(ShareState.Saving)
    val state: StateFlow<ShareState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val core = MemeoryCore.get(app)
            val result = if (uris.isEmpty()) {
                ImportResult(added = 0, duplicates = 0, failed = 0)
            } else {
                core.appScope.async { core.importer.importCopies(uris) }.await()
            }
            mutableState.value = ShareState.Done(
                message = ImportMessages.describe(app.resources, result),
                success = result.added > 0 || result.duplicates > 0,
            )
        }
    }
}
