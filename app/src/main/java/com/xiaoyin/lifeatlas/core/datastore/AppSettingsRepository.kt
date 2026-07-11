package com.xiaoyin.lifeatlas.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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

data class RecordPreferenceSettings(
    val defaultMood: String = "平静",
    val defaultTags: String = "",
    val photoSaveStrategy: String = "缓存缩略图，保留原图引用"
)

data class CloudSyncSettings(
    val enabled: Boolean = false,
    val provider: String = "岁迹云端",
    val lastPreparedAt: Long? = null
)

data class ReminderSettings(
    val memoryOfTheDayEnabled: Boolean = false,
    val weeklyReviewEnabled: Boolean = false
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

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val recordPreferences: Flow<RecordPreferenceSettings> = dataStore.data.map { preferences ->
        RecordPreferenceSettings(
            defaultMood = preferences[DEFAULT_MOOD] ?: "平静",
            defaultTags = preferences[DEFAULT_TAGS] ?: "",
            photoSaveStrategy = preferences[PHOTO_SAVE_STRATEGY] ?: "缓存缩略图，保留原图引用"
        )
    }

    val cloudSyncSettings: Flow<CloudSyncSettings> = dataStore.data.map { preferences ->
        CloudSyncSettings(
            enabled = preferences[CLOUD_SYNC_ENABLED] ?: false,
            provider = preferences[CLOUD_SYNC_PROVIDER] ?: "岁迹云端",
            lastPreparedAt = preferences[CLOUD_SYNC_LAST_PREPARED_AT]
        )
    }

    val reminderSettings: Flow<ReminderSettings> = dataStore.data.map { preferences ->
        ReminderSettings(
            memoryOfTheDayEnabled = preferences[REMINDER_MEMORY_OF_DAY] ?: false,
            weeklyReviewEnabled = preferences[REMINDER_WEEKLY_REVIEW] ?: false
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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateRecordPreferences(defaultMood: String, defaultTags: String, photoSaveStrategy: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_MOOD] = defaultMood.trim().ifBlank { "平静" }
            preferences[DEFAULT_TAGS] = defaultTags.trim()
            preferences[PHOTO_SAVE_STRATEGY] = photoSaveStrategy.trim().ifBlank { "缓存缩略图，保留原图引用" }
        }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_ENABLED] = enabled
            preferences[CLOUD_SYNC_PROVIDER] = "岁迹云端"
        }
    }

    suspend fun markCloudSyncPrepared() {
        dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_LAST_PREPARED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateReminderSettings(memoryOfTheDayEnabled: Boolean, weeklyReviewEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMINDER_MEMORY_OF_DAY] = memoryOfTheDayEnabled
            preferences[REMINDER_WEEKLY_REVIEW] = weeklyReviewEnabled
        }
    }

    private companion object {
        val LOCAL_FIRST_ENABLED = booleanPreferencesKey("local_first_enabled")
        val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val PROFILE_SIGNATURE = stringPreferencesKey("profile_signature")
        val PROFILE_AVATAR_URI = stringPreferencesKey("profile_avatar_uri")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_MOOD = stringPreferencesKey("default_mood")
        val DEFAULT_TAGS = stringPreferencesKey("default_tags")
        val PHOTO_SAVE_STRATEGY = stringPreferencesKey("photo_save_strategy")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val CLOUD_SYNC_PROVIDER = stringPreferencesKey("cloud_sync_provider")
        val CLOUD_SYNC_LAST_PREPARED_AT = longPreferencesKey("cloud_sync_last_prepared_at")
        val REMINDER_MEMORY_OF_DAY = booleanPreferencesKey("reminder_memory_of_day")
        val REMINDER_WEEKLY_REVIEW = booleanPreferencesKey("reminder_weekly_review")
    }
}
