# RTMP Streaming App - Clean Architecture

## 📐 Yeni Mimari Yapısı

Bu proje **Clean Architecture** ve **MVVM** pattern'leri kullanılarak yeniden yapılandırılmıştır.

### 🎯 Mimari Katmanları

```
┌─────────────────────────────────────┐
│     Presentation Layer (UI)         │
│  - Activities                       │
│  - ViewModels                       │
│  - UI States                        │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│      Domain Layer (Business)        │
│  - Use Cases                        │
│  - Repository Interfaces            │
│  - Domain Models                    │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│       Data Layer (Storage)          │
│  - Repository Implementations       │
│  - Data Sources (Firebase)          │
│  - Data Models                      │
└─────────────────────────────────────┘
```

### 📦 Paket Yapısı

```
com.example.rtmp.sdk/
├── data/
│   ├── repository/          # Repository implementasyonları
│   │   ├── AuthRepositoryImpl
│   │   ├── StreamRepositoryImpl
│   │   └── ViewerRepositoryImpl
│   └── source/              # Data source sınıfları
│       └── FirebaseDataSource
│
├── domain/
│   ├── repository/          # Repository interface'leri
│   │   ├── AuthRepository
│   │   ├── StreamRepository
│   │   └── ViewerRepository
│   └── usecase/             # Use case sınıfları
│       ├── RegisterUserUseCase
│       ├── LoginUserUseCase
│       ├── CreateLiveStreamUseCase
│       ├── JoinStreamUseCase
│       └── ...
│
├── presentation/
│   ├── viewmodel/           # ViewModel'ler
│   │   ├── LoginViewModel
│   │   ├── RegisterViewModel
│   │   ├── MainViewModel
│   │   ├── BroadcastViewModel
│   │   └── ViewerViewModel
│   └── state/               # UI state modelleri
│       ├── AuthState
│       ├── StreamListState
│       ├── BroadcastState
│       └── ViewerState
│
├── di/                      # Dependency Injection
│   └── DependencyContainer
│
├── models/                  # Data modelleri
│   ├── User
│   ├── LiveStream
│   └── Viewer
│
└── ui/                      # Activity'ler
    ├── BroadcastActivity
    └── ViewerActivity
```

## 🔧 Kullanım Örnekleri

### 1. ViewModel Kullanımı

**Eski Yöntem (❌ Deprecated):**
```kotlin
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Direkt FirebaseManager çağrısı
        lifecycleScope.launch {
            val result = FirebaseManager.loginUser(phone, password)
        }
    }
}
```

**Yeni Yöntem (✅ Önerilen):**
```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: LoginViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewModel oluştur
        viewModel = ViewModelProvider(this, ViewModelFactory())
            [LoginViewModel::class.java]
        
        // State'i gözlemle
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> navigateToMain()
                is AuthState.Error -> showError(state.message)
            }
        }
        
        // Login işlemini başlat
        binding.btnLogin.setOnClickListener {
            viewModel.login(phoneNumber, password)
        }
    }
}
```

### 2. Dependency Injection

```kotlin
// Tüm bağımlılıklar DependencyContainer'da yönetilir
object DependencyContainer {
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseDataSource)
    }
    
    val loginUserUseCase: LoginUserUseCase by lazy {
        LoginUserUseCase(authRepository)
    }
}
```

### 3. Use Case Kullanımı

```kotlin
class LoginViewModel(
    private val loginUserUseCase: LoginUserUseCase
) : ViewModel() {
    
    fun login(phoneNumber: String, password: String) {
        viewModelScope.launch {
            // Use case üzerinden işlemi gerçekleştir
            val result = loginUserUseCase(phoneNumber, password)
            
            _authState.value = if (result.isSuccess) {
                AuthState.Success(result.getOrThrow())
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Hata")
            }
        }
    }
}
```

### 4. UI State Yönetimi

