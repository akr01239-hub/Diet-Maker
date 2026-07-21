package com.nutriai.data.remote.dto

import kotlinx.serialization.Serializable

// ---- Auth ----
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long)

@Serializable
data class PublicUser(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
)

@Serializable
data class AuthResponse(val user: PublicUser, val tokens: Tokens)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokensResponse(val tokens: Tokens)

@Serializable
data class MeResponse(val user: PublicUser)

// ---- Profile ----
@Serializable
data class SensitiveData(
    val sex: String,
    val dob: String,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val targetDate: String? = null,
    val waistCm: Double? = null,
    val conditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val desiredWeeklyLossKg: Double? = null,
)

@Serializable
data class ProfileUpsertRequest(
    val heightCm: Double,
    val activityLevel: String,
    val goal: String,
    val dietType: String,
    val reducedMobility: Boolean = false,
    val sensitive: SensitiveData,
)

@Serializable
data class ProfileEnvelope(val profile: ProfileDto?)

@Serializable
data class ProfileDto(
    val id: String,
    val heightCm: Double,
    val activityLevel: String,
    val goal: String,
    val dietType: String,
    val reducedMobility: Boolean,
    val sensitive: SensitiveData? = null,
)

// ---- Calc ----
@Serializable
data class Flag(val code: String, val severity: String, val message: String)

@Serializable
data class CalcResult(
    val bmi: Double,
    val bmiCategory: String,
    val bmr: Double,
    val tdee: Double,
    val idealWeightMinKg: Double,
    val idealWeightMaxKg: Double,
    val bodyFatEstimate: Double,
    val dailyKcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val fiberG: Double,
    val waterMl: Double,
    val safeWeeklyDeltaKg: Double,
    val requiresSupervision: Boolean,
    val weightLossBlocked: Boolean,
    val flags: List<Flag> = emptyList(),
)

@Serializable
data class CalcEnvelope(val result: CalcResult?)

// ---- Dashboard ----
@Serializable
data class DashCalories(
    val consumed: Double,
    val target: Double? = null,
    val remaining: Double? = null,
    val percent: Double? = null,
)

@Serializable
data class DashMetric(val consumed: Double? = null, val consumedMl: Double? = null, val target: Double? = null, val targetMl: Double? = null, val percent: Double? = null)

@Serializable
data class WeightTrend(val latestKg: Double? = null, val firstKg: Double? = null, val deltaKg: Double? = null)

@Serializable
data class Dashboard(
    val date: String,
    val calories: DashCalories,
    val protein: DashMetric,
    val water: DashMetric,
    val streakDays: Int,
    val bmi: Double? = null,
    val weight: WeightTrend,
)

@Serializable
data class DashboardEnvelope(val dashboard: Dashboard)

// ---- Plan ----
@Serializable
data class MealItem(val foodId: String, val name: String, val grams: Double, val kcal: Double, val proteinG: Double)

@Serializable
data class Meal(val slot: String, val items: List<MealItem>, val kcal: Double, val proteinG: Double)

@Serializable
data class DayTotals(val kcal: Double, val proteinG: Double, val carbG: Double, val fatG: Double, val fiberG: Double, val sodiumMg: Double)

@Serializable
data class DayPlan(val dayIndex: Int, val meals: List<Meal>, val totals: DayTotals)

@Serializable
data class PlanTargets(val dailyKcal: Double, val proteinG: Double)

@Serializable
data class PlanDto(val id: String? = null, val days: List<DayPlan> = emptyList(), val targets: PlanTargets? = null)

@Serializable
data class PlanEnvelope(val plan: PlanDto?)

// ---- Chat ----
@Serializable
data class ChatRequest(val message: String)

@Serializable
data class ChatReply(val intent: String, val reply: String, val sources: List<String> = emptyList())

@Serializable
data class ChatEnvelope(val reply: ChatReply)

// ---- Food logging ----
@Serializable
data class FoodLogRequest(
    val mealSlot: String,
    val grams: Double,
    val foodId: String? = null,
    val foodName: String? = null,
    val entryMethod: String = "text",
)

@Serializable
data class WaterLogRequest(val amountMl: Int)
