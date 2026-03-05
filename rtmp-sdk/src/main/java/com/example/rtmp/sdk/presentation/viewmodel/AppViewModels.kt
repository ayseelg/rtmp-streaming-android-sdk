package com.example.rtmp.sdk.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rtmp.sdk.domain.usecase.*
import com.example.rtmp.sdk.models.*
import com.example.rtmp.sdk.presentation.state.*
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUserUseCase: LoginUserUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    private val _validationState = MutableLiveData<ValidationState>(ValidationState())
    val validationState: LiveData<ValidationState> = _validationState

    fun login(phoneNumber: String, password: String) {
        val validation = ValidationState(
            phoneNumberError = if (phoneNumber.isBlank()) "Telefon numarası gerekli" else null,
            passwordError = when {
                password.isBlank() -> "Şifre gerekli"
                password.length < 6 -> "Şifre en az 6 karakter olmalı"
                else -> null
            }
        )
        _validationState.value = validation
        if (validation.hasErrors()) return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = loginUserUseCase(phoneNumber, password)
            _authState.value = if (result.isSuccess) AuthState.Success(result.getOrThrow())
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Giriş başarısız")
        }
    }

    private val _resetState = MutableLiveData<ResetPasswordState>()
    val resetState: LiveData<ResetPasswordState> = _resetState

    fun resetPassword(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _resetState.value = ResetPasswordState.Error("Telefon numarası gerekli")
            return
        }
        _resetState.value = ResetPasswordState.Loading
        viewModelScope.launch {
            val result = resetPasswordUseCase(phoneNumber)
            _resetState.value = if (result.isSuccess) ResetPasswordState.Success
            else ResetPasswordState.Error(result.exceptionOrNull()?.message ?: "Hata oluştu")
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        _validationState.value = ValidationState()
    }
}

class RegisterViewModel(
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    private val _validationState = MutableLiveData<ValidationState>(ValidationState())
    val validationState: LiveData<ValidationState> = _validationState

    fun register(firstName: String, lastName: String, phoneNumber: String, password: String, confirmPassword: String) {
        val validation = ValidationState(
            firstNameError = if (firstName.isBlank()) "Ad gerekli" else null,
            lastNameError = if (lastName.isBlank()) "Soyad gerekli" else null,
            phoneNumberError = if (phoneNumber.isBlank()) "Telefon numarası gerekli" else null,
            passwordError = when {
                password.isBlank() -> "Şifre gerekli"
                password.length < 6 -> "Şifre en az 6 karakter olmalı"
                else -> null
            },
            confirmPasswordError = if (confirmPassword != password) "Şifreler eşleşmiyor" else null
        )
        _validationState.value = validation
        if (validation.hasErrors()) return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = registerUserUseCase(phoneNumber, password, firstName, lastName)
            _authState.value = if (result.isSuccess) AuthState.Success(result.getOrThrow())
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Kayıt başarısız")
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        _validationState.value = ValidationState()
    }
}

class MainViewModel(
    private val observeAllStreamsUseCase: ObserveAllStreamsUseCase,
    private val deleteStreamUseCase: DeleteStreamUseCase,
    private val logoutUserUseCase: LogoutUserUseCase
) : ViewModel() {
    private val _streamListState = MutableLiveData<StreamListState>(StreamListState.Idle)
    val streamListState: LiveData<StreamListState> = _streamListState
    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult
    private val _logoutTriggered = MutableLiveData<Boolean>()
    val logoutTriggered: LiveData<Boolean> = _logoutTriggered
    private var streamListener: DatabaseReference? = null

    fun observeStreams() {
        if (streamListener != null) return  // already observing
        _streamListState.value = StreamListState.Loading
        streamListener = observeAllStreamsUseCase { streams ->
            _streamListState.postValue(
                if (streams.isEmpty()) StreamListState.Empty else StreamListState.Success(streams)
            )
        }
    }

    fun deleteStream(stream: LiveStream) {
        viewModelScope.launch {
            _deleteResult.value = deleteStreamUseCase(stream.streamId, stream.userId)
        }
    }

    fun logout() {
        logoutUserUseCase()
        _logoutTriggered.value = true
    }

    fun stopObserving() {
        streamListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopObserving()
    }
}
