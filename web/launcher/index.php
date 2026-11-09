<?php
/**
 * GrizmMC Network - 3D Web Minecraft Launcher
 * Three.js destekli tarayıcı başlatıcısı ve otomatik bağlantı sistemi.
 */

$configFile = __DIR__ . '/../config.php';
$loggedInUser = null;
$serverIp = 'play-mc.grizm.tech';
$mcVersion = '1.21.4';

if (file_exists($configFile)) {
    require_once $configFile;
    if (function_exists('get_auth_username')) {
        $loggedInUser = get_auth_username();
    }
    if (defined('SERVER_IP')) {
        $serverIp = SERVER_IP;
    }
    if (defined('LAUNCHER_MC_VERSION')) {
        $mcVersion = LAUNCHER_MC_VERSION;
    }
}
?>
<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>GrizmMC Web Launcher • 3D Minecraft & Otomatik Bağlantı</title>

  <!-- SEO & Brand Meta -->
  <meta name="description" content="GrizmMC Network için Three.js destekli 3D Web Minecraft Launcher'ı. Otomatik sunucuya bağlanın veya tarayıcıda keşfedin.">
  <meta name="theme-color" content="#0a0b10">

  <!-- Google Fonts: Inter & JetBrains Mono -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=JetBrains+Mono:wght@500;700;800&display=swap" rel="stylesheet">

  <!-- FontAwesome 6 CDN -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" integrity="sha512-DTOQO9RWCH3ppGqcWaEA1BIZOC6xxalwEsw9c2QQeAIftl+Vegovlnee1c9QX4TctnWMn13TZye+giMm8e2LwA==" crossorigin="anonymous" referrerpolicy="no-referrer" />

  <!-- Three.js (r128) CDN -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>

  <!-- Launcher CSS -->
  <link rel="stylesheet" href="launcher.css">

  <script>
    // Server-Side Injected Session Details
    window.GRIZM_PHP_CONFIG = {
      username: <?php echo json_encode($loggedInUser); ?>,
      serverIp: <?php echo json_encode($serverIp); ?>,
      mcVersion: <?php echo json_encode($mcVersion); ?>
    };
  </script>
