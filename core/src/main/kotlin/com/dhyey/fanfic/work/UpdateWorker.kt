package com.dhyey.fanfic.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dhyey.fanfic.notify.UpdateNotifier
import com.dhyey.fanfic.repository.FanficRepository
import com.dhyey.fanfic.update.UpdateResult

class UpdateWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: FanficRepository,
    private val notifier: UpdateNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val ficId = inputData.getString("ficId")
            ?: return Result.failure()

        val fic = repository.getFic(ficId)
            ?: return Result.success()

        // Networking/parsing will be injected later
        // Here we assume `freshMetadata` is obtained
        val freshMetadata = TODO("Fetch and parse metadata")

        val result = repository.checkForUpdates(
            ficId = ficId,
            freshMetadata = freshMetadata
        )

        if (result !is UpdateResult.NoChange &&
            result !is UpdateResult.Skipped
        ) {
            notifier.notify(
                ficTitle = fic.title,
                ficId = ficId,
                result = result
            )
        }

        return Result.success()
    }
}
