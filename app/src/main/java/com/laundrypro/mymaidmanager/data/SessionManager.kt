package com.laundrypro.mymaidmanager.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SessionManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "auth_session_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val AUTH_TOKEN = "auth_token"
    }

    /**
     * Saves the authentication token to encrypted storage.
     */
    fun saveAuthToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString(AUTH_TOKEN, token)
        editor.apply()
    }

    /**
     * Fetches the authentication token from encrypted storage.
     * Returns null if no token is found.
     */
    fun fetchAuthToken(): String? {
        return sharedPreferences.getString(AUTH_TOKEN, null)
    }

    /**
     * Clears the authentication token from storage.
     */
    fun clearAuthToken() {
        val editor = sharedPreferences.edit()
        editor.remove(AUTH_TOKEN)
        editor.apply()
    }
}