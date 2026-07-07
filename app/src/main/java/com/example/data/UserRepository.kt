package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.UserDao
import com.example.data.models.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class UserRepository(
    private val userDao: UserDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nexus_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(email: String, displayName: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            if (userDao.getUserByEmail(normalizedEmail) != null) {
                return@withContext Result.failure(Exception("An account with this email already exists."))
            }

            val user = UserEntity(
                email = normalizedEmail,
                displayName = displayName.trim(),
                passwordHash = hashPassword(password),
                authProvider = "email"
            )
            userDao.insertUser(user)
            saveUserSession(normalizedEmail)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(normalizedEmail)
                ?: return@withContext Result.failure(Exception("No account found with this email."))

            if (user.authProvider == "google") {
                return@withContext Result.failure(Exception("This email is registered with Google. Please log in with Google."))
            }

            val inputHash = hashPassword(password)
            if (user.passwordHash == inputHash) {
                saveUserSession(normalizedEmail)
                Result.success(user)
            } else {
                Result.failure(Exception("Incorrect password. Please try again."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(email: String, displayName: String, avatarUrl: String?): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            var user = userDao.getUserByEmail(normalizedEmail)

            if (user == null) {
                // First-time Google user, register them
                user = UserEntity(
                    email = normalizedEmail,
                    displayName = displayName.trim(),
                    passwordHash = null,
                    authProvider = "google",
                    avatarUrl = avatarUrl
                )
                userDao.insertUser(user)
            } else if (user.authProvider == "email") {
                // Link Google sign in if user already has email credentials? 
                // Or standard flow: update provider or error? Let's smoothly link/migrate them to Google login 
                // or keep provider as is, and log them in safely. Let's update or log them in as Google.
                user = user.copy(authProvider = "google", passwordHash = null, avatarUrl = avatarUrl)
                userDao.insertUser(user)
            }

            saveUserSession(normalizedEmail)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        val email = prefs.getString(KEY_LOGGED_IN_EMAIL, null) ?: return@withContext null
        userDao.getUserByEmail(email)
    }

    fun isLoggedIn(): Boolean {
        return prefs.contains(KEY_LOGGED_IN_EMAIL)
    }

    fun logout() {
        prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply()
    }

    private fun saveUserSession(email: String) {
        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, email).apply()
    }
}
