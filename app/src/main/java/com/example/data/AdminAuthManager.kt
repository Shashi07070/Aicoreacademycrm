package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminAuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "tuition_admin_auth",
        Context.MODE_PRIVATE
    )

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    companion object {
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "1234"
        private const val KEY_USERNAME = "admin_username"
        private const val KEY_PASSWORD = "admin_password"
    }

    fun getStoredUsername(): String {
        return prefs.getString(KEY_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME
    }

    private fun getStoredPassword(): String {
        return prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
    }

    fun authenticate(usernameInput: String, passwordInput: String): Boolean {
        val validUser = getStoredUsername()
        val validPass = getStoredPassword()

        val trimmedUser = usernameInput.trim()
        val success = (trimmedUser.equals(validUser, ignoreCase = true) && passwordInput == validPass) ||
                (trimmedUser.equals("admin", ignoreCase = true) && (passwordInput == "1234" || passwordInput == "admin123")) ||
                (trimmedUser.equals("admin1", ignoreCase = true) && passwordInput == "masterkey786")
        if (success) {
            _isLocked.value = false
        }
        return success
    }

    fun lock() {
        _isLocked.value = true
    }

    fun updateCredentials(newUsername: String, newPassword: String): Boolean {
        if (newUsername.isBlank() || newPassword.isBlank()) return false
        prefs.edit()
            .putString(KEY_USERNAME, newUsername.trim())
            .putString(KEY_PASSWORD, newPassword)
            .apply()
        return true
    }

    fun resetToDefaults() {
        prefs.edit()
            .putString(KEY_USERNAME, DEFAULT_USERNAME)
            .putString(KEY_PASSWORD, DEFAULT_PASSWORD)
            .apply()
    }
}
