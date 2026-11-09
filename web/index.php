<?php
$pageTitle = "Ana Sayfa";
$activePage = "home";
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<!-- HERO SECTION -->
<section class="hero-section">
  <div class="hero-glow-orb"></div>
  <div class="container">
    <div class="hero-badge">
      <i class="fa-solid fa-sparkles"></i>
      <span>GRIZMMC NETWORK • <?php echo SERVER_VERSION; ?></span>
    </div>

    <h1 class="hero-title">
      SINIRLARI AŞAN<br>
      <span class="glow-text">MINECRAFT DÜNYASI</span>
    </h1>

    <p class="hero-subtitle">
      <?php echo htmlspecialchars(SERVER_TAGLINE); ?>. Gelişmiş SkyBlock mekanikleri, rekabet dolu PvP arenaları ve klan savaşlarıyla dolu Hardcore SMP seni bekliyor.
    </p>

    <!-- Sunucu Bağlantı & IP Kopyalama Kutusu -->
    <div class="hero-connect-bar">
      <div class="ip-details">
        <div class="ip-icon">
          <i class="fa-solid fa-server"></i>
        </div>
        <div class="ip-text-wrap">
          <span class="ip-label">Sunucu IP Adresi (Java & Bedrock)</span>
          <span class="ip-address"><?php echo SERVER_IP; ?></span>
        </div>
      </div>

      <button class="copy-action-btn" data-copy-ip="<?php echo SERVER_IP; ?>">
        <i class="fa-solid fa-copy"></i>
        <span class="copy-label">IP'Yİ KOPYALA</span>
      </button>
    </div>

    <!-- Hızlı Launcher İndirme Alanı -->
    <div style="margin-top: 24px; display: flex; align-items: center; justify-content: center; gap: 14px; flex-wrap: wrap;">
      <a href="<?php echo LAUNCHER_DOWNLOAD_URL; ?>" download class="btn btn-primary btn-lg" style="box-shadow: 0 0 35px rgba(255,255,255,0.22); font-size: 1rem; padding: 14px 28px;">
        <i class="fa-brands fa-windows" style="font-size: 1.25rem;"></i>
        <span>Launcher'ı İndir</span>
        <span style="background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); padding: 2px 8px; border-radius: 4px; font-size: 0.72rem; font-family: var(--font-mono); margin-left: 6px;">v<?php echo LAUNCHER_VERSION; ?> • 1.21.4</span>
      </a>
      <a href="launcher.php" class="btn btn-ghost btn-lg" style="font-size: 0.95rem; padding: 14px 24px;">
        <i class="fa-solid fa-circle-info"></i>
        <span>Launcher Özellikleri</span>
      </a>
    </div>

    <!-- Hızlı Durum Rozetleri -->
    <div class="hero-meta-stats">
      <div class="stat-item">
        <span class="pulse-dot"></span>
        <span>Durum: <strong>Çevrimiçi</strong></span>
      </div>
      <div class="stat-item">
        <i class="fa-solid fa-users"></i>
        <span>Aktif Oyuncular: <strong class="server-player-count">148</strong> / 500</span>
      </div>
      <div class="stat-item">
        <i class="fa-solid fa-bolt"></i>
        <span>Sürüm: <strong><?php echo SERVER_VERSION; ?></strong></span>
      </div>
      <div class="stat-item">
        <i class="fa-solid fa-shield"></i>
        <span>Altyapı: <strong>Özel Anti-DDoS</strong></span>
      </div>
    </div>
  </div>
</section>

