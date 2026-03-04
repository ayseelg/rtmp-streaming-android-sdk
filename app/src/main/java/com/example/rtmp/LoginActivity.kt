package com.example.rtmp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rtmp.databinding.ActivityLoginBinding
import com.example.rtmp.sdk.utils.FirebaseManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Eğer kullanıcı zaten giriş yapmışsa ana ekrana yönlendir
        if (FirebaseManager.isUserLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            if (validateInput(phoneNumber, password)) {
                performLogin(phoneNumber, password)
            }
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun validateInput(phoneNumber: String, password: String): Boolean {
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
        
        return true
    }
    
    private fun performLogin(phoneNumber: String, password: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = FirebaseManager.loginUser(phoneNumber, password)
            
            showLoading(false)
            
            if (result.isSuccess) {
                Toast.makeText(this@LoginActivity, "Giriş başarılı", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Giriş başarısız"
                Toast.makeText(this@LoginActivity, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.etPhoneNumber.isEnabled = !show
        binding.etPassword.isEnabled = !show
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
