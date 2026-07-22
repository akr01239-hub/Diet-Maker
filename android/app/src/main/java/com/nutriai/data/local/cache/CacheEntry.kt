package com.nutriai.data.local.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single cached JSON blob keyed by name (e.g. "dashboard", "plan"). */
@Entity(tableName = "cache")
data class CacheEntry(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long,
)
