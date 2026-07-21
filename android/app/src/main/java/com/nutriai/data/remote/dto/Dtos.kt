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
data class DashMacros(
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

@Serializable
data class Dashboard(
    val date: String,
    val calories: DashCalories,
    val protein: DashMetric,
    val water: DashMetric,
    val macros: DashMacros = DashMacros(),
    val streakDays: Int,
    val bmi: Double? = null,
    val weight: WeightTrend,
)

@Serializable
data class FoodLogEntry(
    val id: String,
    val foodName: String,
    val mealSlot: String,
    val grams: Double,
    val kcal: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

@Serializable
data class FoodLogsEnvelope(val entries: List<FoodLogEntry> = emptyList())

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
data class FoodLogPer100g(
    val kcal: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

@Serializable
data class FoodLogRequest(
    val mealSlot: String,
    val grams: Double,
    val foodId: String? = null,
    val foodName: String? = null,
    val per100g: FoodLogPer100g? = null,
    val entryMethod: String = "text",
)

@Serializable
data class WaterLogRequest(val amountMl: Int)

@Serializable
data class FoodDto(
    val id: String,
    val name: String,
    val kcal: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val typicalServingG: Double = 100.0,
    val source: String = "local",
)

@Serializable
data class FoodsEnvelope(val foods: List<FoodDto> = emptyList())

// ---- Weekly check-ins ----
@Serializable
data class CheckinRequest(
    val weightKg: Double,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val hipCm: Double? = null,
    val energy: Int? = null,
    val sleepHours: Double? = null,
    val mood: Int? = null,
    val pain: Int? = null,
    val notes: String? = null,
)

@Serializable
data class Measurements(val weightKg: Double, val waistCm: Double? = null)

@Serializable
data class CheckinDto(
    val id: String,
    val date: String,
    val measurements: Measurements? = null,
    val energy: Int? = null,
    val sleepHours: Double? = null,
    val mood: Int? = null,
    val pain: Int? = null,
    val notes: String? = null,
)

@Serializable
data class CheckinsEnvelope(val checkins: List<CheckinDto> = emptyList())

@Serializable
data class CheckinCreated(val id: String, val date: String)

@Serializable
data class CheckinCreatedEnvelope(val checkin: CheckinCreated)

// ---- Grocery ----
@Serializable
data class GroceryItem(
    val foodId: String,
    val name: String,
    val totalGrams: Double,
    val servings: Double,
    val costTier: Int,
)

@Serializable
data class Grocery(
    val items: List<GroceryItem> = emptyList(),
    val estimatedCostTierTotal: Double = 0.0,
    val trimmed: List<String> = emptyList(),
)

@Serializable
data class GroceryEnvelope(val grocery: Grocery)

// ---- Reports ----
@Serializable
data class ReportDay(val date: String, val kcal: Double, val proteinG: Double)

@Serializable
data class ReportTargets(val dailyKcal: Double, val proteinG: Double, val waterMl: Double)

@Serializable
data class WeeklyReport(
    val name: String,
    val generatedAt: String,
    val targets: ReportTargets? = null,
    val bmi: Double? = null,
    val latestWeightKg: Double? = null,
    val weightDeltaKg: Double? = null,
    val days: List<ReportDay> = emptyList(),
    val avgKcal: Double? = null,
    val adherencePct: Double? = null,
    val disclaimer: String,
)

@Serializable
data class ReportEnvelope(val report: WeeklyReport)

// ---- Gamification ----
@Serializable
data class Badge(val code: String, val title: String, val description: String, val earned: Boolean)

@Serializable
data class Gamification(val earnedCount: Int, val total: Int, val badges: List<Badge> = emptyList())

@Serializable
data class GamificationEnvelope(val gamification: Gamification)

// ---- Family ----
@Serializable
data class FamilyMemberRequest(
    val firstName: String,
    val relation: String? = null,
    val heightCm: Double,
    val activityLevel: String,
    val goal: String,
    val dietType: String,
    val sensitive: SensitiveData,
)

@Serializable
data class FamilyMemberDto(
    val id: String,
    val firstName: String,
    val relation: String? = null,
    val heightCm: Double? = null,
    val activityLevel: String? = null,
    val goal: String? = null,
    val dietType: String? = null,
)

@Serializable
data class FamilyEnvelope(val members: List<FamilyMemberDto> = emptyList())

@Serializable
data class FamilyMemberCreated(val id: String, val firstName: String, val relation: String? = null)

@Serializable
data class FamilyMemberCreatedEnvelope(val member: FamilyMemberCreated)

// ---- Barcode ----
@Serializable
data class BarcodePer100g(
    val kcal: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

@Serializable
data class BarcodeFood(val code: String, val name: String, val per100g: BarcodePer100g)

@Serializable
data class BarcodeEnvelope(val food: BarcodeFood)
