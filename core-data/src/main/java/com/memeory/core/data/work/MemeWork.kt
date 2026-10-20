package com.memeory.core.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.memeory.core.data.MemeoryCore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Background jobs. Android's WorkManager runs them, so they keep going (and
 * resume) even if the user leaves the app.
 */
object MemeWork {
    private const val PROCESS = "memeory-process"
    private const val SCAN = "memeory-scan-folders"

    /** Makes thumbnails (and, from Phase 2, reads text) for new memes. */
    fun enqueueProcessing(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            PROCESS,
            // If it's already running, queue one more pass for the newest memes.
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ProcessMemesWorker>().build(),
        )
    }

    fun enqueueFolderScan(context: Context, restartIfRunning: Boolean) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SCAN,
            if (restartIfRunning) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ScanFoldersWorker>().build(),
        )
    }

    /** Called when the app opens: check folders for new memes and finish any leftover work. */
    fun onAppOpened(context: Context) {
        enqueueFolderScan(context, restartIfRunning = false)
        enqueueProcessing(context)
    }

    fun observeFolderScanning(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(SCAN)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .distinctUntilChanged()
}

class ProcessMemesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val core = MemeoryCore.get(applicationContext)
        while (!isStopped) {
            val batch = core.memes.pending(BATCH_SIZE)
            if (batch.isEmpty()) break
            for (meme in batch) {
                if (isStopped) break
                core.processor.process(meme)
            }
        }
        return Result.success()
    }

    private companion object {
        const val BATCH_SIZE = 25
    }
}

class ScanFoldersWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        MemeoryCore.get(applicationContext).folderScanner.scanAll { isStopped }
        return Result.success()
    }
}
