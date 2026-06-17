package com.example.masterenglishfluency.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.masterenglishfluency.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            AppSettings(
                darkModeEnabled = preferences[Keys.DARK_MODE] ?: false,
                notificationsEnabled = preferences[Keys.NOTIFICATIONS] ?: true,
                profileName = preferences[Keys.PROFILE_NAME] ?: "Learner"
            )
        }
        .distinctUntilChanged()

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[Keys.DARK_MODE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setProfileName(name: String) {
        context.settingsDataStore.edit { preferences -> preferences[Keys.PROFILE_NAME] = name.ifBlank { "Learner" } }
    }
}
