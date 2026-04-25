package com.codexlabs.auroratv.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codexlabs.auroratv.data.AppSettings
import com.codexlabs.auroratv.data.BufferProfile
import com.codexlabs.auroratv.data.PreferredPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.auroraDataStore by preferencesDataStore(name = "aurora_settings")

class SettingsRepository(
    private val context: Context,
) {
    private object Keys {
        val providerBaseUrl = stringPreferencesKey("provider_base_url")
        val providerUsername = stringPreferencesKey("provider_username")
        val providerPassword = stringPreferencesKey("provider_password")
        val autoSyncEnabled = booleanPreferencesKey("auto_sync_enabled")
        val adultContentEnabled = booleanPreferencesKey("adult_content_enabled")
        val preferredPlayer = stringPreferencesKey("preferred_player")
        val bufferProfile = stringPreferencesKey("buffer_profile")
        val epgWindowHours = intPreferencesKey("epg_window_hours")
        val parentalPin = stringPreferencesKey("parental_pin")
        val lastSyncEpochMillis = longPreferencesKey("last_sync_epoch_millis")
    }

    val settings: Flow<AppSettings> = context.auroraDataStore.data.map(::mapSettings)

    suspend fun saveProviderCredentials(
        baseUrl: String,
        username: String,
        password: String,
    ) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.providerBaseUrl] = sanitizeBaseUrl(baseUrl)
            prefs[Keys.providerUsername] = username.trim()
            prefs[Keys.providerPassword] = password.trim()
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.autoSyncEnabled] = enabled
        }
    }

    suspend fun setAdultContentEnabled(enabled: Boolean) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.adultContentEnabled] = enabled
        }
    }

    suspend fun setPreferredPlayer(preferredPlayer: PreferredPlayer) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.preferredPlayer] = preferredPlayer.name
        }
    }

    suspend fun setBufferProfile(bufferProfile: BufferProfile) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.bufferProfile] = bufferProfile.name
        }
    }

    suspend fun setEpgWindowHours(hours: Int) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.epgWindowHours] = hours.coerceIn(12, 96)
        }
    }

    suspend fun setParentalPin(pin: String) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.parentalPin] = pin.filter(Char::isDigit).take(4)
        }
    }

    suspend fun markSyncCompleted(epochMillis: Long) {
        context.auroraDataStore.edit { prefs ->
            prefs[Keys.lastSyncEpochMillis] = epochMillis
        }
    }

    private fun mapSettings(prefs: Preferences): AppSettings {
        return AppSettings(
            providerBaseUrl = prefs[Keys.providerBaseUrl].orEmpty(),
            providerUsername = prefs[Keys.providerUsername].orEmpty(),
            providerPassword = prefs[Keys.providerPassword].orEmpty(),
            autoSyncEnabled = prefs[Keys.autoSyncEnabled] ?: true,
            adultContentEnabled = prefs[Keys.adultContentEnabled] ?: false,
            preferredPlayer = PreferredPlayer.from(prefs[Keys.preferredPlayer]),
            bufferProfile = BufferProfile.from(prefs[Keys.bufferProfile]),
            epgWindowHours = prefs[Keys.epgWindowHours] ?: 48,
            parentalPin = prefs[Keys.parentalPin].orEmpty(),
            lastSyncEpochMillis = prefs[Keys.lastSyncEpochMillis],
        )
    }

    private fun sanitizeBaseUrl(value: String): String {
        return value.trim().trimEnd('/')
    }
}
