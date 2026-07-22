package com.nutriai.di

import android.content.Context
import androidx.room.Room
import com.nutriai.data.local.cache.CacheDao
import com.nutriai.data.local.cache.NutriDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NutriDatabase =
        Room.databaseBuilder(context, NutriDatabase::class.java, "nutri-cache.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCacheDao(db: NutriDatabase): CacheDao = db.cacheDao()
}
