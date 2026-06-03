package com.dhyey.fanfic.di

import android.content.Context
import androidx.room.Room
import com.dhyey.fanfic.cache.CacheCleaner
import com.dhyey.fanfic.cache.ChapterCache
import com.dhyey.fanfic.network.FicFetcher
import com.dhyey.fanfic.network.WebViewFetcher
import com.dhyey.fanfic.reader.ProgressRepository
import com.dhyey.fanfic.reader.ReaderRepository
import com.dhyey.fanfic.repository.FanficRepository
import com.dhyey.fanfic.storage.FanficDatabase
import com.dhyey.fanfic.storage.dao.ChapterDao
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.dao.ReadingProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

import com.dhyey.fanfic.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FanficDatabase {
        val keyManager = DatabaseKeyManager(context)
        val passphrase = keyManager.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            FanficDatabase::class.java,
            "fanfic_database"
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    
    private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Add new columns for sorting/filtering with default values
            db.execSQL("ALTER TABLE fics ADD COLUMN dateAdded INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            db.execSQL("ALTER TABLE fics ADD COLUMN lastReadAt INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE fics ADD COLUMN isComplete INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    fun provideFicDao(database: FanficDatabase): FicDao = database.ficDao()

    @Provides
    fun provideChapterDao(database: FanficDatabase): ChapterDao = database.chapterDao()

    @Provides
    fun provideReadingProgressDao(database: FanficDatabase): ReadingProgressDao =
        database.readingProgressDao()

    @Provides
    @Singleton
    fun provideChapterCache(@ApplicationContext context: Context): ChapterCache {
        val filesDir = File(context.filesDir, "chapters")
        return ChapterCache(filesDir)
    }

    @Provides
    @Singleton
    fun provideCacheCleaner(
        chapterCache: ChapterCache
    ): CacheCleaner {
        return CacheCleaner(chapterCache)
    }

    @Provides
    @Singleton
    fun provideFanficRepository(
        ficDao: FicDao,
        chapterDao: ChapterDao,
        chapterCache: ChapterCache,
        cacheCleaner: CacheCleaner
    ): FanficRepository {
        return FanficRepository(ficDao, chapterDao, chapterCache, cacheCleaner)
    }

    @Provides
    @Singleton
    fun provideReaderRepository(
        fanficRepository: FanficRepository
    ): ReaderRepository {
        return ReaderRepository(fanficRepository)
    }

    @Provides
    @Singleton
    fun provideProgressRepository(
        readingProgressDao: ReadingProgressDao
    ): ProgressRepository {
        return ProgressRepository(readingProgressDao)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(authService: com.dhyey.fanfic.auth.FirebaseAuthService): com.dhyey.fanfic.auth.AuthService = authService

    @Provides
    @Singleton
    fun provideFicFetcher(
        okHttpClient: OkHttpClient,
        webViewFetcher: WebViewFetcher
    ): FicFetcher {
        return FicFetcher(okHttpClient, webViewFetcher)
    }
}
