# RTMP Sunucu Kurulum Rehberi

Bu dokümanda RTMP sunucusu kurulumu için detaylı adımlar bulunmaktadır.

## 1. Ubuntu/Linux üzerinde Nginx-RTMP Kurulumu

### Adım 1: Sistemi Güncelleyin

```bash
sudo apt update
sudo apt upgrade -y
```

### Adım 2: Nginx ve RTMP Modülünü Yükleyin

```bash
sudo apt install nginx libnginx-mod-rtmp -y
```

### Adım 3: Nginx Yapılandırmasını Düzenleyin

```bash
sudo nano /etc/nginx/nginx.conf
```

Dosyanın **sonuna** aşağıdaki yapılandırmayı ekleyin:

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;
        
        application live {
            live on;
            record off;
            
            # İstatistikler için
            allow publish all;
            allow play all;
            
            # HLS desteği (opsiyonel)
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3;
            hls_playlist_length 60;
            
            # DASH desteği (opsiyonel)
            dash on;
            dash_path /tmp/dash;
        }
    }
}
```

### Adım 4: HTTP için Nginx Yapılandırması (HLS için gerekli)

```bash
sudo nano /etc/nginx/sites-available/default
```

`server` bloğu içine ekleyin:

```nginx
location /hls {
    types {
        application/vnd.apple.mpegurl m3u8;
        video/mp2t ts;
    }
    root /tmp;
    add_header Cache-Control no-cache;
    add_header Access-Control-Allow-Origin *;
}

location /dash {
    root /tmp;
    add_header Cache-Control no-cache;
    add_header Access-Control-Allow-Origin *;
}
```

### Adım 5: Gerekli Klasörleri Oluşturun

```bash
sudo mkdir -p /tmp/hls
sudo mkdir -p /tmp/dash
sudo chmod -R 755 /tmp/hls
sudo chmod -R 755 /tmp/dash
```

### Adım 6: Nginx'i Test Edin ve Başlatın

```bash
# Yapılandırmayı test et
sudo nginx -t

# Nginx'i yeniden başlat
sudo systemctl restart nginx

# Nginx'in çalıştığını kontrol et
sudo systemctl status nginx
```

### Adım 7: Firewall Ayarları

```bash
# RTMP portu (1935)
sudo ufw allow 1935/tcp

# HTTP portu (80)
sudo ufw allow 80/tcp

# Firewall'u aktifleştir
sudo ufw enable
```

## 2. Docker ile RTMP Sunucusu

### Basit Kurulum

```bash
docker run -d \
    -p 1935:1935 \
    -p 8080:8080 \
    --name rtmp-server \
    tiangolo/nginx-rtmp
```

### Docker Compose ile Kurulum

`docker-compose.yml` dosyası oluşturun:

```yaml
version: '3'
services:
  rtmp:
    image: tiangolo/nginx-rtmp
    ports:
      - "1935:1935"
      - "8080:8080"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    restart: unless-stopped
```

Başlatın:

```bash
docker-compose up -d
```

## 3. Windows üzerinde RTMP Sunucusu

### OBS Studio (Yayıncı olarak)

1. [OBS Studio](https://obsproject.com/) indirin ve yükleyin
2. Ayarlar → Yayın → Sunucu: `rtmp://localhost/live`
3. Stream Key: `test`

### Nginx-RTMP Windows

