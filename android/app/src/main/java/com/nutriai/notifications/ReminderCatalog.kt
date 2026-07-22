package com.nutriai.notifications

/** A single scheduled nudge. Times are local (device) time. */
data class ReminderJob(
    val key: String,
    val hour: Int,
    val minute: Int,
    val title: String,
    val text: String,
    /** 1=Mon … 7=Sun for weekly jobs; null = every day. */
    val weeklyDayIso: Int? = null,
)

/** Toggle groups the user controls in Settings. */
enum class ReminderGroup(val label: String, val subtitle: String) {
    MEALS("Meal reminders", "Breakfast, lunch & dinner nudges"),
    WATER("Hydration reminders", "Gentle water nudges through the day"),
    WEIGH_IN("Weekly weigh-in", "A Monday-morning check-in nudge"),
}

/** The fixed catalog of reminders per group. Deterministic, zero-cost, all local. */
object ReminderCatalog {
    fun jobs(group: ReminderGroup): List<ReminderJob> = when (group) {
        ReminderGroup.MEALS -> listOf(
            ReminderJob("meal_breakfast", 8, 0, "🍳 Breakfast time", "Log your first meal to stay on target."),
            ReminderJob("meal_lunch", 13, 0, "🍽️ Lunch break", "Don't forget to log your lunch."),
            ReminderJob("meal_dinner", 20, 0, "🌙 Dinner", "Log dinner and see how your day added up."),
        )
        ReminderGroup.WATER -> listOf(
            ReminderJob("water_am", 11, 0, "💧 Hydration check", "Time for a glass of water — log it."),
            ReminderJob("water_pm", 16, 0, "💧 Stay hydrated", "A little water goes a long way. Log a glass."),
        )
        ReminderGroup.WEIGH_IN -> listOf(
            ReminderJob("weigh_in", 7, 30, "⚖️ Weekly weigh-in", "Log this week's weight to track your progress.", weeklyDayIso = 1),
        )
    }

    val allGroups: List<ReminderGroup> = ReminderGroup.entries
}
