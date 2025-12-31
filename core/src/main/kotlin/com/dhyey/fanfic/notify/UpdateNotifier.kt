package com.dhyey.fanfic.notify

import com.dhyey.fanfic.update.UpdateResult

interface UpdateNotifier {

    fun notify(
        ficTitle: String,
        ficId: String,
        result: UpdateResult
    )
}