<!-- OYUN MODLARI SECTION -->
<section class="section" id="modes">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Özenle Tasarlanmış Dünyalar</span>
      <h2 class="section-title">OYUN MODLARI</h2>
      <p class="section-desc">
        Her biri özel olarak optimize edilmiş, benzersiz mekaniklere ve dengeli ekonomiye sahip oyun modlarımızı keşfedin.
      </p>
    </div>

    <div class="game-modes-grid">
      <?php foreach ($GAME_MODES as $mode): ?>
        <div class="mode-card">
          <div>
            <div class="mode-card-header">
              <div class="mode-icon">
                <i class="<?php echo htmlspecialchars($mode['icon']); ?>"></i>
              </div>
              <span class="mode-badge"><?php echo htmlspecialchars($mode['badge']); ?></span>
            </div>

            <h3 class="mode-title"><?php echo htmlspecialchars($mode['name']); ?></h3>
            <div class="mode-version">
              <i class="fa-solid fa-code-branch"></i> Sürüm <?php echo htmlspecialchars($mode['version']); ?>
            </div>

            <p class="mode-desc"><?php echo htmlspecialchars($mode['description']); ?></p>

            <ul class="mode-feature-list">
              <?php foreach ($mode['features'] as $feature): ?>
                <li>
                  <i class="fa-solid fa-check"></i>
                  <span><?php echo htmlspecialchars($feature); ?></span>
                </li>
              <?php endforeach; ?>
            </ul>
          </div>

          <button class="btn btn-ghost btn-sm" data-copy-ip="<?php echo SERVER_IP; ?>" style="width: 100%;">
            <i class="fa-solid fa-play"></i>
            <span>Hemen Oyna</span>
          </button>
        </div>
      <?php endforeach; ?>
    </div>
  </div>
</section>

