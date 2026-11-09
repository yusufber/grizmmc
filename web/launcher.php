<?php
$pageTitle = "Launcher İndir • Minecraft 1.21.4";
$activePage = "launcher";
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';

$fileSizeMb = 146.4;
$exePath = __DIR__ . '/' . (defined('LAUNCHER_DIRECT_URL') ? LAUNCHER_DIRECT_URL : 'downloads/GrizmMcLauncher.exe');
if (file_exists($exePath)) {
    $fileSizeMb = round(filesize($exePath) / (1024 * 1024), 1);
}
?>

<!-- HERO SECTION -->
<section class="hero-section" style="padding-top: 70px; padding-bottom: 60px;">
  <div class="hero-glow-orb"></div>
  <div class="container">
    <div class="hero-badge" style="border-color: rgba(0,255,136,0.3); color: #00ff88;">
      <i class="fa-solid fa-bolt"></i>
      <span>RESMİ İSTEMCİ • MINECRAFT <?php echo LAUNCHER_MC_VERSION; ?></span>
    </div>

    <h1 class="hero-title" style="margin-bottom: 18px;">
      GRIZMMC<br>
      <span class="glow-text">ÖZEL LAUNCHER</span>
    </h1>

    <p class="hero-subtitle" style="max-width: 680px; margin: 0 auto 32px auto;">
      GrizmMC Network dünyasına tek tıkla doğrudan bağlanın. Mojang manifesti üzerinden eksik dosyaları otomatik indirir, GrizmID ile güvenli giriş sağlar ve sıfır gecikmeyle oyuna sokar.
    </p>

    <!-- Büyük İndirme Kartı / Aksiyon Alanı -->
    <div style="background: #0d0d12; border: 1px solid var(--border-strong); border-radius: var(--radius-lg); padding: 36px 32px; max-width: 620px; margin: 0 auto 36px auto; box-shadow: 0 30px 80px rgba(0,0,0,0.8), 0 0 45px rgba(255,255,255,0.05); text-align: center;">
      <div style="display: inline-flex; align-items: center; gap: 10px; background: #161620; border: 1px solid #282836; padding: 6px 16px; border-radius: 20px; font-size: 0.8rem; font-family: var(--font-mono); color: #bbb; margin-bottom: 22px;">
        <i class="fa-brands fa-windows" style="color: #00ff88;"></i>
        <span>Windows 10 / 11 (64-Bit)</span>
        <span>•</span>
        <span><?php echo $fileSizeMb; ?> MB</span>
        <span>•</span>
        <span style="color: #00ff88;">v<?php echo LAUNCHER_VERSION; ?></span>
      </div>

      <div style="margin-bottom: 24px;">
        <a href="<?php echo LAUNCHER_DOWNLOAD_URL; ?>" download class="btn btn-primary btn-lg" style="width: 100%; font-size: 1.15rem; padding: 18px 28px; box-shadow: 0 0 40px rgba(255,255,255,0.3);">
          <i class="fa-solid fa-download" style="font-size: 1.3rem;"></i>
          <span>LAUNCHER'I İNDİR (.EXE)</span>
        </a>
      </div>

      <div style="margin-bottom: 24px;">
        <a href="launcher/" class="btn btn-secondary btn-lg" style="width: 100%; font-size: 1rem; padding: 14px 24px; border: 1px solid rgba(0, 255, 136, 0.4); background: rgba(0, 255, 136, 0.08); color: #00ff88; display: inline-flex; align-items: center; justify-content: center; gap: 10px; border-radius: var(--radius-sm); text-decoration: none; transition: all 0.25s ease;">
          <i class="fa-solid fa-cube" style="font-size: 1.15rem;"></i>
          <span>TARAYICIDA 3D WEB LAUNCHER'I AÇ (OTOMATİK BAĞLAN)</span>
        </a>
      </div>

      <div style="display: flex; align-items: center; justify-content: center; gap: 20px; flex-wrap: wrap; font-size: 0.82rem; color: var(--text-muted);">
        <span><i class="fa-solid fa-check" style="color: #00ff88; margin-right: 5px;"></i> Java Kurulumu Gerekmez</span>
        <span><i class="fa-solid fa-check" style="color: #00ff88; margin-right: 5px;"></i> GrizmID Entegre</span>
        <span><i class="fa-solid fa-check" style="color: #00ff88; margin-right: 5px;"></i> Doğrudan Bağlantı</span>
      </div>
    </div>

    <!-- Hızlı Bilgi Rozetleri -->
    <div class="hero-meta-stats">
      <div class="stat-item">
        <i class="fa-solid fa-microchip"></i>
        <span>Hedef Sürüm: <strong>Minecraft <?php echo LAUNCHER_MC_VERSION; ?></strong></span>
      </div>
      <div class="stat-item">
        <i class="fa-solid fa-server"></i>
        <span>Sunucu: <strong><?php echo SERVER_IP; ?></strong></span>
      </div>
      <div class="stat-item">
        <i class="fa-solid fa-shield-halved"></i>
        <span>Güvenlik: <strong>SHA-256 Doğrulamalı</strong></span>
      </div>
    </div>
  </div>
