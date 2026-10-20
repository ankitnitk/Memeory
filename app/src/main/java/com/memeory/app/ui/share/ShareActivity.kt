package com.memeory.app.ui.share

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.memeory.app.R
import com.memeory.app.ui.theme.MemeoryTheme
import kotlinx.coroutines.delay

/**
 * Opens when the user shares images to Memeory from another app.
 * Shows a small "Saving… / Saved!" card, then closes by itself.
 */
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels {
        viewModelFactory { initializer { ShareViewModel(application, sharedUris(intent)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemeoryTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(state) {
                    if (state is ShareState.Done) {
                        delay(1_300)
                        finish()
                    }
                }
                ShareCard(state, onDismiss = { if (state is ShareState.Done) finish() })
            }
        }
    }

    /** Pulls the image addresses out of what the other app sent. */
    private fun sharedUris(intent: Intent): List<Uri> {
        val uris = buildList {
            when (intent.action) {
                Intent.ACTION_SEND ->
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?.let { add(it) }

                Intent.ACTION_SEND_MULTIPLE ->
                    IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?.let { addAll(it) }
            }
            // Some apps only put the images in the clip data.
            if (isEmpty()) {
                intent.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { add(it) }
                }
            }
        }
        return uris.filter { it.scheme == ContentResolver.SCHEME_CONTENT }.distinct()
    }
}

@Composable
private fun ShareCard(state: ShareState, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(Modifier.padding(32.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    ShareState.Saving -> {
                        CircularProgressIndicator(Modifier.size(40.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.saving), style = MaterialTheme.typography.titleMedium)
                    }

                    is ShareState.Done -> {
                        Text(if (state.success) "✅" else "😕", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
