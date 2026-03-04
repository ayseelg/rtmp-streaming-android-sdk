# RTMP SDK - Android Canlı Yayın Kütüphanesi

Instagram Live benzeri canlı yayın ve izleme özelliklerini Android uygulamanıza ekleyin.

## 🎯 Özellikler

- ✅ **RTMP Broadcasting** - Kamera ile canlı yayın yapma
- ✅ **HLS Viewer** - Canlı yayınları izleme (ExoPlayer)
- ✅ **Firebase Integration** - Kullanıcı yönetimi ve canlı yayın listesi
- ✅ **Real-time Updates** - İzleyici sayısı, canlı yayın durumu
- ✅ **Multi-platform Streaming** - YouTube Live, Facebook Live desteği
- ✅ **Material Components** - Modern Android UI

## 📦 Kurulum

### 1. AAR Dosyasını Ekleyin

AAR dosyasını projenizin `app/libs` klasörüne kopyalayın:

```
YeniProje/
  app/
    libs/
      rtmp-sdk-debug.aar
```

### 2. build.gradle.kts (Project level)

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
        classpath("com.google.gms:google-services:4.4.0")
    }
}

plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

### 3. build.gradle.kts (App level)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.yourapp.package"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // RTMP SDK
    implementation(files("libs/rtmp-sdk-debug.aar"))
    
    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Material Components
    implementation("com.google.android.material:material:1.11.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // RTMP Streaming (pedro-library)
    implementation("com.github.pedroSG94.RootEncoder:library:2.2.9")
    
    // ExoPlayer (HLS playback)
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

### 4. settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 5. AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- İzinler -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.MaterialComponents.DayNight">
        
        <!-- Aktiviteler library'den otomatik eklenir -->
        
    </application>
</manifest>
```

### 6. Firebase Ayarları

#### Firebase Console'da:
1. https://console.firebase.google.com adresine gidin
2. Yeni proje oluşturun veya mevcut projeyi seçin
3. Android uygulaması ekleyin (paket adınızla)
4. `google-services.json` dosyasını indirin
5. `app/` klasörüne kopyalayın

#### Firebase Realtime Database Rules:
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "$uid === auth.uid"
      }
    },
    "streams": {
      ".read": true,
      ".indexOn": ["isLive", "startedAt"],
      "$streamId": {
        ".write": "auth != null"
      }
    },
    "viewers": {
      "$streamId": {
        ".read": true,
        "$userId": {
          ".write": "$userId === auth.uid"
        }
      }
    },
    "user_streams": {
      "$uid": {
        ".read": "auth != null",
        ".write": "$uid === auth.uid"
      }
    }
  }
}
```

#### Firebase Authentication:
- Authentication → Sign-in method → Email/Password → Etkinleştir

## 🚀 RTMP Sunucu Kurulumu

SDK HLS streaming için bir RTMP sunucuya ihtiyaç duyar.

### Docker ile (Önerilen):

#### Windows PowerShell:

```powershell
# nginx config dosyası oluştur (nginx-simple.conf)
@"
worker_processes auto;
rtmp_auto_push on;
events {}
rtmp {
    server {
        listen 1935;
        listen [::]:1935 ipv6only=on;

        application stream {
            live on;
            record off;
            
            hls on;
            hls_path /opt/data/hls;
            hls_fragment 2s;
            hls_playlist_length 10s;
        }
    }
}
"@ | Out-File -FilePath nginx-simple.conf -Encoding utf8

# Container başlat
docker run -d --name rtmp-server `
  -p 1935:1935 -p 8080:8080 `
  -v ${PWD}/nginx-simple.conf:/etc/nginx/nginx.conf:ro `
  -v hls-data:/opt/data `
  tiangolo/nginx-rtmp
```

#### Linux/Mac:

```bash
# nginx config dosyası oluştur (nginx-simple.conf)
cat > nginx-simple.conf <<EOF
worker_processes auto;
rtmp_auto_push on;
events {}
rtmp {
    server {
        listen 1935;
        listen [::]:1935 ipv6only=on;

        application stream {
            live on;
            record off;
            
            hls on;
            hls_path /opt/data/hls;
            hls_fragment 2s;
            hls_playlist_length 10s;
        }
    }
}
EOF

# Container başlat
docker run -d --name rtmp-server \
  -p 1935:1935 -p 8080:8080 \
  -v $(pwd)/nginx-simple.conf:/etc/nginx/nginx.conf:ro \
  -v hls-data:/opt/data \
  tiangolo/nginx-rtmp
```

### Sunucu Bilgileri:
- **RTMP Port:** 1935
- **HTTP Port:** 8080
- **RTMP URL:** `rtmp://YOUR_IP:1935/stream`
- **HLS URL:** `http://YOUR_IP:8080/hls/STREAM_KEY.m3u8`

### IP Adresini Bulma:

**Windows:**
```powershell
ipconfig | Select-String "IPv4"
```

**Linux/Mac:**
```bash
ifconfig | grep "inet "
```

## 💻 Kullanım

### 1. Login/Register

```kotlin
import com.example.rtmp.sdk.ui.LoginActivity

// Login ekranını aç
val intent = Intent(this, LoginActivity::class.java)
startActivity(intent)
```

Firebase Auth ile otomatik kullanıcı yönetimi.

### 2. Canlı Yayın Başlatma

```kotlin
import com.example.rtmp.sdk.ui.BroadcastActivity

// Broadcast ekranını aç
val intent = Intent(this, BroadcastActivity::class.java)
startActivity(intent)
```

**BroadcastActivity Özellikleri:**
- Kamera önizlemesi
- RTMP URL ve Stream Key girişi
- Yayını başlat/durdur
- İzleyici sayısı gösterimi
- YouTube Live / Facebook Live entegrasyonu

**Örnek RTMP URL:**
```
rtmp://192.168.1.100:1935/stream
```

**Stream Key:**
```
test
```

### 3. Canlı Yayınları Listeleme ve İzleme

```kotlin
import com.example.rtmp.sdk.ui.MainActivity

// Ana ekranı aç (canlı yayın listesi)
val intent = Intent(this, MainActivity::class.java)
startActivity(intent)
```

**MainActivity Özellikleri:**
- Aktif canlı yayınlar listesi
- Yayına tıklayarak ViewerActivity açılır
- Real-time güncelleme

**ViewerActivity Özellikleri:**
- HLS stream oynatma (ExoPlayer)
- İzleyici sayısı
- Otomatik yeniden deneme mekanizması (HLS hazırlanırken)

## 📱 Örnek Akış

```kotlin
class YourMainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_main)
        
        // Kullanıcı giriş yapmış mı kontrol et
        if (!FirebaseManager.isUserLoggedIn()) {
            // Login ekranına yönlendir
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        // Broadcast butonuna tıklandığında
        btnStartBroadcast.setOnClickListener {
            startActivity(Intent(this, BroadcastActivity::class.java))
        }
        
        // Canlı yayınları göster
        btnWatchLive.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
```

## 🎨 UI Özelleştirme

SDK aktiviteleri otomatik olarak projenizin temasını kullanır.

### Renkleri Değiştirme

`res/values/colors.xml`:
```xml
<color name="primary">#6200EE</color>
<color name="primary_dark">#3700B3</color>
<color name="accent">#03DAC5</color>
```

### Temaları Özelleştirme

`res/values/themes.xml`:
```xml
<style name="AppTheme" parent="Theme.MaterialComponents.DayNight">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorPrimaryDark">@color/primary_dark</item>
    <item name="colorAccent">@color/accent</item>
</style>
```

## 🔧 FirebaseManager API

SDK, Firebase işlemleri için `FirebaseManager` sınıfı sağlar:

```kotlin
import com.example.rtmp.sdk.utils.FirebaseManager

// Kullanıcı kaydı
lifecycleScope.launch {
    val result = FirebaseManager.registerUser(
        phoneNumber = "05551234567",
        password = "password123",
        firstName = "Ahmet",
        lastName = "Yılmaz"
    )
    if (result.isSuccess) {
        val user = result.getOrNull()
        // Başarılı
    }
}

// Kullanıcı girişi
lifecycleScope.launch {
    val result = FirebaseManager.loginUser(
        phoneNumber = "05551234567",
        password = "password123"
    )
    if (result.isSuccess) {
        val user = result.getOrNull()
        // Başarılı
    }
}

// Canlı yayın oluşturma
lifecycleScope.launch {
    val result = FirebaseManager.createLiveStream(
        title = "Günlük Vlog",
        rtmpUrl = "rtmp://192.168.1.100:1935/stream",
        streamKey = "test"
    )
    if (result.isSuccess) {
        val stream = result.getOrNull()
        // stream.streamId kullan
    }
}

// Yayını sonlandırma
lifecycleScope.launch {
    FirebaseManager.endLiveStream(streamId)
}

// Canlı yayınları dinleme
FirebaseManager.observeLiveStreams { streams ->
    // Aktif yayınlar listesi
    streams.forEach { stream ->
        Log.d("TAG", "${stream.title} - ${stream.viewerCount} izleyici")
    }
}

// İzleyici sayısını dinleme
FirebaseManager.observeViewerCount(streamId) { count ->
    Log.d("TAG", "$count izleyici")
}
```

## 🐛 Sorun Giderme

### "HLS Stream Yükleme Hatası"

**Sebep:** RTMP sunucu çalışmıyor veya HLS dosyaları henüz oluşmadı.

**Çözüm:**
1. Docker container'ı kontrol edin: `docker ps`
2. Broadcaster yayını başlattıktan **5-10 saniye bekleyin**
3. HLS dosyalarını kontrol edin: 
```bash
docker exec rtmp-server ls /opt/data/hls/
```

### "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED"

**Sebep:** nginx HTTP portu yanlış yapılandırılmış veya dosyalar bulunamıyor.

**Çözüm:** 
1. nginx config'de `listen 8080;` olduğundan emin olun
2. HLS path'i kontrol edin: `hls_path /opt/data/hls;`
3. Container'ı yeniden başlatın:
```bash
docker restart rtmp-server
```

### "unexpected end of stream"

**Sebep:** nginx HTTP server henüz HLS dosyalarını servis etmiyor.

**Çözüm:** Broadcaster yayını başlattıktan 10 saniye bekleyin, ViewerActivity otomatik olarak yeniden deneyecek.

### Firebase Rules Hatası

**Sebep:** Firebase Realtime Database rules eksik veya yanlış.

**Çözüm:** Yukarıdaki Firebase Rules'u kopyalayın ve Firebase Console'da Realtime Database → Rules kısmına yapıştırın.

### "Camera permission denied"

**Sebep:** Uygulama izinleri verilmemiş.

**Çözüm:**
1. AndroidManifest.xml'de izinlerin olduğundan emin olun
2. Runtime permissions ekleyin:
```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
}
```

### ProGuard Hataları (Release Build)

`proguard-rules.pro`:
```proguard
# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# pedro-library
-keep class com.pedro.** { *; }
-dontwarn com.pedro.**

# RTMP SDK Models
-keep class com.example.rtmp.sdk.models.** { *; }
-keepclassmembers class com.example.rtmp.sdk.models.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

## 📋 Gereksinimler

- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 34
- **Kotlin:** 1.9.20+
- **Android Gradle Plugin:** 8.2.0+
- **Java:** 17
- **Docker:** (RTMP sunucu için)

## 🏗️ SDK Yapısı

```
rtmp-sdk/
├── src/main/
│   ├── java/com/example/rtmp/sdk/
│   │   ├── models/          # Data models (User, LiveStream, Viewer)
│   │   ├── ui/              # Activities (Login, Broadcast, Viewer, Main)
│   │   └── utils/           # FirebaseManager, helpers
│   ├── res/
│   │   ├── layout/          # XML layouts
│   │   ├── drawable/        # Icons, backgrounds
│   │   └── values/          # Strings, colors, styles
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 🔐 Güvenlik Notları

1. **Firebase Rules:** Production'da mutlaka güvenli rules kullanın
2. **RTMP URL:** Hassas bilgileri kodda sabit olarak bırakmayın
3. **API Keys:** `google-services.json` dosyasını git'e eklemeyin
4. **ProGuard:** Release build'de mutlaka ProGuard kullanın

## 📝 Best Practices

1. **Stream Key:** Her yayın için unique stream key kullanın
2. **Error Handling:** Network hatalarını handle edin
3. **Memory Management:** Activity lifecycle'a dikkat edin
4. **UI Thread:** Firebase işlemlerini coroutine'de yapın
5. **Permissions:** Runtime permissions'ı düzgün yönetin

## 🚀 İleri Seviye Özellikler

### Kendi RTMP URL'inizi Kullanma

```kotlin
import com.example.rtmp.sdk.ui.BroadcastActivity

val intent = Intent(this, BroadcastActivity::class.java).apply {
    // RTMP URL'i önceden doldur
    putExtra("rtmpUrl", "rtmp://your-server.com:1935/live")
    putExtra("streamKey", "unique-stream-key-123")
}
startActivity(intent)
```

### YouTube Live Entegrasyonu

BroadcastActivity otomatik olarak YouTube RTMP URL'lerini destekler:
```
rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY
```

### Facebook Live Entegrasyonu

Facebook RTMP URL'leri de desteklenir:
```
rtmps://live-api-s.facebook.com:443/rtmp/YOUR_STREAM_KEY
```

## 📄 Lisans

Bu SDK özel bir projedir. Ticari kullanım için lütfen iletişime geçin.

## 🤝 Destek

Sorularınız için:
- **GitHub Issues:** https://github.com/yourusername/rtmp-sdk/issues
- **Email:** support@example.com
- **Documentation:** https://docs.example.com/rtmp-sdk

## 📝 Changelog

### Version 1.0.0 (2026-03-04)
- ✅ İlk sürüm
- ✅ RTMP Broadcasting (pedro-library)
- ✅ HLS Viewer (ExoPlayer/Media3)
- ✅ Firebase Authentication & Realtime Database
- ✅ Real-time viewer count
- ✅ Material Components UI
- ✅ YouTube/Facebook Live support
- ✅ Docker nginx-rtmp config

---

**Not:** Bu SDK aktif geliştirme aşamasındadır. Öneri ve hata raporlarınızı bekliyoruz! 🚀

## 🎬 Demo Video

Yakında eklenecek...

## 📸 Screenshots

| Login | Broadcast | Live Streams | Viewer |
|-------|-----------|--------------|--------|
| 📱    | 📹        | 📋           | 📺     |

---

Made with ❤️ by RTMP SDK Team
