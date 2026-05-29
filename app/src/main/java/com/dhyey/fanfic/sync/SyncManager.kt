package com.dhyey.fanfic.sync

import com.dhyey.fanfic.auth.AuthService
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.dao.ReadingProgressDao
import com.dhyey.fanfic.storage.entity.FicEntity
import com.dhyey.fanfic.storage.entity.ReadingProgressEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val authService: AuthService,
    private val ficDao: FicDao,
    private val readingProgressDao: ReadingProgressDao
) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSynced = MutableStateFlow<Long?>(null)
    val lastSynced: StateFlow<Long?> = _lastSynced.asStateFlow()

    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = authService.currentUser.value
        if (user == null) {
            return@withContext Result.failure(Exception("Sync failed: User not logged in"))
        }
        
        _isSyncing.value = true
        try {
            val userId = user.uid
            val libraryCollection = firestore.collection("users").document(userId).collection("library")
            val progressCollection = firestore.collection("users").document(userId).collection("progress")

            // 1. Sync Fics Library
            val localFics = ficDao.getAllFics()
            val remoteFicsSnapshot = libraryCollection.get().await()
            val remoteFicsMap = remoteFicsSnapshot.documents.associateBy { it.id }

            var ficBatch = firestore.batch()
            var ficBatchCount = 0

            for (localFic in localFics) {
                val remoteFicDoc = remoteFicsMap[localFic.ficId]
                val localUpdatedAt = maxOf(localFic.dateAdded, localFic.lastReadAt ?: 0L, localFic.lastChecked)
                
                if (remoteFicDoc == null) {
                    // Upload to remote
                    val data = hashMapOf(
                        "ficId" to localFic.ficId,
                        "site" to localFic.site,
                        "url" to localFic.url,
                        "title" to localFic.title,
                        "author" to localFic.author,
                        "chapters" to localFic.chapters,
                        "words" to localFic.words,
                        "published" to localFic.published,
                        "updated" to localFic.updated,
                        "lastChecked" to localFic.lastChecked,
                        "dateAdded" to localFic.dateAdded,
                        "lastReadAt" to localFic.lastReadAt,
                        "isComplete" to localFic.isComplete,
                        "updatedAt" to localUpdatedAt
                    )
                    ficBatch.set(libraryCollection.document(localFic.ficId), data)
                    ficBatchCount++
                } else {
                    val remoteUpdatedAt = remoteFicDoc.safeGetLong("updatedAt", 0L)
                    if (localUpdatedAt > remoteUpdatedAt) {
                        // Upload local to remote
                        val data = hashMapOf(
                            "ficId" to localFic.ficId,
                            "site" to localFic.site,
                            "url" to localFic.url,
                            "title" to localFic.title,
                            "author" to localFic.author,
                            "chapters" to localFic.chapters,
                            "words" to localFic.words,
                            "published" to localFic.published,
                            "updated" to localFic.updated,
                            "lastChecked" to localFic.lastChecked,
                            "dateAdded" to localFic.dateAdded,
                            "lastReadAt" to localFic.lastReadAt,
                            "isComplete" to localFic.isComplete,
                            "updatedAt" to localUpdatedAt
                        )
                        ficBatch.set(libraryCollection.document(localFic.ficId), data)
                        ficBatchCount++
                    } else if (remoteUpdatedAt > localUpdatedAt) {
                        // Apply remote to local DB
                        val remoteFic = FicEntity(
                            ficId = remoteFicDoc.getString("ficId") ?: localFic.ficId,
                            site = remoteFicDoc.getString("site") ?: localFic.site,
                            url = remoteFicDoc.getString("url") ?: localFic.url,
                            title = remoteFicDoc.getString("title") ?: localFic.title,
                            author = remoteFicDoc.getString("author") ?: localFic.author,
                            chapters = remoteFicDoc.safeGetInt("chapters", localFic.chapters),
                            words = remoteFicDoc.safeGetInt("words", localFic.words),
                            published = remoteFicDoc.getString("published"),
                            updated = remoteFicDoc.getString("updated"),
                            lastChecked = remoteFicDoc.safeGetLong("lastChecked", localFic.lastChecked),
                            dateAdded = remoteFicDoc.safeGetLong("dateAdded", localFic.dateAdded),
                            lastReadAt = if (remoteFicDoc.contains("lastReadAt")) remoteFicDoc.safeGetLong("lastReadAt", 0L).let { if (it == 0L) null else it } else null,
                            isComplete = remoteFicDoc.safeGetBoolean("isComplete", localFic.isComplete)
                        )
                        ficDao.upsertFic(remoteFic)
                    }
                }

                if (ficBatchCount >= 100) {
                    ficBatch.commit().await()
                    ficBatch = firestore.batch()
                    ficBatchCount = 0
                }
            }

            if (ficBatchCount > 0) {
                ficBatch.commit().await()
            }

            // Sync fics that are in remote but not local
            for ((remoteFicId, remoteFicDoc) in remoteFicsMap) {
                if (localFics.none { it.ficId == remoteFicId }) {
                    val remoteFic = FicEntity(
                        ficId = remoteFicId,
                        site = remoteFicDoc.getString("site") ?: "AO3",
                        url = remoteFicDoc.getString("url") ?: "",
                        title = remoteFicDoc.getString("title") ?: "Unknown Title",
                        author = remoteFicDoc.getString("author") ?: "Unknown Author",
                        chapters = remoteFicDoc.safeGetInt("chapters", 1),
                        words = remoteFicDoc.safeGetInt("words", 0),
                        published = remoteFicDoc.getString("published"),
                        updated = remoteFicDoc.getString("updated"),
                        lastChecked = remoteFicDoc.safeGetLong("lastChecked", System.currentTimeMillis()),
                        dateAdded = remoteFicDoc.safeGetLong("dateAdded", System.currentTimeMillis()),
                        lastReadAt = if (remoteFicDoc.contains("lastReadAt")) remoteFicDoc.safeGetLong("lastReadAt", 0L).let { if (it == 0L) null else it } else null,
                        isComplete = remoteFicDoc.safeGetBoolean("isComplete", false)
                    )
                    ficDao.upsertFic(remoteFic)
                }
            }

            // 2. Sync Reading Progress
            val remoteProgressSnapshot = progressCollection.get().await()
            val remoteProgressMap = remoteProgressSnapshot.documents.associateBy { it.id }

            var progressBatch = firestore.batch()
            var progressBatchCount = 0

            for (localFic in localFics) {
                val localProgresses = readingProgressDao.getForFic(localFic.ficId)
                for (localProgress in localProgresses) {
                    val remoteProgressDoc = remoteProgressMap[localProgress.chapterId]
                    
                    if (remoteProgressDoc == null) {
                        // Upload progress
                        val data = hashMapOf(
                            "chapterId" to localProgress.chapterId,
                            "ficId" to localProgress.ficId,
                            "chapterNumber" to localProgress.chapterNumber,
                            "lastReadPosition" to localProgress.lastReadPosition,
                            "lastReadAt" to localProgress.lastReadAt,
                            "totalTimeSpentMillis" to localProgress.totalTimeSpentMillis
                        )
                        progressBatch.set(progressCollection.document(localProgress.chapterId), data)
                        progressBatchCount++
                    } else {
                        val remoteLastReadAt = remoteProgressDoc.safeGetLong("lastReadAt", 0L)
                        if (localProgress.lastReadAt > remoteLastReadAt) {
                            // Upload local to remote
                            val data = hashMapOf(
                                "chapterId" to localProgress.chapterId,
                                "ficId" to localProgress.ficId,
                                "chapterNumber" to localProgress.chapterNumber,
                                "lastReadPosition" to localProgress.lastReadPosition,
                                "lastReadAt" to localProgress.lastReadAt,
                                "totalTimeSpentMillis" to localProgress.totalTimeSpentMillis
                            )
                            progressBatch.set(progressCollection.document(localProgress.chapterId), data)
                            progressBatchCount++
                        } else if (remoteLastReadAt > localProgress.lastReadAt) {
                            // Apply remote to local DB
                            val remoteProgress = ReadingProgressEntity(
                                chapterId = remoteProgressDoc.getString("chapterId") ?: localProgress.chapterId,
                                ficId = remoteProgressDoc.getString("ficId") ?: localProgress.ficId,
                                chapterNumber = remoteProgressDoc.safeGetInt("chapterNumber", localProgress.chapterNumber),
                                lastReadPosition = remoteProgressDoc.safeGetInt("lastReadPosition", localProgress.lastReadPosition),
                                lastReadAt = remoteLastReadAt,
                                totalTimeSpentMillis = remoteProgressDoc.safeGetLong("totalTimeSpentMillis", localProgress.totalTimeSpentMillis)
                            )
                            readingProgressDao.upsert(remoteProgress)
                        }
                    }

                    if (progressBatchCount >= 100) {
                        progressBatch.commit().await()
                        progressBatch = firestore.batch()
                        progressBatchCount = 0
                    }
                }
            }

            if (progressBatchCount > 0) {
                progressBatch.commit().await()
            }

            // Sync progress that is in remote but not local
            for ((remoteChapterId, remoteProgressDoc) in remoteProgressMap) {
                val localProgress = readingProgressDao.getByChapter(remoteChapterId)
                if (localProgress == null) {
                    val remoteProgress = ReadingProgressEntity(
                        chapterId = remoteChapterId,
                        ficId = remoteProgressDoc.getString("ficId") ?: "",
                        chapterNumber = remoteProgressDoc.safeGetInt("chapterNumber", 1),
                        lastReadPosition = remoteProgressDoc.safeGetInt("lastReadPosition", 0),
                        lastReadAt = remoteProgressDoc.safeGetLong("lastReadAt", System.currentTimeMillis()),
                        totalTimeSpentMillis = remoteProgressDoc.safeGetLong("totalTimeSpentMillis", 0L)
                    )
                    readingProgressDao.upsert(remoteProgress)
                }
            }

            _lastSynced.value = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.safeGetLong(field: String, default: Long): Long {
        return try {
            val value = get(field)
            when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: default
                else -> default
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.safeGetInt(field: String, default: Int): Int {
        return try {
            val value = get(field)
            val result = when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: default.toLong()
                else -> default.toLong()
            }
            if (result > Int.MAX_VALUE) Int.MAX_VALUE
            else if (result < 0) 0
            else result.toInt()
        } catch (e: Exception) {
            default
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.safeGetBoolean(field: String, default: Boolean): Boolean {
        return try {
            val value = get(field)
            when (value) {
                is Boolean -> value
                is String -> value.toBoolean()
                is Number -> value.toInt() != 0
                else -> default
            }
        } catch (e: Exception) {
            default
        }
    }
}
