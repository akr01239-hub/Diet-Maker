package com.nutriai.notifications

/** A single scheduled nudge. Times are local (device) time. */
data class ReminderJob(
    val key: String,
    val hour: Int,
    val minute: Int,
    val title: String,
    val text: String,
    /** Which home tab to open when the notification is tapped: 0 Home, 1 Plan, 2 Log. */
    val tab: Int = 0,
    /** 1=Mon … 7=Sun for weekly jobs; null = every day. */
    val weeklyDayIso: Int? = null,
)

/** Toggle groups the user controls in Settings. */
enum class ReminderGroup(val label: String, val subtitle: String) {
    MEALS("Meal reminders", "Breakfast, lunch & dinner windows"),
    WATER("Hydration reminders", "Gentle water nudges through the day"),
    WORKOUT("5 AM workout call", "Early wake-up nudge — tap to open your plan"),
    WEIGH_IN("Weekly weigh-in", "A Monday-morning check-in nudge"),
}

/** The fixed catalog of reminders per group. Deterministic, zero-cost, all local. */
object ReminderCatalog {
    fun jobs(group: ReminderGroup): List<ReminderJob> = when (group) {
        // Meal windows: breakfast 8–10, lunch 12:30–2, dinner by 7–8:30 → remind at the window start.
        ReminderGroup.MEALS -> listOf(
            ReminderJob("meal_breakfast", 8, 0, "🍳 Breakfast (8–10 am)", "Fuel up and log your breakfast.", tab = 2),
            ReminderJob("meal_lunch", 12, 30, "🍽️ Lunch (12:30–2 pm)", "Time for lunch — log it to stay on target.", tab = 2),
            ReminderJob("meal_dinner", 19, 0, "🌙 Dinner (7–8:30 pm)", "Aim to finish dinner by 8:30. Log your meal.", tab = 2),
        )
        ReminderGroup.WATER -> listOf(
            ReminderJob("water_am", 11, 0, "💧 Hydration check", "Time for a glass of water — log it.", tab = 0),
            ReminderJob("water_pm", 16, 0, "💧 Stay hydrated", "A little water goes a long way. Log a glass.", tab = 0),
        )
        ReminderGroup.WORKOUT -> listOf(
            ReminderJob("workout_5am", 5, 0, "🏋️ 5 AM — time to train", "Rise and grind. Tap to open today's workout.", tab = 1),
        )
        ReminderGroup.WEIGH_IN -> listOf(
            ReminderJob("weigh_in", 7, 30, "⚖️ Weekly weigh-in", "Log this week's weight to track your progress.", tab = 0, weeklyDayIso = 1),
        )
    }

    val allGroups: List<ReminderGroup> = ReminderGroup.entries
}
