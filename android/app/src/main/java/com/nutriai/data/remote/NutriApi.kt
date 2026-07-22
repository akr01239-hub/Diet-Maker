package com.nutriai.data.remote

import com.nutriai.data.remote.dto.AuthResponse
import com.nutriai.data.remote.dto.BarcodeEnvelope
import com.nutriai.data.remote.dto.CalcEnvelope
import com.nutriai.data.remote.dto.ChatEnvelope
import com.nutriai.data.remote.dto.ChatRequest
import com.nutriai.data.remote.dto.CheckinCreatedEnvelope
import com.nutriai.data.remote.dto.CheckinRequest
import com.nutriai.data.remote.dto.CheckinsEnvelope
import com.nutriai.data.remote.dto.DashboardEnvelope
import com.nutriai.data.remote.dto.FamilyEnvelope
import com.nutriai.data.remote.dto.FamilyMemberCreatedEnvelope
import com.nutriai.data.remote.dto.FamilyMemberRequest
import com.nutriai.data.remote.dto.FoodLogRequest
import com.nutriai.data.remote.dto.FoodsEnvelope
import com.nutriai.data.remote.dto.GamificationEnvelope
import com.nutriai.data.remote.dto.GroceryEnvelope
import com.nutriai.data.remote.dto.LoginRequest
import com.nutriai.data.remote.dto.PlanEnvelope
import com.nutriai.data.remote.dto.ProfileEnvelope
import com.nutriai.data.remote.dto.ProfileUpsertRequest
import com.nutriai.data.remote.dto.RegisterRequest
import com.nutriai.data.remote.dto.ReportEnvelope
import com.nutriai.data.remote.dto.WaterLogRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    @GET("foods/search")
    suspend fun foodsSearch(@Query("q") q: String?): FoodsEnvelope

    @POST("logs/food")
    suspend fun logFood(@Body body: FoodLogRequest)

    @GET("logs/food")
    suspend fun todayLogs(): com.nutriai.data.remote.dto.FoodLogsEnvelope

    @POST("logs/water")
    suspend fun logWater(@Body body: WaterLogRequest)

    @retrofit2.http.Streaming
    @GET("reports/weekly.pdf")
    suspend fun weeklyPdf(): okhttp3.ResponseBody

    // ---- Check-ins ----
    @POST("checkins")
    suspend fun createCheckin(@Body body: CheckinRequest): CheckinCreatedEnvelope

    @GET("checkins")
    suspend fun checkins(): CheckinsEnvelope

    // ---- Grocery ----
    @GET("grocery")
    suspend fun grocery(): GroceryEnvelope

    // ---- Reports ----
    @GET("reports/weekly.json")
    suspend fun weeklyReport(): ReportEnvelope

    // ---- Gamification ----
    @GET("gamification")
    suspend fun gamification(): GamificationEnvelope

    // ---- Family ----
    @GET("family")
    suspend fun family(): FamilyEnvelope

    @POST("family")
    suspend fun addFamilyMember(@Body body: FamilyMemberRequest): FamilyMemberCreatedEnvelope

    @GET("family/{id}/calc")
    suspend fun familyCalc(@Path("id") id: String): CalcEnvelope

    // ---- Barcode ----
    @GET("barcode/{code}")
    suspend fun barcode(@Path("code") code: String): BarcodeEnvelope

    // ---- Workout ----
    @GET("exercise-plan")
    suspend fun exercisePlan(): com.nutriai.data.remote.dto.WorkoutEnvelope

    @POST("exercise-logs")
    suspend fun logExercise(@Body body: com.nutriai.data.remote.dto.ExerciseLogRequest): com.nutriai.data.remote.dto.ExerciseLogEnvelope

    @GET("exercise-logs")
    suspend fun exerciseLogs(@Query("date") date: String?): com.nutriai.data.remote.dto.ExerciseLogsEnvelope

    @GET("exercise-logs/last")
    suspend fun exerciseLast(): com.nutriai.data.remote.dto.LastPerformanceEnvelope

    @retrofit2.http.DELETE("exercise-logs/{id}")
    suspend fun deleteExerciseLog(@Path("id") id: String)

    // ---- Account ----
    @GET("auth/me")
    suspend fun me(): com.nutriai.data.remote.dto.MeResponse

    @retrofit2.http.DELETE("me")
    suspend fun deleteAccount()
}
