# RTMP Canlı Yayın Uygulaması

Bu uygulama, Android cihazlardan RTMP protokolü kullanarak canlı yayın yapmanızı ve izlemenizi sağlar.

## Özellikler

- ✅ Kullanıcı kaydı ve girişi (Ad, Soyad, Telefon Numarası)
- ✅ Canlı yayın başlatma (kamera ve ses ile)
- ✅ Aktif yayınları görüntüleme
- ✅ Yayınları izleme
- ✅ Gerçek zamanlı izleyici sayısı
- ✅ Firebase Realtime Database entegrasyonu
- ✅ RTMP streaming

## Gereksinimler

### Yazılım
- Android Studio (son sürüm)
- JDK 11 veya üzeri
- Android SDK (API 29+)

### Servisler
- Firebase projesi
- RTMP sunucusu (nginx-rtmp veya benzer)

## Kurulum Adımları

### 1. Firebase Yapılandırması

1. [Firebase Console](https://console.firebase.google.com/) üzerinden yeni bir proje oluşturun
2. Android uygulaması ekleyin (package name: `com.example.rtmp`)
3. `google-services.json` dosyasını indirin
4. İndirdiğiniz dosyayı `app/` klasörüne yerleştirin (mevcut olanın üzerine yazın)
5. Firebase Console'da:
   - **Authentication** → Email/Password'u etkinleştirin
   - **Realtime Database** oluşturun
   - Database Rules'u şu şekilde ayarlayın:

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
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "viewers": {
      ".read": "auth != null",
      ".write": "auth != null"
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

### 2. RTMP Sunucusu Kurulumu

#### Seçenek A: Nginx-RTMP (Ubuntu/Linux)

```bash
# Nginx ve RTMP modülünü yükle
sudo apt update
sudo apt install nginx libnginx-mod-rtmp

# Nginx yapılandırmasını düzenle
sudo nano /etc/nginx/nginx.conf
```

Dosyanın sonuna ekleyin:

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;

        application live {
            live on;
            record off;
            
            # HLS için (opsiyonel)
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3;
            hls_playlist_length 60;
        }
    }
}
```

```bash
# Nginx'i yeniden başlat
sudo systemctl restart nginx
```

#### Seçenek B: Docker ile RTMP Sunucusu

```bash
docker run -d -p 1935:1935 --name rtmp-server tiangolo/nginx-rtmp
```

### 3. Android Uygulamasını Çalıştırma

1. Projeyi Android Studio'da açın
2. Gradle sync yapın (otomatik olarak yapılmalı)
3. `local.properties` dosyasında SDK path'in doğru olduğundan emin olun
4. Uygulamayı bir Android cihaza yükleyin (emulator önerilmez, kamera gerekli)

### 4. Uygulama Kullanımı

#### Kayıt Olma
1. Uygulamayı açın
2. "Kayıt Ol" seçeneğine tıklayın
3. Ad, Soyad, Telefon Numarası ve Şifre girin
4. "Kayıt Ol" butonuna tıklayın

#### Yayın Başlatma
1. Ana ekranda sağ alttaki kamera butonuna tıklayın
2. Yayın başlığı girin
3. RTMP sunucu URL'sini girin (örn: `rtmp://192.168.1.100:1935/live`)
4. Stream key girin (örn: `stream`)
5. "Yayını Başlat" butonuna tıklayın

#### Yayın İzleme
1. Ana ekranda aktif yayınlar listelenir
2. İzlemek istediğiniz yayına tıklayın
3. Yayın başlar (RTMP player gereklidir)

## Dosya Yapısı

```
app/
├── src/main/
│   ├── java/com/example/rtmp/
│   │   ├── models/
│   │   │   ├── User.kt              # Kullanıcı modeli
│   │   │   ├── LiveStream.kt        # Yayın modeli
│   │   │   └── Viewer.kt            # İzleyici modeli
│   │   ├── utils/
│   │   │   └── FirebaseManager.kt   # Firebase işlemleri
│   │   ├── adapters/
│   │   │   └── LiveStreamAdapter.kt # RecyclerView adapter
│   │   ├── LoginActivity.kt         # Giriş ekranı
│   │   ├── RegisterActivity.kt      # Kayıt ekranı
│   │   ├── MainActivity.kt          # Ana ekran
│   │   ├── BroadcastActivity.kt     # Yayın ekranı
│   │   └── ViewerActivity.kt        # İzleme ekranı
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_login.xml
│   │   │   ├── activity_register.xml
│   │   │   ├── activity_main.xml
│   │   │   ├── activity_broadcast.xml
│   │   │   ├── activity_viewer.xml
│   │   │   └── item_live_stream.xml
│   │   └── menu/
│   │       └── menu_main.xml
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Kullanılan Kütüphaneler

- **Firebase**: Authentication ve Realtime Database
- **RTMP Library**: `com.github.pedroSG94.rtmp-rtsp-stream-client-java:rtplibrary:2.2.7`
- **Material Design**: Modern UI bileşenleri
- **Kotlin Coroutines**: Asenkron işlemler

## Önemli Notlar

⚠️ **Dikkat Edilmesi Gerekenler:**

1. **İzinler**: Uygulama ilk açılışta kamera ve mikrofon izni isteyecektir
2. **Sunucu IP**: RTMP sunucu IP adresini doğru girin (yerel ağ için 192.168.x.x)
3. **Güvenlik**: `google-services.json` dosyası gerçek Firebase bilgilerinizle değiştirilmelidir
4. **Network**: Cihazlar aynı ağda olmalı veya sunucu public IP'ye sahip olmalı
5. **Test**: İlk testler için aynı WiFi ağında iki cihaz kullanın

## RTMP URL Formatı

```
rtmp://[SUNUCU_IP]:[PORT]/[UYGULAMA]/[STREAM_KEY]

Örnek:
rtmp://192.168.1.100:1935/live/stream
```

- **SUNUCU_IP**: RTMP sunucusunun IP adresi
- **PORT**: Genellikle 1935 (varsayılan RTMP portu)
- **UYGULAMA**: nginx.conf'ta tanımlanan uygulama adı (genellikle "live")
- **STREAM_KEY**: Benzersiz yayın anahtarı

## Sorun Giderme

### Yayın Başlamıyor
- RTMP sunucusunun çalıştığından emin olun
- IP adresi ve port'u kontrol edin
- Firewall ayarlarını kontrol edin

### Bağlantı Hatası
- Cihazın internet bağlantısını kontrol edin
- Firebase yapılandırmasını kontrol edin
- `google-services.json` dosyasının doğru olduğundan emin olun

### Kamera Açılmıyor
- Uygulama izinlerini kontrol edin
- Başka bir uygulamanın kamerayı kullanmadığından emin olun

## Geliştirme Önerileri

- [ ] ExoPlayer ile RTMP player entegrasyonu
- [ ] Yayın içi chat sistemi
- [ ] Push notification ile yayın bildirimleri
- [ ] Yayın kaydetme özelliği
- [ ] Profil resimleri
- [ ] Yayın istatistikleri

## Lisans

Bu proje eğitim ve geliştirme amaçlıdır.

## İletişim

Sorularınız için issue açabilirsiniz.
