package com.nutriai.data.remote

import com.nutriai.data.remote.dto.AuthResponse
import com.nutriai.data.remote.dto.CalcEnvelope
import com.nutriai.data.remote.dto.ChatEnvelope
import com.nutriai.data.remote.dto.ChatRequest
import com.nutriai.data.remote.dto.DashboardEnvelope
import com.nutriai.data.remote.dto.FoodLogRequest
import com.nutriai.data.remote.dto.FoodsEnvelope
import com.nutriai.data.remote.dto.LoginRequest
import com.nutriai.data.remote.dto.PlanEnvelope
import com.nutriai.data.remote.dto.ProfileEnvelope
import com.nutriai.data.remote.dto.ProfileUpsertRequest
import com.nutriai.data.remote.dto.RegisterRequest
import com.nutriai.data.remote.dto.WaterLogRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface NutriApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("profile")
    suspend fun getProfile(): ProfileEnvelope

    @PUT("profile")
    suspend fun putProfile(@Body body: ProfileUpsertRequest): ProfileEnvelope

    @POST("calc")
    suspend fun computeCalc(): CalcEnvelope

    @GET("calc/latest")
    suspend fun latestCalc(): CalcEnvelope

    @GET("dashboard")
    suspend fun dashboard(): DashboardEnvelope

    @POST("plan")
    suspend fun generatePlan(@Body body: Map<String, Int>): PlanEnvelope

    @GET("plan/latest")
    suspend fun latestPlan(): PlanEnvelope

    @POST("chat")
    suspend fun chat(@Body body: ChatRequest): ChatEnvelope

    @GET("foods")
    suspend fun foods(@Query("q") q: String?): FoodsEnvelope

    @POST("logs/food")
    suspend fun logFood(@Body body: FoodLogRequest)

    @POST("logs/water")
    suspend fun logWater(@Body body: WaterLogRequest)
}
