# GrizmAuth - Minecraft Paper / Spigot Kimlik Doğrulama Eklentisi (1.21.1)

GrizmMC Network için özel olarak geliştirilmiş; **GrizmMC Launcher** ve **Web API** entegrasyonlu, şifresiz ve otomatik doğrulamalı yeni nesil Minecraft sunucu kimlik doğrulama eklentisi.

---

## 🎯 Temel Özellikler

1. **Launcher Tabanlı Şifresiz Otomatik Giriş:**
   - Oyuncu resmi GrizmMC Launcher üzerinden **OYNA** butonuna bastığında Launcher, Web API'ye (`/api/auth.php`) geçici bir canlı oturum kaydı oluşturur.
   - Oyuncu Minecraft sunucusuna bağlandığında, **GrizmAuth** arka planda asenkron olarak Web API'yi sorgular.
   - Doğrulanan oyuncu hiçbir şifre veya komut yazmadan doğrudan oyuna başlar. Özel karşılama başlığı ve ses efekti oynatılır.

2. **Harici / Korsan İstemci Koruması (Anti-Bypass):**
   - Sunucuya resmi Launcher dışında (TLauncher, standart istemci vb.) doğrudan bağlanmaya çalışan oyuncular sunucuya alınmaz.
   - Şık, monokrom ve bilgilendirici bir kick ekranı ile oyuncuya Launcher indirme bağlantısı gösterilir.

3. **Yedek Doğrulama (Fallback / Kod Girişi Modu):**
   - `security.require-launcher: false` olarak ayarlanırsa, Launcher olmadan giren oyuncular dondurulur, körlük efekti verilir ve web profilindeki tek kullanımlık 6 haneli kodla giriş yapmaları istenir:
     - `/login <kod>` veya `/grizmauth <code>`

4. **Yönetici Araçları:**
   - `/grizmauth status`: Web API bağlantısını ve online doğrulanmış oyuncu sayısını gösterir.
   - `/grizmauth check <oyuncu>`: Oyuncunun Launcher doğrulama ve GrizmID e-posta durumunu sorgular.
   - `/grizmauth reload`: `config.yml` dosyasını oyunu durdurmadan yeniler.

---

## 📁 Dosya Yapısı

```
Grizm Auth/
├── pom.xml                                   # Paper 1.21.1 Maven Proje Dosyası
├── build.bat                                 # 1-Tıkla Otomatik Derleme Scripti
├── src/main/resources/
│   ├── plugin.yml                            # Eklenti Tanımı ve Komutlar
│   └── config.yml                            # API ve Güvenlik Ayarları
└── src/main/java/com/grizmtech/auth/
    ├── GrizmAuth.java                        # Ana Plugin Sınıfı
    ├── api/
    │   ├── AuthApiClient.java                # Web API Asenkron HTTP İstemcisi
    │   └── AuthVerifyResult.java             # API Yanıt Modeli
    ├── config/
    │   └── Settings.java                     # Yapılandırma ve Renk Yöneticisi
    ├── model/
    │   └── UserProfileData.java              # Oyuncu Profil Modeli
    ├── session/
    │   └── SessionManager.java               # Oturum Durumu ve Önbellek
    ├── listeners/
    │   ├── PreLoginListener.java             # Giriş Öncesi Launcher Doğrulayıcı
    │   ├── PlayerJoinQuitListener.java       # Başlık, Ses ve Oturum Yaşam Döngüsü
    │   └── PlayerRestrictionListener.java    # Doğrulanmamış Oyuncuyu Kısıtlama
    └── commands/
        ├── GrizmAuthCommand.java             # /grizmauth Komut Yöneticisi
        └── LoginCommand.java                 # /login Komut Kısayolu
```

---

## 🚀 Derleme ve Kurulum

1. Klasördeki `build.bat` dosyasına çift tıklayın (veya terminalden `mvn clean package` çalıştırın).
2. Derlenen `target/GrizmAuth-1.0.0.jar` dosyası otomatik olarak `server/SONYEDEK/lobby/plugins` klasörüne kopyalanır.
3. Sunucuyu başlatın veya `/reload confirm` komutunu verin.
