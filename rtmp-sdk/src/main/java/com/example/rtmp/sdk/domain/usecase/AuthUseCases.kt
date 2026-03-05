package com.example.rtmp.sdk.domain.usecase

import com.example.rtmp.sdk.domain.repository.*
import com.example.rtmp.sdk.models.*

class RegisterUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        phoneNumber: String, password: String, firstName: String, lastName: String
    ): Result<User> {
        if (phoneNumber.isBlank()) return Result.failure(IllegalArgumentException("Telefon numarası gerekli"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Şifre en az 6 karakter olmalı"))
        if (firstName.isBlank()) return Result.failure(IllegalArgumentException("Ad gerekli"))
        if (lastName.isBlank()) return Result.failure(IllegalArgumentException("Soyad gerekli"))
        return authRepository.registerUser(phoneNumber, password, firstName, lastName)
    }
}

class LoginUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(phoneNumber: String, password: String): Result<User> {
        if (phoneNumber.isBlank()) return Result.failure(IllegalArgumentException("Telefon numarası gerekli"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Şifre gerekli"))
        return authRepository.loginUser(phoneNumber, password)
    }
}

class LogoutUserUseCase(private val authRepository: AuthRepository) {
    operator fun invoke() = authRepository.signOut()
}

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(phoneNumber: String): Result<Boolean> {
        if (phoneNumber.isBlank()) return Result.failure(IllegalArgumentException("Telefon numarası gerekli"))
        return authRepository.resetPassword(phoneNumber)
    }
}
