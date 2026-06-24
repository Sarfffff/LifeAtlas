package com.xiaoyin.lifeatlas.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "lifeatlas_settings")

class AppSettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val localFirstEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOCAL_FIRST_ENABLED] ?: true
    }

    suspend fun setLocalFirstEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCAL_FIRST_ENABLED] = enabled
        }
    }

    private companion object {
        val LOCAL_FIRST_ENABLED = booleanPreferencesKey("local_first_enabled")
    }
}
