# RTMP Sunucu Kurulumu

## Seçenek 1: Docker ile RTMP Sunucusu (Önerilen)

### Adımlar:
1. Docker Desktop'ı indirin ve kurun: https://www.docker.com/products/docker-desktop/

2. PowerShell'de şu komutu çalıştırın:
```powershell
docker run -d -p 1935:1935 -p 8080:8080 -p 8088:8088 --name rtmp-server tiangolo/nginx-rtmp
```

3. Sunucu başladıktan sonra uygulamada şu bilgileri girin:
   - **RTMP URL**: `rtmp://192.168.1.X:1935/live` (X yerine bilgisayarınızın IP'sini yazın)
   - **Stream Key**: `test` (istediğiniz herhangi bir değer)

4. Bilgisayarınızın IP adresini öğrenmek için:
```powershell
ipconfig | Select-String "IPv4"
```

5. Yayını izlemek için VLC Media Player kullanın:
   - Media → Open Network Stream
   - URL: `rtmp://localhost:1935/live/test`

---

## Seçenek 2: YouTube Live Streaming

### Adımlar:
1. YouTube Studio'ya gidin: https://studio.youtube.com
2. Sağ üst köşeden "Create" → "Go Live" tıklayın
3. "Stream" sekmesini seçin
4. "Stream Settings" kısmından:
   - **Stream URL** ve **Stream Key** değerlerini kopyalayın

5. Android uygulamada:
   - **RTMP URL**: `rtmp://a.rtmp.youtube.com/live2`
   - **Stream Key**: YouTube'dan kopyaladığınız key

6. Yayını YouTube Studio'dan canlı izleyebilirsiniz

---

## Seçenek 3: Facebook Live

### Adımlar:
1. Facebook'ta "Live Video" başlatın
2. "Streaming Software" seçeneğini seçin
3. **Stream Key** ve **Server URL** değerlerini alın

4. Android uygulamada bu değerleri girin

---

## Ağ Ayarları

### Firewall İzinleri:
Yerel sunucu kullanıyorsanız, Windows Firewall'da 1935 portunu açın:

```powershell
New-NetFirewallRule -DisplayName "RTMP Server" -Direction Inbound -Protocol TCP -LocalPort 1935 -Action Allow
```

### Android Cihaz ile Aynı Ağda Olun:
- Bilgisayar ve Android cihaz aynı WiFi ağına bağlı olmalı
- Emülatör kullanıyorsanız, `10.0.2.2` IP adresini kullanın bilgisayarınız için

---

## Sorun Giderme

### "Bağlantı Hatası" alıyorsanız:
1. ✅ RTMP sunucunuzun çalıştığından emin olun
2. ✅ IP adresi ve port numarası doğru mu kontrol edin
3. ✅ Android cihaz internete veya yerel ağa bağlı mı
4. ✅ Firewall ayarlarını kontrol edin
5. ✅ AndroidManifest.xml'de INTERNET ve CAMERA izinleri var mı

### Logları kontrol edin:
Android Studio'da Logcat'te "RTMP" veya "ConnectChecker" filtreleyin

---

## Test RTMP URLs (Ücretsiz Test Sunucuları)

⚠️ **Uyarı**: Bu sunucular herkese açık ve güvenli değildir, sadece test için kullanın!

- **RTMP URL**: `rtmp://live.twitch.tv/app/`
- **Stream Key**: Twitch hesabınızdan stream key alın

veya

- **RTMP URL**: `rtmp://a.rtmp.youtube.com/live2`
- **Stream Key**: YouTube'dan alın
