package com.memeory.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.memeory.core.data.MemeoryCore
import com.memeory.core.data.db.MemeDetail
import com.memeory.core.data.db.StorageKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

data class DetailUiState(
    val loaded: Boolean = false,
    val detail: MemeDetail? = null,
    /** What to show large: the full original, or the preview if the original is gone. */
    val image: Any? = null,
    val thumbnail: File? = null,
)

class MemeDetailViewModel(app: Application, savedState: SavedStateHandle) : AndroidViewModel(app) {

    private val core = MemeoryCore.get(app)
    private val memeId: Long = checkNotNull(savedState["memeId"])

    val state: StateFlow<DetailUiState> = core.memes.observeDetail(memeId)
        .map { detail ->
            if (detail == null) {
                DetailUiState(loaded = true)
            } else {
                val meme = detail.meme
                val thumbnail = core.files.thumbnailFile(meme.id)
                val image: Any = when {
                    meme.storage == StorageKind.COPIED -> core.files.memeFile(requireNotNull(meme.fileName))
                    !meme.isMissing && meme.sourceUri != null -> meme.sourceUri!!
                    else -> thumbnail
                }
                DetailUiState(loaded = true, detail = detail, image = image, thumbnail = thumbnail)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())
}