</section>

<!-- ÖZELLİKLER SECTION -->
<section class="section" style="padding-top: 10px;">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Neden Bizim Launcher'ımız?</span>
      <h2 class="section-title">GELİŞMİŞ ÖZELLİKLER</h2>
      <p class="section-desc">
        Oyuncularımızın en hızlı ve zahmetsiz şekilde sunucumuza bağlanabilmesi için özel olarak tasarlandı.
      </p>
    </div>

    <div class="features-grid">
      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-bolt"></i>
        </div>
        <h4 class="feature-box-title">Doğrudan Sunucu Bağlantısı</h4>
        <p class="feature-box-desc">
          Oyna butonuna bastığınız anda Minecraft açılır ve sunucu listesinde IP aramadan doğrudan <strong><?php echo SERVER_IP; ?></strong> adresine giriş yapar.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-fingerprint"></i>
        </div>
        <h4 class="feature-box-title">GrizmID Web Senkronizasyonu</h4>
        <p class="feature-box-desc">
          Web sitemiz ve <strong>auth.dlmtd.com</strong> üzerindeki GrizmID hesabınızla tek tıkla oturum açın; Minecraft adınız ve 3D kafa avatarınız otomatik yüklenir.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-download"></i>
        </div>
        <h4 class="feature-box-title">Otomatik 1.21.4 Manifesti</h4>
        <p class="feature-box-desc">
          Eksik istemci jar dosyalarını ve kütüphaneleri resmi Mojang meta sunucularından eksiksiz indirerek otomatik çalıştırır.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-memory"></i>
        </div>
        <h4 class="feature-box-title">Dinamik RAM Tahsisi</h4>
        <p class="feature-box-desc">
          Launcher içindeki kaydırma çubuğu ile bilgisayarınızın gücüne göre 2 GB ile 16 GB arasında bellek miktarını kolayca belirleyin.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-desktop"></i>
        </div>
        <h4 class="feature-box-title">Bağımsız Tek Dosya (.exe)</h4>
        <p class="feature-box-desc">
          Kurulum gerektirmez! İndirdiğiniz tek bir dosyaya çift tıklayarak anında çalıştırabilirsiniz.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-tower-broadcast"></i>
        </div>
        <h4 class="feature-box-title">Canlı Sunucu Ping & Sayacı</h4>
        <p class="feature-box-desc">
          Launcher arayüzünde aktif sunucu durumunu ve anlık çevrimiçi oyuncu sayısını oyuna girmeden canlı olarak takip edin.
        </p>
      </div>
    </div>
  </div>
</section>

