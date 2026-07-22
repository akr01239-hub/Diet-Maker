package com.nutriai.data

import com.nutriai.data.local.TokenStore
import com.nutriai.data.local.cache.CacheDao
import com.nutriai.data.local.cache.CacheEntry
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
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val api: NutriApi,
    private val tokenStore: TokenStore,
    private val cacheDao: CacheDao,
    private val json: Json,
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

    /** Loads the saved profile so onboarding/settings can pre-fill instead of starting blank. */
    suspend fun getProfile(): Result<com.nutriai.data.remote.dto.ProfileDto?> =
        runCatching { api.getProfile().profile }

    suspend fun latestCalc(): Result<CalcResult?> = runCatching { api.latestCalc().result }

    /** Fetches the dashboard; on network failure falls back to the last cached copy. */
    suspend fun dashboard(): Result<Dashboard> {
        val r = runCatching { api.dashboard().dashboard }
        return if (r.isSuccess) {
            val d = r.getOrThrow()
            runCatching { cacheDao.put(CacheEntry("dashboard", json.encodeToString(Dashboard.serializer(), d), System.currentTimeMillis())) }
            Result.success(d)
        } else {
            val cached = runCatching {
                cacheDao.get("dashboard")?.let { json.decodeFromString(Dashboard.serializer(), it.json) }
            }.getOrNull()
            if (cached != null) Result.success(cached) else r
        }
    }

    /** Fetches the latest plan; on network failure falls back to the last cached copy. */
    suspend fun latestPlan(): Result<PlanDto?> {
        val r = runCatching { api.latestPlan().plan }
        return if (r.isSuccess) {
            val p = r.getOrThrow()
            if (p != null) {
                runCatching { cacheDao.put(CacheEntry("plan", json.encodeToString(PlanDto.serializer(), p), System.currentTimeMillis())) }
            }
            Result.success(p)
        } else {
            val cached = runCatching {
                cacheDao.get("plan")?.let { json.decodeFromString(PlanDto.serializer(), it.json) }
            }.getOrNull()
            if (cached != null) Result.success(cached) else r
        }
    }

    suspend fun generatePlan(): Result<PlanDto?> = runCatching { api.generatePlan(mapOf("days" to 7)).plan }

    suspend fun swapMeal(dayIndex: Int, slot: String): Result<PlanDto?> = runCatching {
        api.swapMeal(com.nutriai.data.remote.dto.SwapMealRequest(dayIndex, slot)).plan
    }

    suspend fun chat(message: String): Result<ChatReply> = runCatching { api.chat(ChatRequest(message)).reply }

    suspend fun adaptation(): Result<com.nutriai.data.remote.dto.Adaptation> =
        runCatching { api.adaptation().adaptation }

    suspend fun guidance(): Result<com.nutriai.data.remote.dto.Guidance> =
        runCatching { api.guidance().guidance }

    suspend fun applyAdaptation(): Result<com.nutriai.data.remote.dto.AdaptApplyResponse> =
        runCatching { api.applyAdaptation() }

    suspend fun searchFoods(q: String): Result<List<com.nutriai.data.remote.dto.FoodDto>> = runCatching {
        api.foodsSearch(if (q.isBlank()) null else q).foods
    }

    suspend fun logFood(slot: String, foodId: String, grams: Double): Result<Unit> = runCatching {
        api.logFood(FoodLogRequest(mealSlot = slot, grams = grams, foodId = foodId))
    }

    /** Logs any food (local or USDA) by name + per-100g so no local DB row is required. */
    suspend fun logFoodItem(
        slot: String,
        food: com.nutriai.data.remote.dto.FoodDto,
        grams: Double,
    ): Result<Unit> = runCatching {
        api.logFood(
            FoodLogRequest(
                mealSlot = slot,
                grams = grams,
                foodName = food.name,
                per100g = com.nutriai.data.remote.dto.FoodLogPer100g(
                    kcal = food.kcal,
                    proteinG = food.proteinG,
                    carbG = food.carbG,
                    fatG = food.fatG,
                    fiberG = food.fiberG,
                    sugarG = food.sugarG,
                    sodiumMg = food.sodiumMg,
                ),
                entryMethod = if (food.source == "usda") "barcode" else "text",
            ),
        )
    }

    suspend fun logWater(ml: Int): Result<Unit> = runCatching { api.logWater(WaterLogRequest(ml)) }

    suspend fun todayLogs(): Result<List<com.nutriai.data.remote.dto.FoodLogEntry>> =
        runCatching { api.todayLogs().entries }

    suspend fun deleteFoodLog(id: String): Result<Unit> = runCatching { api.deleteFoodLog(id) }

    suspend fun reportPdfBytes(): Result<ByteArray> = runCatching { api.weeklyPdf().bytes() }

    // ---- Check-ins ----
    suspend fun createCheckin(body: com.nutriai.data.remote.dto.CheckinRequest): Result<Unit> =
        runCatching { api.createCheckin(body); Unit }

    suspend fun checkins(): Result<List<com.nutriai.data.remote.dto.CheckinDto>> =
        runCatching { api.checkins().checkins }

    // ---- Grocery ----
    suspend fun grocery(): Result<com.nutriai.data.remote.dto.Grocery> =
        runCatching { api.grocery().grocery }

    // ---- Reports ----
    suspend fun weeklyReport(): Result<com.nutriai.data.remote.dto.WeeklyReport> =
        runCatching { api.weeklyReport().report }

    // ---- Gamification ----
    suspend fun gamification(): Result<com.nutriai.data.remote.dto.Gamification> =
        runCatching { api.gamification().gamification }

    // ---- Family ----
    suspend fun family(): Result<List<com.nutriai.data.remote.dto.FamilyMemberDto>> =
        runCatching { api.family().members }

    suspend fun addFamilyMember(body: com.nutriai.data.remote.dto.FamilyMemberRequest): Result<Unit> =
        runCatching { api.addFamilyMember(body); Unit }

    suspend fun familyCalc(id: String): Result<com.nutriai.data.remote.dto.CalcResult?> =
        runCatching { api.familyCalc(id).result }

    // ---- Workout ----
    suspend fun exercisePlan(): Result<com.nutriai.data.remote.dto.WeeklyWorkout> =
        runCatching { api.exercisePlan().plan }

    // ---- Workout logging ----
    suspend fun logExercise(body: com.nutriai.data.remote.dto.ExerciseLogRequest): Result<Unit> =
        runCatching { api.logExercise(body); Unit }

    suspend fun exerciseLogs(date: String?): Result<List<com.nutriai.data.remote.dto.ExerciseLogDto>> =
        runCatching { api.exerciseLogs(date).entries }

    suspend fun lastPerformance(): Result<Map<String, com.nutriai.data.remote.dto.LastPerformance>> =
        runCatching { api.exerciseLast().last }

    suspend fun deleteExerciseLog(id: String): Result<Unit> =
        runCatching { api.deleteExerciseLog(id) }

    suspend fun deleteAccount(): Result<Unit> = runCatching { api.deleteAccount() }

    suspend fun me(): Result<com.nutriai.data.remote.dto.PublicUser> = runCatching { api.me().user }

    // ---- Barcode ----
    suspend fun barcode(code: String): Result<com.nutriai.data.remote.dto.BarcodeFood> =
        runCatching { api.barcode(code).food }

    /** Logs a scanned barcode food by name + per-100g (no local DB row needed). */
    suspend fun logBarcodeFood(
        slot: String,
        food: com.nutriai.data.remote.dto.BarcodeFood,
        grams: Double,
    ): Result<Unit> = runCatching {
        api.logFood(
            FoodLogRequest(
                mealSlot = slot,
                grams = grams,
                foodName = food.name,
                per100g = com.nutriai.data.remote.dto.FoodLogPer100g(
                    kcal = food.per100g.kcal,
                    proteinG = food.per100g.proteinG,
                    carbG = food.per100g.carbG,
                    fatG = food.per100g.fatG,
                    fiberG = food.per100g.fiberG,
                    sugarG = food.per100g.sugarG,
                    sodiumMg = food.per100g.sodiumMg,
                ),
                entryMethod = "barcode",
            ),
        )
    }
}
