package com.nutriai.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderStore by preferencesDataStore(name = "reminders")

/** Persisted on/off state per reminder group. Meals + water default ON on first run. */
@Singleton
class ReminderPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun key(group: ReminderGroup) = booleanPreferencesKey("enabled_${group.name}")

    private fun default(group: ReminderGroup) = group != ReminderGroup.WEIGH_IN

    val settings: Flow<Map<ReminderGroup, Boolean>> = context.reminderStore.data.map { prefs ->
        ReminderCatalog.allGroups.associateWith { g -> prefs[key(g)] ?: default(g) }
    }

    suspend fun isEnabled(group: ReminderGroup): Boolean =
        context.reminderStore.data.first()[key(group)] ?: default(group)

    suspend fun setEnabled(group: ReminderGroup, enabled: Boolean) {
        context.reminderStore.edit { it[key(group)] = enabled }
    }

    suspend fun snapshot(): Map<ReminderGroup, Boolean> = settings.first()
}
