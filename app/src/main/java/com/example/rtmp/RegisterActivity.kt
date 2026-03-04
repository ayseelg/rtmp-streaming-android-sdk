package com.example.rtmp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rtmp.databinding.ActivityRegisterBinding
import com.example.rtmp.sdk.utils.FirebaseManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            
            if (validateInput(firstName, lastName, phoneNumber, password, confirmPassword)) {
                performRegister(firstName, lastName, phoneNumber, password)
            }
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun validateInput(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Ad gerekli"
            return false
        }
        binding.tilFirstName.error = null
        
        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Soyad gerekli"
            return false
        }
        binding.tilLastName.error = null
        
        if (phoneNumber.isEmpty()) {
            binding.tilPhoneNumber.error = "Telefon numarası gerekli"
            return false
        }
        binding.tilPhoneNumber.error = null
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Şifre gerekli"
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Şifre en az 6 karakter olmalı"
            return false
        }
        binding.tilPassword.error = null
        
        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Şifreler eşleşmiyor"
            return false
        }
        binding.tilConfirmPassword.error = null
        
        return true
    }
    
    private fun performRegister(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        password: String
    ) {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = FirebaseManager.registerUser(phoneNumber, password, firstName, lastName)
            
            showLoading(false)
            
            if (result.isSuccess) {
                Toast.makeText(this@RegisterActivity, "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Kayıt başarısız"
                Toast.makeText(this@RegisterActivity, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.etFirstName.isEnabled = !show
        binding.etLastName.isEnabled = !show
        binding.etPhoneNumber.isEnabled = !show
        binding.etPassword.isEnabled = !show
        binding.etConfirmPassword.isEnabled = !show
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
