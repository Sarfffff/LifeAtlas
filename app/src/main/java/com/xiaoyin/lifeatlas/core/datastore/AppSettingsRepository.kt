package com.xiaoyin.lifeatlas.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "lifeatlas_settings")

data class UserProfileSettings(
    val displayName: String = "旷野小旅人",
    val signature: String = "记录生活，探索世界",
    val avatarUri: String? = null
)

class AppSettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val localFirstEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOCAL_FIRST_ENABLED] ?: true
    }

    val userProfile: Flow<UserProfileSettings> = dataStore.data.map { preferences ->
        UserProfileSettings(
            displayName = preferences[PROFILE_DISPLAY_NAME] ?: "旷野小旅人",
            signature = preferences[PROFILE_SIGNATURE] ?: "记录生活，探索世界",
            avatarUri = preferences[PROFILE_AVATAR_URI]
        )
    }

    suspend fun setLocalFirstEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCAL_FIRST_ENABLED] = enabled
        }
    }

    suspend fun updateProfile(displayName: String, signature: String, avatarUri: String?) {
        dataStore.edit { preferences ->
            preferences[PROFILE_DISPLAY_NAME] = displayName.trim().ifBlank { "旷野小旅人" }
            preferences[PROFILE_SIGNATURE] = signature.trim().ifBlank { "记录生活，探索世界" }
            if (avatarUri.isNullOrBlank()) {
                preferences.remove(PROFILE_AVATAR_URI)
            } else {
                preferences[PROFILE_AVATAR_URI] = avatarUri
            }
        }
    }

    private companion object {
        val LOCAL_FIRST_ENABLED = booleanPreferencesKey("local_first_enabled")
        val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val PROFILE_SIGNATURE = stringPreferencesKey("profile_signature")
        val PROFILE_AVATAR_URI = stringPreferencesKey("profile_avatar_uri")
    }
}
