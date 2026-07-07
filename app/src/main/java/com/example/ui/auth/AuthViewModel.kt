package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserRepository
import com.example.data.local.AppDatabase
import com.example.data.models.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(db.userDao(), application)

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun setEmail(value: String) {
        _email.value = value
        _errorMessage.value = null
    }

    fun setPassword(value: String) {
        _password.value = value
        _errorMessage.value = null
    }

    fun setDisplayName(value: String) {
        _displayName.value = value
        _errorMessage.value = null
    }

    fun toggleMode() {
        _isRegisterMode.value = !_isRegisterMode.value
        _errorMessage.value = null
        _email.value = ""
        _password.value = ""
        _displayName.value = ""
    }

    fun login() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "Please fill in all fields."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = userRepository.login(_email.value, _password.value)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "An error occurred during login."
            }
        }
    }

    fun register() {
        if (_email.value.isBlank() || _password.value.isBlank() || _displayName.value.isBlank()) {
            _errorMessage.value = "Please fill in all fields."
            return
        }

        if (_password.value.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters long."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = userRepository.register(_email.value, _displayName.value, _password.value)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "An error occurred during sign-up."
            }
        }
    }

    fun loginWithGoogle(email: String, name: String, avatarUrl: String?) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = userRepository.loginWithGoogle(email, name, avatarUrl)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "An error occurred with Google Sign-In."
            }
        }
    }

    fun logout() {
        userRepository.logout()
        _currentUser.value = null
        _email.value = ""
        _password.value = ""
        _displayName.value = ""
        _errorMessage.value = null
    }
}
