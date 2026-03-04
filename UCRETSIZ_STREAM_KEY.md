# 🚀 5 DAKİKADA ÜCRETSIZ RTMP TEST

## Yöntem 1: Docker ile Yerel Sunucu (Önerilen - Anında Çalışır)

### Adım 1: Docker Kur
https://www.docker.com/products/docker-desktop/
- İndir ve kur
- Bilgisayarı yeniden başlat

### Adım 2: PowerShell'de Çalıştır
```powershell
# RTMP sunucusunu başlat
docker run -d -p 1935:1935 --name rtmp-server tiangolo/nginx-rtmp

# Bilgisayarının IP'sini öğren
ipconfig | Select-String "IPv4"
```

Çıktı örnek:
```
IPv4 Address. . . . . . . . . . . : 192.168.1.105
```

### Adım 3: Android Uygulamasında Yaz
- **RTMP URL**: `rtmp://192.168.1.105:1935/live`
- **Stream Key**: `test`
- **Yayını Başlat** bas

### Adım 4: İzle (Opsiyonel)
VLC Media Player ile:
- Media → Open Network Stream
- URL: `rtmp://localhost:1935/live/test`

✅ **AVANTAJLAR:**
- Tamamen ücretsiz
- Anında çalışır (bekleme yok)
- İnternet gerektirmez (aynı WiFi yeterli)
- Unlimited yayın süresi

---

## Yöntem 2: YouTube (24 Saat Bekleme Gerekir)

### Adım 1: Hesap Doğrula
1. https://youtube.com/verify
2. Telefon numaranı gir
3. SMS kodu yaz
4. ⏳ **24 saat bekle**

### Adım 2: Stream Key Al (24 saat sonra)
1. https://studio.youtube.com
2. **Create** → **Go Live**
3. **Stream** tab
4. Stream Key'i kopyala

### Adım 3: Uygulamada Kullan
- **RTMP URL**: `rtmp://a.rtmp.youtube.com/live2`
- **Stream Key**: YouTube'dan kopyaladığın

✅ **AVANTAJLAR:**
- Ücretsiz
- Herkes YouTube'dan izleyebilir
- Otomatik kayıt (Video olarak kaydedilir)

❌ **DEZAVANTAJLAR:**
- İlk kez için 24 saat bekleme
- İnternet bağlantısı şart
- Uygulamadan izlenemez (sadece YouTube'dan)

---

## Yöntem 3: Facebook Live

### Adım 1: Facebook'ta Live Başlat
1. Facebook profil → Create → Live Video
2. **Use streaming software** seç

### Adım 2: Bilgileri Kopyala
- Server URL (örn: `rtmps://live-api.facebook.com:443/rtmp/`)
- Stream Key (örn: `12345678?s_sw=0...`)

### Adım 3: Uygulamada Kullan
- **RTMP URL**: Server URL'i yapıştır
- **Stream Key**: Stream Key'i yapıştır

✅ **AVANTAJLAR:**
- Anında çalışır (bekleme yok)
- Ücretsiz
- Arkadaşların Facebook'tan izler

---

## 🎯 HANGİSİNİ SEÇMELİ?

### Test/Geliştirme İçin:
👉 **Docker ile Yerel Sunucu**
- En hızlı
- Bekleme yok
- İnternet gerektirmez

### Gerçek Yayın İçin:
👉 **YouTube Live**
- En profesyonel
- Geniş izleyici
- Otomatik kayıt

### Hızlı Sosyal Paylaşım:
👉 **Facebook Live**
- Anında başla
- Arkadaşlarınla paylaş

---

## 💡 ÖNERİM:

1. **ŞİMDİ:** Docker ile yerel test yap (5 dakika)
2. **BUGÜN:** YouTube hesabını doğrula (24 saat bekleyecek)
3. **YARIN:** YouTube ile gerçek yayın yap

### Docker Kurulum Komutları (Kopyala-Yapıştır):

```powershell
# 1. Sunucuyu başlat
docker run -d -p 1935:1935 --name my-rtmp tiangolo/nginx-rtmp

# 2. Çalıştığını kontrol et
docker ps

# 3. IP adresini öğren
ipconfig | Select-String "IPv4"

# 4. Test et (VLC ile)
# VLC aç → Media → Open Network Stream
# rtmp://localhost:1935/live/test

# Sunucuyu durdurmak için:
docker stop my-rtmp

# Sunucuyu tekrar başlatmak için:
docker start my-rtmp

# Sunucuyu silmek için:
docker rm -f my-rtmp
```

### Uygulamada Yazacakların:
```
Yayın Başlığı: İlk Test Yayınım
RTMP URL: rtmp://192.168.1.XXX:1935/live
Stream Key: test
```

Hepsi bu kadar! 🎉