```kotlin
// Sealed class ile tip-güvenli state yönetimi
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Activity'de kullanım
viewModel.authState.observe(this) { state ->
    when (state) {
        is AuthState.Idle -> { /* Hiçbir şey yapma */ }
        is AuthState.Loading -> showLoading(true)
        is AuthState.Success -> onLoginSuccess(state.user)
        is AuthState.Error -> showError(state.message)
    }
}
```

## 🎨 SOLID Prensipleri

### Single Responsibility Principle (SRP)
- Her sınıf tek bir sorumluluğa sahip
- Activity'ler sadece UI binding yapar
- ViewModel'ler UI state'ini yönetir
- Use case'ler tek bir iş mantığını içerir
- Repository'ler veri operasyonlarını yönetir

### Open/Closed Principle (OCP)
- Interface'ler sayesinde genişlemeye açık
- Repository interface'leri farklı implementasyonlar alabilir
- Yeni data source eklemek için mevcut kodu değiştirmeye gerek yok

### Liskov Substitution Principle (LSP)
- Repository implementasyonları interface'lerini tam olarak karşılar
- Mock implementasyonlar test için kullanılabilir

### Interface Segregation Principle (ISP)
- Her repository belirli bir domain'e odaklanır
- AuthRepository, StreamRepository, ViewerRepository ayrı ayrı

### Dependency Inversion Principle (DIP)
- ViewModel'ler somut sınıflara değil, use case'lere bağımlı
- Use case'ler somut repository'lere değil, interface'lere bağımlı

## 🧪 Test Edilebilirlik

Yeni mimari sayesinde kolayca test yazılabilir:

```kotlin
class LoginViewModelTest {
    @Test
    fun `login success should update state`() = runTest {
        // Mock use case
        val mockUseCase = mockk<LoginUserUseCase>()
        coEvery { mockUseCase(any(), any()) } returns Result.success(mockUser)
        
        // ViewModel oluştur
        val viewModel = LoginViewModel(mockUseCase)
        
        // Test
        viewModel.login("123456", "password")
        
        // Assertion
        assertTrue(viewModel.authState.value is AuthState.Success)
    }
}
```

## 📊 Avantajlar

### ✅ Temiz Kod
- Okunabilirlik arttı
- Bakım kolaylığı
- Kod tekrarı azaldı

### ✅ Test Edilebilir
- Her katman bağımsı z test edilebilir
- Mock/fake implementasyonlar kolayca oluşturulabilir

### ✅ Ölçeklenebilir
- Yeni özellikler kolayca eklenebilir
- Mevcut kodu bozmadan genişletilebilir

### ✅ Bağımlılık Yönetimi
- DependencyContainer ile merkezi yönetim
- Interface'ler ile gevşek bağlılık

### ✅ UI State Yönetimi
- Sealed class'lar ile tip-güvenli state
- Reactive programlama (LiveData/Flow)

## 🔄 Migration Kılavuzu

Eski kodunuzu yeni mimariye geçirmek için:

1. **FirebaseManager yerine Use Case kullanın**
   - `FirebaseManager.loginUser()` → `loginUserUseCase()`

2. **Activity'lerde ViewModel kullanın**
   - İş mantığını Activity'den ViewModel'e taşıyın
   - UI state'ini observe edin

3. **Sealed class'lar ile state yönetin**
   - Boolean flag'ler yerine sealed class kullanın
   - Tüm state'ler tip-güvenli olsun

## 📝 Notlar

- `FirebaseManager` hala çalışıyor ama **deprecated** olarak işaretlendi
- Yeni özellikler sadece yeni mimari ile eklenmelidir
- Eski kod kademeli olarak refactor edilecek

## 🚀 Gelecek Geliştirmeler

- [ ] Hilt/Koin ile gerçek DI
- [ ] Flow kullanımı (LiveData yerine)
- [ ] Repository katmanında caching
- [ ] Offline-first yaklaşımı
- [ ] Unit ve UI testleri

---

**Not:** Sistem işlevselliği değişmedi, sadece mimari temizlendi ve profesyonel hale getirildi.
