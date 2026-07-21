package com.nutriai.domain.model

/** Domain model for the API liveness result, decoupled from the network DTO. */
data class HealthStatus(
    val service: String,
    val version: String,
    val aiProvider: String,
)
