# 🚀 HIZLI TEST - RTMP Canlı Yayın Uygulaması

## 📱 Uygulama Kullanımı

### ADIM 1: Giriş Yap
1. Uygulamayı aç
2. **Kayıt Ol** butonuna bas
3. Ad, Soyad, Telefon, Şifre gir
4. Kayıt olduktan sonra otomatik giriş yapılacak

### ADIM 2: Ana Ekran
- Canlı yayınların listesi görünür (başta boş olacak)
- **SAĞ ALTTA KIRMIZI "📹 Yayın Başlat" BUTONUNA BAS** ← ÖNEMLİ!

### ADIM 3: Yayın Ekranı (RTMP Bilgileri Buraya Girilecek)
Ekranda 3 alan göreceksiniz:

#### 📝 1) Yayın Başlığı
- İstediğiniz herhangi bir başlık
- Örnek: `Benim İlk Yayınım`

#### 🌐 2) RTMP Sunucu URL
**Seçenek A - YouTube Live:**
```
rtmp://a.rtmp.youtube.com/live2
```

**Seçenek B - Yerel Sunucu (Docker):**
```
rtmp://192.168.1.X:1935/live
```
(X yerine bilgisayarınızın IP adresini yazın)

#### 🔑 3) Stream Key
**YouTube kullanıyorsanız:**
- YouTube Studio → Go Live → Stream Key'i kopyalayın

**Yerel sunucu kullanıyorsanız:**
- `test` yazın (veya istediğiniz herhangi bir kelime)

### ADIM 4: Yayını Başlat
1. "**Yayını Başlat**" butonuna bas
2. Kamera izni ver
3. Mikrofon izni ver
4. Yayın başlayacak!

---

## 💻 Yerel RTMP Sunucusu Kurulumu (Test İçin)

### Yöntem 1: Docker (Önerilen - En Kolay)

#### Windows PowerShell'de çalıştırın:

```powershell
# 1. Docker Desktop'ın yüklü olduğundan emin olun
# İndirmek için: https://www.docker.com/products/docker-desktop/

# 2. RTMP sunucusunu başlatın
docker run -d -p 1935:1935 --name rtmp-test tiangolo/nginx-rtmp

# 3. Bilgisayarınızın IP adresini öğrenin
ipconfig | Select-String "IPv4"
# Çıktıda göreceğiniz IP'yi not alın (örn: 192.168.1.105)

# 4. Sunucunun çalıştığını kontrol edin
docker ps
```

#### Uygulamada girilecek bilgiler:
- **RTMP URL**: `rtmp://192.168.1.105:1935/live` (IP'nizi yazın)
- **Stream Key**: `test`

#### Yayını izlemek için (VLC Media Player):
1. VLC'yi açın
2. Media → Open Network Stream
3. URL girin: `rtmp://localhost:1935/live/test`
4. Play'e basın

### Yöntem 2: YouTube Live (En Pratik)

#### Adımlar:
1. https://studio.youtube.com adresine gidin
2. Sağ üst → **Create** → **Go Live**
3. **Stream** sekmesini seçin
4. **Stream Settings** kısmından:
   - Stream URL: `rtmp://a.rtmp.youtube.com/live2`
   - Stream Key: (Sayfada gösterilecek, kopyalayın)

#### Uygulamada girilecek bilgiler:
- **RTMP URL**: `rtmp://a.rtmp.youtube.com/live2`
- **Stream Key**: YouTube'dan kopyaladığınız key

---

## ❓ Sorun Giderme

### "Bağlantı Hatası" Alıyorum
✅ **Kontrol Listesi:**
1. RTMP URL `rtmp://` ile başlıyor mu?
2. Yerel sunucu kullanıyorsanız, Docker container çalışıyor mu?
   ```powershell
   docker ps
   ```
3. IP adresi doğru mu?
4. Android cihaz aynı WiFi ağında mı?
5. Firewall 1935 portunu engelliyor mu?

### Kamera Açılmıyor
1. Ayarlar → Uygulamalar → RTMP → İzinler
2. Kamera ve Mikrofon izinlerini aç

### "RTMP Kimlik Doğrulama Hatası"
- Stream Key yanlış girilmiş olabilir
- YouTube/Facebook'ta stream key'i kontrol edin

### Emülatörde Test Ediyorum
- Yerel sunucu IP'si olarak `10.0.2.2` kullanın
- Örnek: `rtmp://10.0.2.2:1935/live`

---

## 🎯 Hızlı Test Senaryosu (5 Dakika)

### 1. Docker Sunucusunu Başlat
```powershell
docker run -d -p 1935:1935 --name rtmp-quick tiangolo/nginx-rtmp
ipconfig | Select-String "IPv4"
# IP'yi not al (örn: 192.168.1.105)
```

### 2. Uygulamayı Aç
- Kayıt ol: `05551234567` / `123456`
- Giriş yap

### 3. Yayın Başlat
- **📹 Yayın Başlat** butonuna bas
- Başlık: `Test`
- RTMP URL: `rtmp://192.168.1.105:1935/live`
- Stream Key: `test`
- **Yayını Başlat** butonuna bas

### 4. İzle (Başka Cihazdan)
- VLC ile: `rtmp://192.168.1.105:1935/live/test`
- Veya uygulamada başka hesapla giriş yap, yayını listeden seç

---

## 📊 Logları İzleme (Hata Ayıklama)

Android Studio'da:
1. **Logcat** sekmesini aç
2. Filtre ekle: `BroadcastActivity`
3. Yayın başlatırken logları izle

Göreceğiniz loglar:
- `Connecting to: rtmp://...` - Bağlantı başladı
- `Connection successful` - Başarılı! ✅
- `Connection failed: ...` - Hata nedeni gösterilecek ❌

---

## 📞 İletişim Bilgileri Test

Firebase Realtime Database'de yayın bilgileri otomatik kaydediliyor:
- Yayın başlığı
- Broadcaster bilgileri
- İzleyici sayısı
- Başlangıç zamanı

Başka bir cihazdan aynı Firebase'e bağlanırsa, canlı yayınları görebilir ve izleyebilir.
