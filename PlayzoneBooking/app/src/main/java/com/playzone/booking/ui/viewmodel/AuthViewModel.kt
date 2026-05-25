package com.playzone.booking.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playzone.booking.data.model.UserProfile
import com.playzone.booking.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(repository.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Role diambil langsung dari Firestore field "role"
    val isAdmin: Boolean get() = _userProfile.value?.role == "admin"

    val currentUserId: String get() = repository.currentUser?.uid ?: ""
    val currentUserEmail: String get() = repository.currentUser?.email ?: ""

    init {
        if (repository.currentUser != null) loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val profile = repository.getUserProfile(currentUserId)
            _userProfile.value = profile
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.signIn(email.trim(), password)
                .onSuccess { uid ->
                    repository.updateFcmToken(uid)
                    loadUserProfile()
                    _uiState.value = AuthUiState(successMessage = "Login berhasil")
                    _isLoggedIn.value = true
                }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Login gagal") }
        }
    }

    fun register(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.register(email.trim(), password, name.trim(), phone.trim())
                .onSuccess { uid ->
                    repository.updateFcmToken(uid)
                    loadUserProfile()
                    _uiState.value = AuthUiState(successMessage = "Registrasi berhasil")
                    _isLoggedIn.value = true
                }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Registrasi gagal") }
        }
    }

    fun logout() {
        repository.signOut()
        _isLoggedIn.value = false
        _userProfile.value = null
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}