1. [Nginx Windows](http://nginx.org/en/download.html) indirin
2. RTMP modülünü derleyin (karmaşık) veya hazır binary kullanın
3. Yapılandırma dosyasını düzenleyin

## 4. Bulut Tabanlı RTMP Sunucusu

### AWS EC2

1. Ubuntu 20.04 EC2 instance oluşturun
2. Security Group'ta 1935 portunu açın
3. SSH ile bağlanıp yukarıdaki nginx-rtmp kurulumunu yapın

### DigitalOcean Droplet

1. Ubuntu Droplet oluşturun
2. Firewall'da 1935 portunu açın
3. SSH ile bağlanıp nginx-rtmp kurulumunu yapın

## 5. Test Etme

### FFmpeg ile Test Yayını

```bash
# Video dosyasından yayın
ffmpeg -re -i video.mp4 -c:v libx264 -c:a aac -f flv rtmp://localhost:1935/live/test

# Webcam'den canlı yayın
ffmpeg -f v4l2 -i /dev/video0 -c:v libx264 -c:a aac -f flv rtmp://localhost:1935/live/webcam
```

### VLC ile İzleme

1. VLC Media Player açın
2. Media → Open Network Stream
3. URL: `rtmp://[SUNUCU_IP]:1935/live/test`
4. Play

### OBS ile Yayın

1. OBS Studio açın
2. Ayarlar → Yayın
3. Sunucu: `rtmp://[SUNUCU_IP]:1935/live`
4. Stream Key: `mystream`
5. Yayını Başlat

## 6. İstatistikler ve İzleme

### Nginx RTMP Stats

`nginx.conf` içine ekleyin:

```nginx
http {
    server {
        listen 8080;
        
        location /stat {
            rtmp_stat all;
            rtmp_stat_stylesheet stat.xsl;
        }
        
        location /stat.xsl {
            root /usr/local/nginx/html;
        }
    }
}
```

İstatistiklere erişim: `http://[SUNUCU_IP]:8080/stat`

## 7. Güvenlik

### IP Kısıtlaması

```nginx
application live {
    live on;
    
    # Sadece belirli IP'lerden yayın
    allow publish 192.168.1.0/24;
    deny publish all;
    
    # Herkes izleyebilir
    allow play all;
}
```

### Stream Key Doğrulama

```nginx
application live {
    live on;
    
    # PHP ile stream key kontrolü
    on_publish http://localhost/auth.php;
}
```

`auth.php`:

```php
<?php
$stream_key = $_POST['name'];
$valid_keys = ['secret_key_1', 'secret_key_2'];

if (in_array($stream_key, $valid_keys)) {
    http_response_code(200);
} else {
    http_response_code(403);
}
?>
```

## 8. Performans Optimizasyonu

### Nginx Ayarları

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;
        
        # Buffer boyutları
        buflen 5s;
        max_streams 32;
        
        application live {
            live on;
            
            # Düşük gecikme
            sync 10ms;
            
            # Bağlantı zaman aşımı
            drop_idle_publisher 10s;
        }
    }
}
```

## 9. Sorun Giderme

### Port 1935 Dinlemiyor

```bash
# Port kontrolü
sudo netstat -tulpn | grep 1935

# Nginx logları
sudo tail -f /var/log/nginx/error.log
```

### Bağlantı Reddediliyor

```bash
# Firewall kontrolü
sudo ufw status

# SELinux kontrolü (CentOS/RHEL)
sudo getenforce
```

### Yayın Gönderilemiyor

1. Nginx yapılandırmasını kontrol edin
2. Logları inceleyin
3. Network bağlantısını test edin
4. Stream URL ve key'i doğrulayın

## 10. Yedekleme ve Kayıt

### Otomatik Kayıt

```nginx
application live {
    live on;
    
    # Yayınları kaydet
    record all;
    record_path /var/recordings;
    record_suffix _%Y%m%d_%H%M%S.flv;
    
    # Maksimum dosya boyutu
    record_max_size 100M;
}
```

Kayıt klasörünü oluşturun:

```bash
sudo mkdir -p /var/recordings
sudo chown -R www-data:www-data /var/recordings
```

## Sonuç

RTMP sunucunuz artık hazır! Android uygulamanızdan bu sunucuya yayın yapabilirsiniz.

Test için:
- **RTMP URL**: `rtmp://[SUNUCU_IP]:1935/live`
- **Stream Key**: `stream` (veya istediğiniz key)

Örnek: `rtmp://192.168.1.100:1935/live/mystream`
