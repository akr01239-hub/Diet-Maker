package com.nutriai.data.remote

import com.nutriai.data.remote.dto.HealthDto
import retrofit2.http.GET

interface HealthApi {
    // Liveness endpoint lives at the server root, not under /api/v1.
    @GET("/health")
    suspend fun health(): HealthDto
}
