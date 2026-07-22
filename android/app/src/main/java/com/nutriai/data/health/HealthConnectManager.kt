package com.nutriai.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads step data from Health Connect. Fully optional and free — every call degrades to 0
 * when Health Connect is unavailable or the read permission hasn't been granted, so the app
 * never crashes on devices without it.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val readPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun clientOrNull(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasStepPermission(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(readPermissions)
    }

    /**
     * Total steps today (device local day). Uses the AGGREGATE API, which de-duplicates
     * steps across sources (phone sensor, Google Fit, etc.) by priority — summing raw
     * records double-counts overlapping data. Returns 0 if unavailable/denied.
     */
    suspend fun readTodaySteps(): Long {
        val client = clientOrNull() ?: return 0L
        if (!hasStepPermission()) return 0L
        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now().atStartOfDay(zone).toInstant()
            val end = Instant.now()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
