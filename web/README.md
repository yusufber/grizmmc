# GrizmMC Network - Siyah Beyaz (Noir) Minecraft Web Sitesi

GrizmMC Network için özel olarak hazırlanmış, modern monokrom (siyah-beyaz), lüks ve dinamik PHP Minecraft sunucu web sitesi.

---

## 📁 Proje Dizin Yapısı

```
web/
├── api/
│   ├── auth.php            # Launcher & Minecraft Paper sunucu oturum doğrulama API'si
│   ├── status.php          # Gerçek zamanlı sunucu çevrimiçi oyuncu sorgusu ve önbellek
│   └── user.php            # Oyuncu profili ve avatar sorgu API'si
├── assets/
│   ├── css/
│   │   └── style.css       # Özel siyah-beyaz lüks noir tasarım, bilet ve başvuru stilleri
│   └── js/
│       └── main.js         # IP kopyalama, ses efekti, parçacık kanvası ve modal kontrolü
├── data/                   # Güvenli JSON flat-file veritabanı (.htaccess ile korumalı)
│   ├── users.json          # Kayıtlı oyuncu profilleri
│   ├── tickets.json        # Destek talepleri ve mesajlaşma geçmişi
│   ├── applications.json   # Kadro & yetkili başvuruları
│   └── sessions.json       # Launcher ve sunucu tek kullanımlık oturumları
├── includes/
│   ├── auth.php            # Oturum, GrizmID OAuth, yetki kontrolleri & kullanıcı adı zorunluluğu
│   ├── handoff.php         # Launcher giriş aktarım (handoff) şablonu ve köprüsü
│   ├── db.php              # Flat-file JSON veritabanı ve yardımcı fonksiyonlar (time_ago_tr)
│   ├── tickets.php         # Destek talepleri (ticket) veri modelleri ve fonksiyonları
│   ├── applications.php    # Yetkili başvuruları modelleri ve fonksiyonları
│   ├── data.php            # Oyun modları, kurallar, kadro ve VIP paketleri verileri
│   ├── header.php          # HTML başlık ve SEO meta etiketleri
│   ├── navbar.php          # Yapışkan (Sticky) şık navigasyon menüsü
│   └── footer.php          # Alt bilgi, telif, sosyal linkler
├── .htaccess               # cPanel/Apache performans ve güvenlik ayarları
├── config.php              # Çekirdek ayarlar (Site, IP, Auth sabitleri) + Modül bootstrap
├── index.php               # Ana sayfa (Hero, oyun modları, özellikler, liderler, CTA)
├── launcher.php            # Launcher tanıtım ve indirme sayfası
├── download.php            # Güvenli doğrudan Launcher indirme yönlendiricisi
├── store.php               # VIP paketleri, avantajlar ve sipariş modal penceresi
├── support.php             # Destek Talebi Merkezi (Bilet açma, listeleme & yetkili havuzu)
├── ticket.php              # Bilet Sohbet & Mesajlaşma Ekranı (Oyuncu-Yetkili canlı yazışma)
├── apply.php               # Yetkili & Kadro Başvuru Portalı (Açık pozisyonlar & inceleme)
├── rules.php               # Kategorize edilmiş sunucu kuralları ve ceza cetveli
├── staff.php               # Minecraft 3D kafa avatarlarıyla yetkili kadrosu
├── profile.php             # Oyuncu profil ve hesap ayarları sayfası
├── login.php               # GrizmID OAuth2 giriş yönlendiricisi
├── logout.php              # Güvenli oturum kapatma
└── set-username.php        # İlk girişte Minecraft kullanıcı adı belirleme & 3D kafa önizleme
```

---

## ⚙️ Ayarları Değiştirme (`config.php`)

- **Web Adresi:** `SITE_URL` (`https://www.grizm.tech/mc`)
- **GrizmID OAuth2:** `AUTH_CLIENT_ID` (`871cea3dbba7a4c10474`), `AUTH_CLIENT_SECRET`, `AUTH_SERVER_URL`
- **Callback URL:** `callback.php` (Auth panelinde tanımlı callback URL)

---

## 🔐 GrizmID Giriş Sistemi & İlk Giriş Kullanıcı Adı Belirleme

- Kullanıcılar **Giriş Yap** butonuna bastığında `login.php` aracılığıyla `https://auth.dlmtd.com/authorize` sayfasına yönlendirilir.
- Kullanıcı onay verdikten sonra `callback.php` adresine tek kullanımlık bir yetkilendirme kodu ile döner.
- `callback.php`, bu kodu arka planda Client Secret ile doğrulatıp kullanıcıyı tanır.
- **İlk Giriş Kontrolü:** Eğer kullanıcı ilk defa giriş yapıyorsa (veya henüz Minecraft kullanıcı adını belirlememişse), otomatik olarak `set-username.php` sayfasına yönlendirilir.
- `set-username.php` sayfasında kullanıcı adını yazarken **anlık 3D Minecraft kafa önizlemesi** gösterilir ve geçerli ad seçildiğinde profil kaydedilir.
- Sonraki girişlerde kullanıcı adı hatırlanır ve doğrudan profil paneline yönlendirilir. Kullanıcı dilediğinde profilinden adını güncelleyebilir (`set-username.php?edit=1`).

---

## 🚀 Hosting / cPanel Kurulumu

1. `web` klasörünün içindeki tüm dosyaları seçin ve bir ZIP arşivine sıkıştırın (veya FTP ile aktarın).
2. Hosting yönetim panelinizde (cPanel, Plesk veya CyberPanel) **Dosya Yöneticisi**ne (File Manager) girin.
3. `public_html` (veya alan adınızın kök dizinine) dosyaları yükleyip çıkarın.
4. `config.php` dosyasını kendi sunucu IP ve Discord adresinize göre düzenleyin.
5. Siteniz anında yayına girecektir! Herhangi bir veritabanı (MySQL) kurulumuna ihtiyaç duymaz, tamamen hafif ve ultra hızlıdır.