<!-- RESMİ LAUNCHER TANITIM SECTION -->
<section class="section" style="padding-top: 20px; padding-bottom: 40px;">
  <div class="container">
    <div style="background: linear-gradient(135deg, #111117 0%, #0c0c10 100%); border: 1px solid var(--border-strong); border-radius: var(--radius-lg); padding: 48px 40px; box-shadow: 0 25px 60px rgba(0,0,0,0.8), 0 0 40px rgba(255,255,255,0.03); display: grid; grid-template-columns: 1.2fr 1fr; gap: 40px; align-items: center;">
      
      <div>
        <div style="display: inline-flex; align-items: center; gap: 8px; background: rgba(0,255,136,0.1); border: 1px solid rgba(0,255,136,0.3); color: #00ff88; padding: 6px 14px; border-radius: 20px; font-size: 0.78rem; font-weight: 700; font-family: var(--font-mono); margin-bottom: 16px;">
          <i class="fa-solid fa-bolt"></i>
          <span>RESMİ 1.21.4 LAUNCHER</span>
        </div>
        
        <h2 style="font-family: var(--font-display); font-size: 2.1rem; font-weight: 800; color: #ffffff; line-height: 1.25; margin-bottom: 16px;">
          TEK TIKLA DOĞRUDAN<br>
          <span style="color: #ffffff; text-shadow: 0 0 25px rgba(255,255,255,0.35);">SUNUCUYA BAĞLAN</span>
        </h2>
        
        <p style="color: var(--text-secondary); font-size: 0.96rem; line-height: 1.7; margin-bottom: 24px;">
          Karmaşık Minecraft veya Java kurulumlarıyla uğraşmayın. GrizmMC Launcher, gerekli tüm dosyaları ve Java 21 altyapısını otomatik yapılandırır; GrizmID hesabınızla tek tıkla oturum açıp doğrudan <strong>play-mc.grizm.tech</strong> sunucusuna girmenizi sağlar.
        </p>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 28px;">
          <div style="background: #14141a; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 12px 14px; display: flex; align-items: center; gap: 10px;">
            <i class="fa-solid fa-fingerprint" style="color: #00ff88; font-size: 1.1rem;"></i>
            <span style="font-size: 0.84rem; font-weight: 600; color: #eee;">GrizmID Doğrulama</span>
          </div>
          <div style="background: #14141a; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 12px 14px; display: flex; align-items: center; gap: 10px;">
            <i class="fa-solid fa-microchip" style="color: #00ff88; font-size: 1.1rem;"></i>
            <span style="font-size: 0.84rem; font-weight: 600; color: #eee;">Dinamik RAM Ayarı</span>
          </div>
          <div style="background: #14141a; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 12px 14px; display: flex; align-items: center; gap: 10px;">
            <i class="fa-solid fa-cloud-arrow-down" style="color: #00ff88; font-size: 1.1rem;"></i>
            <span style="font-size: 0.84rem; font-weight: 600; color: #eee;">Otomatik 1.21.4</span>
          </div>
          <div style="background: #14141a; border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 12px 14px; display: flex; align-items: center; gap: 10px;">
            <i class="fa-solid fa-gauge-high" style="color: #00ff88; font-size: 1.1rem;"></i>
            <span style="font-size: 0.84rem; font-weight: 600; color: #eee;">60+ FPS Performans</span>
          </div>
        </div>

        <div style="display: flex; align-items: center; gap: 14px; flex-wrap: wrap;">
          <a href="<?php echo LAUNCHER_DOWNLOAD_URL; ?>" download class="btn btn-primary btn-lg" style="box-shadow: 0 0 35px rgba(255,255,255,0.25);">
            <i class="fa-brands fa-windows"></i>
            <span>Hemen İndir (.exe)</span>
          </a>
          <a href="launcher.php" class="btn btn-ghost btn-lg">
            <span>Kurulum Rehberi</span>
            <i class="fa-solid fa-arrow-right"></i>
          </a>
        </div>
      </div>

      <!-- Sağ Taraf: Launcher Önizleme / Mockup Kartı -->
      <div style="position: relative;">
        <div style="background: #0e0e13; border: 1px solid var(--border-strong); border-radius: var(--radius-md); padding: 26px 22px; box-shadow: 0 20px 50px rgba(0,0,0,0.9); text-align: left;">
          <!-- Pencere Başlık Barı -->
          <div style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-medium); padding-bottom: 12px; margin-bottom: 18px;">
            <div style="display: flex; align-items: center; gap: 7px;">
              <span style="width: 10px; height: 10px; border-radius: 50%; background: #ff5f56; display: inline-block;"></span>
              <span style="width: 10px; height: 10px; border-radius: 50%; background: #ffbd2e; display: inline-block;"></span>
              <span style="width: 10px; height: 10px; border-radius: 50%; background: #27c93f; display: inline-block;"></span>
            </div>
            <span style="font-family: var(--font-mono); font-size: 0.74rem; color: var(--text-muted);">GrizmMcLauncher.exe • 1.21.4</span>
          </div>

          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
            <div>
              <div style="font-family: var(--font-display); font-weight: 800; font-size: 1.05rem; color: #fff;">GRIZM MC LAUNCHER</div>
              <div style="font-size: 0.74rem; color: #00ff88; font-family: var(--font-mono); margin-top: 2px;">● play-mc.grizm.tech</div>
            </div>
            <span style="background: #1c1c24; border: 1px solid #2e2e38; color: #ddd; font-size: 0.7rem; padding: 4px 8px; border-radius: 4px; font-weight: 700;">v1.0.0</span>
          </div>

          <!-- Mockup RAM & Durum -->
          <div style="background: #121217; border: 1px solid var(--border-medium); border-radius: 8px; padding: 14px; margin-bottom: 14px;">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
              <span style="font-size: 0.78rem; color: #aaa;">Tahsis Edilen RAM:</span>
              <span style="font-family: var(--font-mono); font-size: 0.78rem; color: #fff; font-weight: bold;">4 GB</span>
            </div>
            <div style="background: #22222a; height: 6px; border-radius: 3px; overflow: hidden;">
              <div style="background: #fff; width: 50%; height: 100%;"></div>
            </div>
          </div>

          <div style="display: flex; align-items: center; justify-content: space-between; background: #08080a; border: 1px solid #1c1c24; padding: 12px 16px; border-radius: 8px;">
            <div style="font-size: 0.8rem; color: #888;">
              <strong style="color: #fff; display: block;">Minecraft 1.21.4</strong>
              GrizmID Girişi Aktif
            </div>
            <div style="background: #fff; color: #000; font-weight: 900; font-size: 0.84rem; padding: 8px 18px; border-radius: 6px; text-transform: uppercase;">
              OYNA
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</section>

