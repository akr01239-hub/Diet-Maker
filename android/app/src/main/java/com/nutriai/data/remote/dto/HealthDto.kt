package com.nutriai.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthDto(
    val status: String,
    val service: String,
    val version: String,
    val aiProvider: String,
    val uptimeSeconds: Long,
)
