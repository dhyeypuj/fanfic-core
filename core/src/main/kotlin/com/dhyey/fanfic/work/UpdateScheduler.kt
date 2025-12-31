package com.dhyey.fanfic.work

import android.content.Context
import androidx.work.*

import java.util.concurrent.TimeUnit

object UpdateScheduler {

    fun scheduleDailyCheck(
        context: Context,
        ficId: String
    ) {
        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(
                workDataOf("ficId" to ficId)
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "update_$ficId",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
}