</head>
<body>
  <!-- 3D Three.js WebGL Canvas -->
  <canvas id="webgl-canvas"></canvas>

  <!-- Vignette Lighting Effect -->
  <div class="overlay-vignette"></div>

  <!-- Main Launcher UI -->
  <div id="launcher-ui">
    <!-- Top Navigation Bar -->
    <header class="top-bar">
      <div class="brand interactive">
        <div class="brand-logo">G</div>
        <div class="brand-text">
          <h1>GrizmMC</h1>
          <span>WEB LAUNCHER • <?php echo htmlspecialchars($mcVersion); ?></span>
        </div>
      </div>

      <div class="top-actions interactive">
        <!-- Live Server Status Pill -->
        <div class="server-status-pill" id="btn-copy-ip" title="IP Adresini Kopyala">
          <div class="status-indicator" id="status-indicator"></div>
          <span id="server-player-count">Sunucu Durumu Alınıyor...</span>
          <i class="fa-solid fa-copy" style="font-size: 0.75rem; color: var(--accent-cyan); margin-left: 4px;"></i>
        </div>

        <!-- User Profile Pill -->
        <div class="user-pill" id="btn-open-settings" title="Kullanıcı Ayarları">
          <img id="user-avatar-img" class="user-avatar" src="https://mc-heads.net/avatar/<?php echo urlencode($loggedInUser ?: 'GrizmOyuncu'); ?>/48" alt="Oyuncu Avatarı">
          <span class="user-name" id="user-name-display"><?php echo htmlspecialchars($loggedInUser ?: 'GrizmOyuncu'); ?></span>
        </div>

        <!-- Settings Button -->
        <button class="btn-icon" id="btn-open-settings-2" onclick="document.getElementById('modal-settings').classList.add('active'); AudioFX.playClick();" title="Ayarlar">
          <i class="fa-solid fa-gear"></i>
        </button>

        <!-- Fullscreen Button -->
        <button class="btn-icon" id="btn-fullscreen" title="Tam Ekran">
          <i class="fa-solid fa-expand"></i>
        </button>
      </div>
    </header>

    <!-- Center Hero Section -->
    <main class="center-content">
      <div class="badge-tag interactive">
        <i class="fa-solid fa-bolt"></i>
        <span>OTOMATİK BAĞLANTI SİSTEMİ AKTİF</span>
      </div>

      <h2 class="hero-title">GRIZMMC NETWORK</h2>
      <p class="hero-subtitle">
        Sıfır kurulumla Minecraft <?php echo htmlspecialchars($mcVersion); ?> ağına bağlanın. Doğrudan istemci yönlendirmesi veya tarayıcı içi 3D deneyimiyle oyuna dahil olun.
      </p>

      <!-- Auto-Connect Countdown Card -->
      <div class="auto-connect-card interactive">
        <div class="auto-connect-header">
          <div class="auto-title">
            <i class="fa-solid fa-circle-nodes" style="color: var(--accent-green);"></i>
            <span>Hedef: <strong id="target-server-label">Ana Ağ (Proxy - Port: 25565)</strong></span>
          </div>
          <div class="auto-timer" id="auto-timer-val">5s</div>
        </div>

        <div class="progress-track">
          <div class="progress-fill" id="progress-fill"></div>
        </div>

        <div class="auto-controls">
          <button class="btn-primary" id="btn-connect-now">
            <i class="fa-solid fa-play"></i>
            <span>HEMEN BAĞLAN</span>
          </button>
          <button class="btn-secondary" id="btn-pause-auto">
            <i class="fa-solid fa-pause"></i>
            <span>Durdur</span>
          </button>
        </div>
      </div>

      <!-- Quick Action Buttons -->
      <div class="quick-actions interactive">
        <a href="play.html" class="chip-btn" style="text-decoration: none; background: rgba(0, 255, 136, 0.15); border-color: var(--accent-green); color: #00ff88; font-weight: 700;">
          <i class="fa-solid fa-gamepad" style="color: var(--accent-green);"></i>
          <span>Tarayıcıda Gerçek Minecraft'ı Başlat (EaglercraftX)</span>
        </a>
        <button class="chip-btn" id="btn-enter-3d">
          <i class="fa-solid fa-cube" style="color: var(--accent-purple);"></i>
          <span>Three.js 3D Keşfet (Voxel Modu)</span>
        </button>
        <button class="chip-btn" onclick="copyServerIP(true)">
          <i class="fa-solid fa-clipboard" style="color: var(--accent-cyan);"></i>
          <span>IP: <?php echo htmlspecialchars($serverIp); ?></span>
        </button>
        <a href="../downloads/GrizmMcLauncher_Setup_v1.0.1.exe" class="chip-btn" style="text-decoration: none;" download>
          <i class="fa-brands fa-windows" style="color: #fff;"></i>
          <span>Masaüstü Launcher (.EXE)</span>
        </a>
      </div>
    </main>

    <!-- Bottom Bar / Subserver Selector -->
    <footer class="bottom-bar">
      <div class="server-selector-container interactive">
        <!-- Subserver 1: Proxy -->
        <div class="server-card active" data-server-id="proxy">
          <div class="server-header">
            <div class="server-icon"><i class="fa-solid fa-network-wired"></i></div>
            <span class="server-name">Ana Ağ (Proxy)</span>
          </div>
          <span class="server-desc">Merkezi Giriş ve Yönlendirme</span>
          <span class="server-badge">25565</span>
        </div>

        <!-- Subserver 2: Lobby -->
        <div class="server-card" data-server-id="lobby">
          <div class="server-header">
            <div class="server-icon"><i class="fa-solid fa-landmark"></i></div>
            <span class="server-name">Merkez Lobi</span>
          </div>
          <span class="server-desc">Lobi, GrizmAuth, NPC'ler</span>
          <span class="server-badge">25566</span>
        </div>

        <!-- Subserver 3: Towny -->
        <div class="server-card" data-server-id="towny">
          <div class="server-header">
            <div class="server-icon"><i class="fa-solid fa-shield-halved"></i></div>
            <span class="server-name">Grizm Towny</span>
          </div>
          <span class="server-desc">Kasaba, Kuşatma & Ekonomi</span>
          <span class="server-badge">25567</span>
        </div>

        <!-- Subserver 4: Skyblock -->
        <div class="server-card" data-server-id="skyblock">
          <div class="server-header">
            <div class="server-icon"><i class="fa-solid fa-cloud"></i></div>
            <span class="server-name">Skyblock Void</span>
          </div>
          <span class="server-desc">Özel Void Adalar & Ada Bankası</span>
          <span class="server-badge">25568</span>
        </div>

        <!-- Subserver 5: PvP Duels -->
        <div class="server-card" data-server-id="pvp">
          <div class="server-header">
            <div class="server-icon"><i class="fa-solid fa-swords"></i></div>
            <span class="server-name">PvP Duels</span>
          </div>
          <span class="server-desc">1v1 Sıralamalı Arenalar</span>
          <span class="server-badge">25569</span>
        </div>
      </div>

      <!-- 3D Explorer Callout -->
      <div class="mode-toggle-card interactive">
        <div class="mode-info">
          <h4>Web 3D Minecraft</h4>
          <p>Three.js voxel dünyasını tarayıcıdan gezin</p>
        </div>
        <button class="btn-mode" id="btn-enter-3d-alt" onclick="enterInBrowserPlayMode()">
          <i class="fa-solid fa-gamepad"></i>
          <span>Oyna</span>
        </button>
      </div>
    </footer>
  </div>

  <!-- In-Browser 3D Game HUD (When in Voxel Explorer Mode) -->
  <div id="game-hud">
    <div id="crosshair"></div>

    <div id="hud-controls-help">
      <div><kbd>W</kbd><kbd>A</kbd><kbd>S</kbd><kbd>D</kbd> : Hareket</div>
      <div><kbd>Boşluk</kbd> : Zıplama</div>
      <div><kbd>Sol Tık</kbd> : Blok Kır</div>
      <div><kbd>Sağ Tık</kbd> : Blok Yerleştir</div>
      <div><kbd>1-5</kbd> : Blok Seç</div>
    </div>

    <button id="btn-exit-game">
      <i class="fa-solid fa-arrow-left"></i>
      <span>Launcher'a Dön (ESC)</span>
    </button>

    <div id="hotbar">
      <div class="hotbar-slot selected" onclick="selectHotbarSlot(1)">
        <span class="slot-key">1</span>
        <div class="slot-block-preview" style="background: #4c962b;"></div>
      </div>
      <div class="hotbar-slot" onclick="selectHotbarSlot(2)">
        <span class="slot-key">2</span>
        <div class="slot-block-preview" style="background: #787878;"></div>
      </div>
      <div class="hotbar-slot" onclick="selectHotbarSlot(3)">
        <span class="slot-key">3</span>
        <div class="slot-block-preview" style="background: #866043;"></div>
      </div>
      <div class="hotbar-slot" onclick="selectHotbarSlot(4)">
        <span class="slot-key">4</span>
        <div class="slot-block-preview" style="background: #4dedf4;"></div>
      </div>
      <div class="hotbar-slot" onclick="selectHotbarSlot(5)">
        <span class="slot-key">5</span>
        <div class="slot-block-preview" style="background: #100c1c; border: 1px solid #7815b3;"></div>
      </div>
    </div>
  </div>

  <!-- Modal: Settings -->
  <div class="modal-overlay" id="modal-settings">
    <div class="modal-card">
      <div class="modal-header">
        <h3><i class="fa-solid fa-sliders" style="color: var(--accent-green); margin-right: 8px;"></i>Launcher Ayarları</h3>
        <button class="btn-close" id="btn-close-settings">&times;</button>
      </div>

      <div class="form-group">
        <label>Oyuncu Adı (Minecraft / GrizmID)</label>
        <input type="text" id="setting-username" class="form-input" placeholder="Kullanıcı Adı Giriniz" value="<?php echo htmlspecialchars($loggedInUser ?: 'GrizmOyuncu'); ?>">
      </div>

      <div class="form-group">
        <label>Minecraft İstemci Sürümü</label>
        <select class="form-select" id="setting-version">
          <option value="1.21.4" selected>Minecraft 1.21.4 (Önerilen)</option>
          <option value="1.21.1">Minecraft 1.21.1</option>
          <option value="1.20.4">Minecraft 1.20.4</option>
        </select>
      </div>

      <div class="form-group">
        <label>Tahsis Edilen RAM (Masaüstü)</label>
        <select class="form-select" id="setting-ram">
          <option value="4">4 GB RAM (Standart)</option>
          <option value="6" selected>6 GB RAM (Önerilen)</option>
          <option value="8">8 GB RAM (Yüksek Performans)</option>
        </select>
      </div>

      <button class="btn-primary" id="btn-save-settings" style="width: 100%; margin-top: 10px;">
        <i class="fa-solid fa-floppy-disk"></i>
        <span>Kaydet ve Kapat</span>
      </button>
    </div>
  </div>

  <!-- Modal: Connected / Handshake -->
  <div class="modal-overlay" id="modal-connected">
    <div class="modal-card" style="text-align: center;">
      <div style="width: 60px; height: 60px; background: rgba(0, 255, 136, 0.15); border: 1px solid var(--accent-green); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px auto;">
        <i class="fa-solid fa-link" style="color: var(--accent-green); font-size: 1.5rem;"></i>
      </div>

      <h3 style="font-size: 1.3rem; margin-bottom: 8px;">Sunucuya Bağlanılıyor!</h3>
      <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 20px;">
        Minecraft istemciniz açılıyor ve <strong id="modal-conn-address" style="color: #fff;"><?php echo htmlspecialchars($serverIp); ?>:25565</strong> adresine yönlendiriliyor.
      </p>

      <div style="background: rgba(255, 255, 255, 0.05); padding: 14px; border-radius: var(--radius-sm); margin-bottom: 20px; font-family: var(--font-mono); font-size: 0.82rem;">
        <div>Kullanıcı: <span id="modal-conn-user" style="color: var(--accent-cyan);"><?php echo htmlspecialchars($loggedInUser ?: 'GrizmOyuncu'); ?></span></div>
        <div style="margin-top: 4px; color: var(--accent-green);">IP Adresi Panonuza Kopyalandı!</div>
      </div>

      <div style="display: flex; flex-direction: column; gap: 10px;">
        <button class="btn-primary" onclick="window.location.href='play.html'" style="background: linear-gradient(135deg, #00ff88, #00b4d8);">
          <i class="fa-solid fa-gamepad"></i>
          <span>Tarayıcıda Gerçek Minecraft'ı Aç (Web Client)</span>
        </button>
        <button class="btn-secondary" onclick="window.location.href='minecraft://<?php echo htmlspecialchars($serverIp); ?>:25565'">
          <i class="fa-solid fa-cube"></i>
          <span>Masaüstü Minecraft İstemcisini Aç</span>
        </button>
        <button class="btn-secondary" id="btn-close-connected" style="border: none; color: var(--text-muted); font-size: 0.8rem;">
          <span>Vazgeç / Kapat</span>
        </button>
      </div>
    </div>
  </div>

  <!-- Toast Notification Container -->
  <div class="toast-notice" id="toast-notice">
    <i class="fa-solid fa-circle-check"></i>
    <span id="toast-text">İşlem Başarılı</span>
  </div>

  <!-- Launcher Logic Script -->
  <script src="launcher.js"></script>
</body>
</html>

