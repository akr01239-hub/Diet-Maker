package com.nutriai.data.local.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class NutriDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
