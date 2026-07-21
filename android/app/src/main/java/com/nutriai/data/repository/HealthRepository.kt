package com.nutriai.data.repository

import com.nutriai.data.remote.HealthApi
import com.nutriai.domain.model.HealthStatus
import javax.inject.Inject
import javax.inject.Singleton

interface HealthRepository {
    /** Returns the API health, or throws on network/parse failure. */
    suspend fun check(): HealthStatus
}

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val api: HealthApi,
) : HealthRepository {
    override suspend fun check(): HealthStatus {
        val dto = api.health()
        return HealthStatus(
            service = dto.service,
            version = dto.version,
            aiProvider = dto.aiProvider,
        )
    }
}