<!-- ADIM ADIM KURULUM REHBERİ -->
<section class="section" style="background: #09090c; border-top: 1px solid var(--border-medium); border-bottom: 1px solid var(--border-medium);">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Kolay Kurulum</span>
      <h2 class="section-title">NASIL BAŞLARIM?</h2>
      <p class="section-desc">
        Yalnızca 3 basit adımda GrizmMC Network sunucusuna katılabilirsiniz.
      </p>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; max-width: 1000px; margin: 0 auto;">
      
      <div class="feature-box" style="position: relative; padding: 32px 28px;">
        <span style="font-family: var(--font-mono); font-size: 2.2rem; font-weight: 900; color: rgba(255,255,255,0.12); position: absolute; top: 18px; right: 22px;">01</span>
        <div class="feature-box-icon" style="margin-bottom: 18px;">
          <i class="fa-solid fa-cloud-arrow-down"></i>
        </div>
        <h4 class="feature-box-title" style="margin-bottom: 10px;">İndir ve Çalıştır</h4>
        <p class="feature-box-desc">
          Yukarıdaki butona tıklayarak <strong>GrizmMcLauncher.exe</strong> dosyasını bilgisayarınıza indirin ve çift tıklayarak açın.
        </p>
      </div>

      <div class="feature-box" style="position: relative; padding: 32px 28px;">
        <span style="font-family: var(--font-mono); font-size: 2.2rem; font-weight: 900; color: rgba(255,255,255,0.12); position: absolute; top: 18px; right: 22px;">02</span>
        <div class="feature-box-icon" style="margin-bottom: 18px;">
          <i class="fa-solid fa-id-card"></i>
        </div>
        <h4 class="feature-box-title" style="margin-bottom: 10px;">GrizmID ile Giriş Yap</h4>
        <p class="feature-box-desc">
          "Grizm ID ile Giriş Yap" butonuna basın. Açılan tarayıcı sekmesinde oturumunuzu onaylayın; bilgiler Launcher'a anında aktarılacaktır.
        </p>
      </div>

      <div class="feature-box" style="position: relative; padding: 32px 28px;">
        <span style="font-family: var(--font-mono); font-size: 2.2rem; font-weight: 900; color: rgba(255,255,255,0.12); position: absolute; top: 18px; right: 22px;">03</span>
        <div class="feature-box-icon" style="margin-bottom: 18px;">
          <i class="fa-solid fa-gamepad"></i>
        </div>
        <h4 class="feature-box-title" style="margin-bottom: 10px;">Oyna'ya Bas ve Katıl!</h4>
        <p class="feature-box-desc">
          İstediğiniz RAM miktarını ayarlayıp büyük <strong>OYNA</strong> butonuna tıklayın. Minecraft 1.21.4 açılıp doğrudan sunucuya bağlanacaktır.
        </p>
      </div>

    </div>

    <!-- Alt İndirme Çağrısı -->
    <div style="text-align: center; margin-top: 50px;">
      <a href="<?php echo LAUNCHER_DOWNLOAD_URL; ?>" download class="btn btn-primary btn-lg" style="box-shadow: 0 0 35px rgba(255,255,255,0.25);">
        <i class="fa-brands fa-windows"></i>
        <span>GrizmMcLauncher.exe İndir (Windows)</span>
      </a>
    </div>
  </div>
</section>

<!-- SIKÇA SORULAN SORULAR -->
<section class="section">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Yardım & Destek</span>
      <h2 class="section-title">SIKÇA SORULAN SORULAR</h2>
      <p class="section-desc">
        Launcher ile ilgili merak edilen detaylar ve çözümler.
      </p>
    </div>

    <div style="max-width: 760px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px;">
      
      <div style="background: #111116; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 22px 26px;">
        <h4 style="font-size: 1.05rem; font-weight: 700; color: #fff; margin-bottom: 8px;">
          <i class="fa-solid fa-circle-question" style="color: #00ff88; margin-right: 8px;"></i>
          Bilgisayarımda Java yüklü olması şart mı?
        </h4>
        <p style="font-size: 0.92rem; color: var(--text-secondary); line-height: 1.6;">
          Hayır. Launcher, sisteminizde Java 21 olup olmadığını otomatik denetler. Eğer bulunamazsa resmi Java Runtime bileşenlerini arka planda yapılandırır.
        </p>
      </div>

      <div style="background: #111116; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 22px 26px;">
        <h4 style="font-size: 1.05rem; font-weight: 700; color: #fff; margin-bottom: 8px;">
          <i class="fa-solid fa-circle-question" style="color: #00ff88; margin-right: 8px;"></i>
          Windows Defender veya SmartScreen uyarısı alırsam ne yapmalıyım?
        </h4>
        <p style="font-size: 0.92rem; color: var(--text-secondary); line-height: 1.6;">
          Uygulama yeni derlendiği ve henüz Microsoft dijital sertifikasıyla imzalanmadığı için Windows SmartScreen "Korumalı Bilgisayar" uyarısı verebilir. <strong>"Ek Bilgi"</strong> seçeneğine tıklayıp <strong>"Yine de Çalıştır"</strong> butonuna basarak güvenle açabilirsiniz.
        </p>
      </div>

      <div style="background: #111116; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 22px 26px;">
        <h4 style="font-size: 1.05rem; font-weight: 700; color: #fff; margin-bottom: 8px;">
          <i class="fa-solid fa-circle-question" style="color: #00ff88; margin-right: 8px;"></i>
          GrizmID olmadan sunucuya girebilir miyim?
        </h4>
        <p style="font-size: 0.92rem; color: var(--text-secondary); line-height: 1.6;">
          GrizmMC Network altyapısı güvenli hesap yönetimi için GrizmID OAuth2 kullanır. Tek bir hesapla hem web sitemize, hem mağazamıza, hem de sunucumuza güvenle erişebilirsiniz.
        </p>
      </div>

    </div>
  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
