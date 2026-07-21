package com.nutriai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

// App-private DataStore. Tokens live in sandboxed storage; a Keystore-backed
// EncryptedSharedPreferences upgrade is tracked as a hardening item.
private val Context.dataStore by preferencesDataStore(name = "auth")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[accessKey] }

    suspend fun save(access: String, refresh: String) {
        context.dataStore.edit {
            it[accessKey] = access
            it[refreshKey] = refresh
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun accessToken(): String? = context.dataStore.data.first()[accessKey]

    suspend fun refreshToken(): String? = context.dataStore.data.first()[refreshKey]

    /** Blocking read for the OkHttp interceptor (runs on a background thread). */
    fun accessTokenBlocking(): String? = runBlocking { accessToken() }
}
