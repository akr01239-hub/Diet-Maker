package com.nutriai.data.remote

import com.nutriai.data.local.TokenStore
import com.nutriai.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * On a 401, transparently refreshes the access token using the stored refresh token and
 * retries the request. If refresh fails, tokens are cleared so the app returns to login.
 * This is why sessions no longer break after the 15-minute access-token expiry.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val refreshApi: Provider<RefreshApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up after one retry to avoid infinite loops.
        if (priorResponseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenStore.refreshToken() } ?: return null

        val tokens = try {
            runBlocking { refreshApi.get().refresh(RefreshRequest(refreshToken)).tokens }
        } catch (e: Exception) {
            runBlocking { tokenStore.clear() } // refresh invalid -> force re-login
            return null
        }

        runBlocking { tokenStore.save(tokens.accessToken, tokens.refreshToken) }
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${tokens.accessToken}")
            .build()
    }

    private fun priorResponseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
