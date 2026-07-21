package com.nutriai.data.remote

import com.nutriai.data.remote.dto.RefreshRequest
import com.nutriai.data.remote.dto.TokensResponse
import retrofit2.http.Body
import retrofit2.http.POST

/** Separate API (no auth interceptor/authenticator) used only to refresh tokens. */
interface RefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokensResponse
}
