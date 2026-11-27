package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexoraDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexora_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACTIVE_BUSINESS_ID = stringPreferencesKey("active_business_id")
    }

    val darkThemeFlow: Flow<Boolean?> = context.nexoraDataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME]
    }

    val activeBusinessIdFlow: Flow<String?> = context.nexoraDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_BUSINESS_ID]
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.nexoraDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setActiveBusinessId(businessId: String) {
        context.nexoraDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_BUSINESS_ID] = businessId
        }
    }
}
