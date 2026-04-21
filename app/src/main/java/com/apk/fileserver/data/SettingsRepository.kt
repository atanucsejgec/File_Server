package com.apk.fileserver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.apk.fileserver.viewmodel.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property - single instance
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    // %%% KEYS %%%
    companion object {
        val KEY_SERVER_PORT       = intPreferencesKey("server_port")
        val KEY_PASSWORD_ENABLED  = booleanPreferencesKey("password_enabled")
        val KEY_PASSWORD          = stringPreferencesKey("password")
        val KEY_DARK_MODE_WEB     = booleanPreferencesKey("dark_mode_web")
        val KEY_SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val KEY_AUTO_START_SERVER = booleanPreferencesKey("auto_start_server")
    }

    // %%% READ - Flow of AppSettings %%%
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            // If DataStore throws, emit empty preferences
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                serverPort       = preferences[KEY_SERVER_PORT]       ?: 8080,
                passwordEnabled  = preferences[KEY_PASSWORD_ENABLED]  ?: false,
                password         = preferences[KEY_PASSWORD]          ?: "",
                darkModeWeb      = preferences[KEY_DARK_MODE_WEB]     ?: false,
                showHiddenFiles  = preferences[KEY_SHOW_HIDDEN_FILES] ?: false,
                autoStartServer  = preferences[KEY_AUTO_START_SERVER] ?: false
            )
        }

    // %%% WRITE %%%
    suspend fun updateServerPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_PORT] = port.coerceIn(1024, 65535)
        }
    }

    suspend fun updatePasswordEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PASSWORD_ENABLED] = enabled
        }
    }

    suspend fun updatePassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PASSWORD] = password
        }
    }

    suspend fun updateDarkModeWeb(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE_WEB] = enabled
        }
    }

    suspend fun updateShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_HIDDEN_FILES] = show
        }
    }

    suspend fun updateAutoStartServer(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_SERVER] = enabled
        }
    }
}