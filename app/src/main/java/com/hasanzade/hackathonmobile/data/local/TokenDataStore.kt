package com.hasanzade.hackathonmobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenDataStore(private val context: Context) {

    companion object {
        private val TOKEN_KEY        = stringPreferencesKey("access_token")
        private val ROLE_KEY         = stringPreferencesKey("user_role")
        private val USER_ID_KEY      = stringPreferencesKey("user_id")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val FILIAL_KEY       = stringPreferencesKey("filial")
        private val DEPARTMENT_KEY   = stringPreferencesKey("department")
    }

    suspend fun saveSession(
        token: String,
        role: String,
        userId: String,
        displayName: String,
        filial: String,
        department: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY]        = token
            prefs[ROLE_KEY]         = role
            prefs[USER_ID_KEY]      = userId
            prefs[DISPLAY_NAME_KEY] = displayName
            prefs[FILIAL_KEY]       = filial
            prefs[DEPARTMENT_KEY]   = department ?: ""
        }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.map { it[TOKEN_KEY] }.first()

    suspend fun getRole(): String? =
        context.dataStore.data.map { it[ROLE_KEY] }.first()

    suspend fun getUserId(): String? =
        context.dataStore.data.map { it[USER_ID_KEY] }.first()

    suspend fun getDisplayName(): String? =
        context.dataStore.data.map { it[DISPLAY_NAME_KEY] }.first()

    suspend fun getFilial(): String? =
        context.dataStore.data.map { it[FILIAL_KEY] }.first()

    suspend fun getDepartment(): String? =
        context.dataStore.data.map { it[DEPARTMENT_KEY] }.first()
            .takeIf { it?.isNotEmpty() == true }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null
}