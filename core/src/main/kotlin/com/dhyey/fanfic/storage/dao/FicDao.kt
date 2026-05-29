package com.dhyey.fanfic.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhyey.fanfic.storage.entity.FicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFic(fic: FicEntity)

    @Update
    suspend fun updateFic(fic: FicEntity)

    @Query("SELECT * FROM fics WHERE ficId = :ficId")
    suspend fun getFicById(ficId: String): FicEntity?

    @Query("SELECT * FROM fics")
    fun observeAllFics(): Flow<List<FicEntity>>

    @Query("SELECT * FROM fics")
    suspend fun getAllFics(): List<FicEntity>

    @Query("DELETE FROM fics WHERE ficId = :ficId")
    suspend fun deleteFic(ficId: String)

    @Query("UPDATE fics SET lastReadAt = :timestamp WHERE ficId = :ficId")
    suspend fun updateLastReadAt(ficId: String, timestamp: Long)
}