<!-- NEDEN BİZ? / ÖZELLİKLER SECTION -->
<section class="section">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Altyapı & Standartlar</span>
      <h2 class="section-title">NEDEN GRIZMMC?</h2>
      <p class="section-desc">
        Kesintisiz 20 TPS performansı ve adil rekabet ortamını sunmak için en güçlü donanımlarla çalışıyoruz.
      </p>
    </div>

    <div class="features-grid">
      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-shield-halved"></i>
        </div>
        <h4 class="feature-box-title">Gelişmiş Anti-Cheat</h4>
        <p class="feature-box-desc">
          Yapay zeka destekli hile koruma sistemimiz sayesinde haksız avantaj sağlayan yazılımlar anında tespit edilir ve engellenir.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-microchip"></i>
        </div>
        <h4 class="feature-box-title">Ultra Yüksek Donanım</h4>
        <p class="feature-box-desc">
          En son nesil AMD Ryzen işlemciler ve NVMe SSD depolama ile lag ve donma olmadan akıcı bir oyun zevki.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-arrows-split-up-and-left"></i>
        </div>
        <h4 class="feature-box-title">Dengeli Ekonomi</h4>
        <p class="feature-box-desc">
          Pay to win (P2W) mantığına karşı, tamamen oyuncu emeğini ödüllendiren adil ve sürdürülebilir pazar dinamikleri.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-trophy"></i>
        </div>
        <h4 class="feature-box-title">Haftalık Nakit Ödüller</h4>
        <p class="feature-box-desc">
          Klan savaşları ve ada sıralamalarında zirveye oturan oyuncularımıza her sezon nakit para ve bakiye ödülleri.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-headset"></i>
        </div>
        <h4 class="feature-box-title">7/24 Aktif Destek</h4>
        <p class="feature-box-desc">
          Discord sunucumuzdaki bilet sistemiyle sorularınıza ve taleplerinize dakikalar içinde geri dönüş yapan profesyonel kadro.
        </p>
      </div>

      <div class="feature-box">
        <div class="feature-box-icon">
          <i class="fa-solid fa-arrow-rotate-right"></i>
        </div>
        <h4 class="feature-box-title">Düzenli Güncellemeler</h4>
        <p class="feature-box-desc">
          Topluluktan gelen geri bildirimler doğrultusunda her hafta yeni zindanlar, görevler ve kozmetik içerikler eklenir.
        </p>
      </div>
    </div>
  </div>
</section>

<!-- EN İYİ OYUNCULAR / SIRALAMA ÖNİZLEMESİ -->
<section class="section">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Zirvedekiler</span>
      <h2 class="section-title">LİDER OYUNCULAR</h2>
      <p class="section-desc">
        GrizmMC Network arenasında gücünü ve ustalığını kanıtlayıp liderlik koltuğuna oturan şampiyonlar.
      </p>
    </div>

    <div class="leaderboard-preview">
      <div class="player-card">
        <img src="https://mc-heads.net/avatar/Notch/54" alt="Karakter" class="player-avatar">
        <div class="player-info">
          <span class="player-rank">#1 SkyBlock Ada</span>
          <strong class="player-name">VortexX</strong>
          <span class="player-stat">Seviye 4,820</span>
        </div>
      </div>

      <div class="player-card">
        <img src="https://mc-heads.net/avatar/Alex/54" alt="Karakter" class="player-avatar">
        <div class="player-info">
          <span class="player-rank">#1 PvP ELO Şampiyonu</span>
          <strong class="player-name">ShadowBlade</strong>
          <span class="player-stat">2,450 ELO</span>
        </div>
      </div>

      <div class="player-card">
        <img src="https://mc-heads.net/avatar/Steve/54" alt="Karakter" class="player-avatar">
        <div class="player-info">
          <span class="player-rank">#1 SMP En Zengin</span>
          <strong class="player-name">CryptoMiner</strong>
          <span class="player-stat">9.4M Zümrüt</span>
        </div>
      </div>

      <div class="player-card">
        <img src="https://mc-heads.net/avatar/Herobrine/54" alt="Karakter" class="player-avatar">
        <div class="player-info">
          <span class="player-rank">#1 En Çok Öldürme</span>
          <strong class="player-name">NightReaper</strong>
          <span class="player-stat">1,240 Kill</span>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- DISCORD KATILIM CTA BANNER -->
<section class="section">
  <div class="container">
    <div class="discord-cta-card">
      <div class="discord-cta-content">
        <h3>TOPLULUĞUMUZA KATIL!</h3>
        <p>
          Çekilişler, en son güncellemeler, bakım duyuruları ve binlerce aktif oyuncuyla sohbet etmek için Discord sunucumuza sen de gel.
        </p>
      </div>
      <div>
        <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="btn btn-primary btn-lg">
          <i class="fa-brands fa-discord"></i>
          <span>Discord'a Katıl</span>
        </a>
      </div>
    </div>
  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
