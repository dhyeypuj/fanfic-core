package com.dhyey.fanfic.notify

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dhyey.fanfic.update.UpdateResult
import com.dhyey.fanfic.R

class AndroidUpdateNotifier(
    private val context: Context
) : UpdateNotifier {

    override fun notify(
        ficTitle: String,
        ficId: String,
        result: UpdateResult
    ) {
        val message = when (result) {
            is UpdateResult.NewChapters ->
                "New chapters added (${result.oldChapterCount} → ${result.newChapterCount})"
            is UpdateResult.MetadataChanged ->
                "Story updated (${result.oldWords} → ${result.newWords} words)"
            else -> return
        }

        val notification = NotificationCompat.Builder(context, "fanfic_updates")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ficTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ficId.hashCode(), notification)
    }
}
