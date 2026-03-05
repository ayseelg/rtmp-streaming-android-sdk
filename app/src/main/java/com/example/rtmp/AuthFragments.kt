package com.example.rtmp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rtmp.databinding.FragmentLoginBinding
import com.example.rtmp.databinding.FragmentRegisterBinding
import com.example.rtmp.sdk.di.DependencyContainer
import com.example.rtmp.sdk.presentation.state.AuthState
import com.example.rtmp.sdk.presentation.state.ResetPasswordState
import com.example.rtmp.sdk.presentation.viewmodel.LoginViewModel
import com.example.rtmp.sdk.presentation.viewmodel.RegisterViewModel
import com.example.rtmp.sdk.presentation.viewmodel.ViewModelFactory

// Kullanıcı giriş ekranı
class LoginFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Login işlemlerini yöneten ViewModel
    private lateinit var viewModel: LoginViewModel

    // Fragment arayüzünü oluşturur
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Fragment hazır olduğunda çalışır
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel oluşturulur
        viewModel = ViewModelProvider(this, ViewModelFactory())[LoginViewModel::class.java]

        // Kullanıcı zaten giriş yapmışsa ana ekrana yönlendir
        if (DependencyContainer.authRepository.isUserLoggedIn()) {
            navigateToMain()
            return
        }

        setupListeners()
        observeViewModel()
    }

    // Buton tıklama işlemleri
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etPhoneNumber.text.toString().trim(),
                binding.etPassword.text.toString()
            )
        }

        // Kayıt ekranına geç
        binding.tvRegister.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, RegisterFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // Şifre sıfırlama
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    // ViewModel durumlarını gözlemler
    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> showLoading(false)
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Giriş başarılı", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is AuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Form hataları
        viewModel.validationState.observe(viewLifecycleOwner) { v ->
            binding.tilPhoneNumber.error = v.phoneNumberError
            binding.tilPassword.error = v.passwordError
        }

        // Şifre sıfırlama durumu
        viewModel.resetState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResetPasswordState.Loading -> Toast.makeText(requireContext(), "Gönderiliyor...", Toast.LENGTH_SHORT).show()
                is ResetPasswordState.Success -> AlertDialog.Builder(requireContext())
                    .setTitle("E-posta Gönderildi")
                    .setMessage("Şifre sıfırlama bağlantısı telefon numaranıza bağlı e-posta adresine gönderildi.")
                    .setPositiveButton("Tamam", null)
                    .show()
                is ResetPasswordState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Loading durumunda UI kontrolü
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.etPhoneNumber.isEnabled = !show
        binding.etPassword.isEnabled = !show
    }

    // Ana ekrana geçiş
    private fun navigateToMain() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, MainFragment.newInstance())
            .commit()
    }

    private fun showForgotPasswordDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Telefon Numaranız"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Şifremi Unuttum")
            .setMessage("Telefon numaranızı girin, şifre sıfırlama bağlantısı gönderelim.")
            .setView(input)
            .setPositiveButton("Gönder") { _, _ ->
                viewModel.resetPassword(input.text.toString().trim())
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Memory leak önlemek için binding temizlenir
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): LoginFragment = LoginFragment()
    }
}


// Kullanıcı kayıt ekranı
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Register işlemlerini yöneten ViewModel
    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelFactory())[RegisterViewModel::class.java]

        setupListeners()
        observeViewModel()
    }

    // Buton işlemleri
    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.etFirstName.text.toString().trim(),
                binding.etLastName.text.toString().trim(),
                binding.etPhoneNumber.text.toString().trim(),
                binding.etPassword.text.toString(),
                binding.etConfirmPassword.text.toString()
            )
        }

        // Giriş ekranına dön
        binding.tvLogin.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ViewModel durumlarını dinler
    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> showLoading(false)
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is AuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Form hataları
        viewModel.validationState.observe(viewLifecycleOwner) { v ->
            binding.tilFirstName.error = v.firstNameError
            binding.tilLastName.error = v.lastNameError
            binding.tilPhoneNumber.error = v.phoneNumberError
            binding.tilPassword.error = v.passwordError
            binding.tilConfirmPassword.error = v.confirmPasswordError
        }
    }

    // Loading UI kontrolü
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE

        listOf(
            binding.btnRegister,
            binding.etFirstName,
            binding.etLastName,
            binding.etPhoneNumber,
            binding.etPassword,
            binding.etConfirmPassword
        ).forEach { it.isEnabled = !show }
    }

    // Ana ekrana geçiş
    private fun navigateToMain() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, MainFragment.newInstance())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): RegisterFragment = RegisterFragment()
    }
}