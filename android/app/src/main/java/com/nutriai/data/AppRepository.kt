package com.nutriai.data

import com.nutriai.data.local.TokenStore
import com.nutriai.data.remote.NutriApi
import com.nutriai.data.remote.dto.CalcResult
import com.nutriai.data.remote.dto.ChatReply
import com.nutriai.data.remote.dto.ChatRequest
import com.nutriai.data.remote.dto.Dashboard
import com.nutriai.data.remote.dto.FoodLogRequest
import com.nutriai.data.remote.dto.LoginRequest
import com.nutriai.data.remote.dto.PlanDto
import com.nutriai.data.remote.dto.ProfileUpsertRequest
import com.nutriai.data.remote.dto.RegisterRequest
import com.nutriai.data.remote.dto.WaterLogRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val api: NutriApi,
    private val tokenStore: TokenStore,
) {
    val isLoggedIn: Flow<Boolean> = tokenStore.accessTokenFlow.map { !it.isNullOrBlank() }

    suspend fun register(email: String, password: String, first: String, last: String): Result<Unit> =
        runCatching {
            val res = api.register(RegisterRequest(email, password, first, last))
            tokenStore.save(res.tokens.accessToken, res.tokens.refreshToken)
        }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val res = api.login(LoginRequest(email, password))
        tokenStore.save(res.tokens.accessToken, res.tokens.refreshToken)
    }

    suspend fun logout() = tokenStore.clear()

    suspend fun saveProfile(body: ProfileUpsertRequest): Result<Unit> = runCatching {
        api.putProfile(body)
        api.computeCalc() // refresh the CalcResult snapshot
        Unit
    }

    suspend fun latestCalc(): Result<CalcResult?> = runCatching { api.latestCalc().result }

    suspend fun dashboard(): Result<Dashboard> = runCatching { api.dashboard().dashboard }

    suspend fun latestPlan(): Result<PlanDto?> = runCatching { api.latestPlan().plan }

    suspend fun generatePlan(): Result<PlanDto?> = runCatching { api.generatePlan(mapOf("days" to 7)).plan }

    suspend fun chat(message: String): Result<ChatReply> = runCatching { api.chat(ChatRequest(message)).reply }

    suspend fun searchFoods(q: String): Result<List<com.nutriai.data.remote.dto.FoodDto>> = runCatching {
        api.foods(if (q.isBlank()) null else q).foods
    }

    suspend fun logFood(slot: String, foodId: String, grams: Double): Result<Unit> = runCatching {
        api.logFood(FoodLogRequest(mealSlot = slot, grams = grams, foodId = foodId))
    }

    suspend fun logWater(ml: Int): Result<Unit> = runCatching { api.logWater(WaterLogRequest(ml)) }
}
