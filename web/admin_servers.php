<?php
/**
 * GrizmMC Network - Web Tabanlı Sunucu Yönetim & Konsol Paneli
 * Sadece yetkililerin PIN doğrulaması ile erişebildiği interaktif kontrol merkezi.
 */

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/server_manager.php';

// Yetkili Kontrolü
if (!is_staff_user()) {
    header('Location: login.php?redirect=admin_servers.php');
    exit;
}

$pageTitle = "Sunucu Kontrol Merkezi";
$activePage = "admin_servers";
$servers = ServerManager::getServers();
$isUnlocked = !empty($_SESSION['server_panel_unlocked']);
$systemInfo = ServerManager::getSystemResources();

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<main class="admin-panel-wrapper">
  <div class="container">
    
    <!-- Üst Başlık & Durum Barı -->
    <div class="admin-header-row">
      <div class="admin-title-group">
        <div class="admin-badge">
          <i class="fa-solid fa-server"></i>
          <span>GRIZMMC NETWORK CLOUD CONTROL</span>
        </div>
        <h1 class="admin-title">Sunucu Yönetim & Konsol Merkezi</h1>
        <p class="admin-subtitle">
          Velocity Proxy, Hub, Towny SMP, PvP ve SkyBlock sunucularının durumlarını canlı izleyin, tek tıkla yönetin ve doğrudan konsoldan komut yürütün.
        </p>
      </div>

      <div class="admin-global-actions">
        <a href="store.php?view=admin" class="btn btn-outline btn-sm" title="Rol, İzin & VIP Mağaza Yönetimi">
          <i class="fa-solid fa-crown"></i> Rol & VIP Yönetimi
        </a>
        <?php if ($isUnlocked): ?>
          <button class="btn btn-outline btn-sm" id="btnLockPanel" title="Paneli Kilitle">
            <i class="fa-solid fa-lock"></i> Paneli Kilitle
          </button>
          <button class="btn btn-primary btn-sm" id="btnStartAll" title="Tüm sunucuları sırayla başlatır">
            <i class="fa-solid fa-rocket"></i> Tümünü Başlat
          </button>
          <button class="btn btn-danger btn-sm" id="btnStopAll" title="Tüm sunucuları güvenli şekilde durdurur">
            <i class="fa-solid fa-power-off"></i> Tümünü Kapat
          </button>
        <?php else: ?>
          <button class="btn btn-primary btn-sm" onclick="showPinModal()">
            <i class="fa-solid fa-key"></i> PIN İle Kilidi Aç
          </button>
        <?php endif; ?>
      </div>
    </div>

    <!-- Sistem Kaynakları ve Canlı İstatistikler -->
    <div class="system-stats-grid">
      <div class="stat-card">
        <div class="stat-icon"><i class="fa-solid fa-microchip"></i></div>
        <div class="stat-info">
          <span class="stat-label">Sunucu CPU Yükü</span>
          <strong class="stat-value" id="sysCpuVal"><?php echo htmlspecialchars($systemInfo['cpu_usage'] ?? 'Hesaplanıyor...'); ?></strong>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon"><i class="fa-solid fa-memory"></i></div>
        <div class="stat-info">
          <span class="stat-label">Sistem Belleği (RAM)</span>
          <strong class="stat-value" id="sysRamVal"><?php echo htmlspecialchars($systemInfo['ram_usage'] ?? 'Hesaplanıyor...'); ?></strong>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon"><i class="fa-solid fa-users"></i></div>
        <div class="stat-info">
          <span class="stat-label">Ağdaki Toplam Oyuncu</span>
          <strong class="stat-value" id="sysTotalPlayers"><span class="pulse-dot"></span> Canlı...</strong>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon"><i class="fa-solid fa-shield-halved"></i></div>
        <div class="stat-info">
          <span class="stat-label">İşletim Sistemi</span>
          <strong class="stat-value"><?php echo htmlspecialchars(PHP_OS . ' (' . (ServerManager::isWindows() ? 'Windows' : 'Linux') . ')'); ?></strong>
        </div>
      </div>
    </div>

    <!-- PIN Kilit Ekranı (Panel kilitliyse gösterilir) -->
    <div id="pinLockOverlay" class="pin-lock-overlay <?php echo $isUnlocked ? 'hidden' : ''; ?>">
      <div class="pin-card">
        <div class="pin-icon">
          <i class="fa-solid fa-shield-cat"></i>
        </div>
        <h2>Güvenlik Doğrulaması</h2>
        <p>Sunucu süreçlerine ve RCON konsoluna erişmek için lütfen Yönetici PIN kodunuzu girin.</p>
        <form id="pinForm" onsubmit="handlePinSubmit(event)">
          <div class="pin-input-wrap">
            <input type="password" id="adminPinInput" placeholder="PIN Kodu Girin" required autocomplete="off" autofocus>
            <button type="submit" class="btn btn-primary" id="btnPinSubmit">
              <i class="fa-solid fa-unlock"></i> Kilidi Aç
            </button>
          </div>
          <div id="pinErrorMsg" class="pin-error-msg"></div>
        </form>
        <span class="pin-hint"><i class="fa-solid fa-info-circle"></i> Varsayılan PIN: <code>Grizm2026!</code> (config.php içerisinden değiştirilebilir)</span>
      </div>
    </div>

    <!-- Panel Ana Gövdesi (Kilit Açılınca Kullanılabilir) -->
    <div id="adminPanelContent" class="<?php echo !$isUnlocked ? 'blurred-content' : ''; ?>">
      
      <!-- Sunucular Listesi / Kartlar -->
      <div class="section-heading-sm">
        <h3><i class="fa-solid fa-cubes-stacked"></i> Ağ Sunucuları</h3>
        <span class="auto-refresh-pill">
          <span class="live-indicator"></span> Otomatik Güncelleme (3sn)
        </span>
      </div>

      <div class="server-cards-grid">
        <?php foreach ($servers as $key => $s): ?>
          <div class="srv-card" id="srv-card-<?php echo $key; ?>" data-server-key="<?php echo $key; ?>">
            <div class="srv-card-top">
              <div class="srv-info">
                <span class="srv-badge"><?php echo htmlspecialchars($s['badge']); ?></span>
                <h4 class="srv-name"><?php echo htmlspecialchars($s['name']); ?></h4>
                <div class="srv-ports">
                  <span>Port: <strong><?php echo $s['port']; ?></strong></span>
                  <?php if (!empty($s['rcon_port'])): ?>
                    <span class="badge-rcon" title="RCON Etkin"><i class="fa-solid fa-terminal"></i> RCON: <?php echo $s['rcon_port']; ?></span>
                  <?php endif; ?>
                </div>
              </div>
              <div class="srv-status-badge status-checking" id="badge-status-<?php echo $key; ?>">
                <span class="status-dot"></span>
                <span class="status-text">Kontrol Ediliyor...</span>
              </div>
            </div>

            <div class="srv-card-body">
              <p class="srv-desc"><?php echo htmlspecialchars($s['description']); ?></p>
              
              <div class="srv-meta-stats">
                <div class="meta-item">
                  <i class="fa-solid fa-user-group"></i>
                  <span>Oyuncular: <strong id="players-<?php echo $key; ?>">- / -</strong></span>
                </div>
                <div class="meta-item">
                  <i class="fa-solid fa-code-branch"></i>
                  <span>Sürüm: <strong id="version-<?php echo $key; ?>"><?php echo htmlspecialchars($s['type'] === 'velocity' ? 'Velocity 3.x' : '1.21.4'); ?></strong></span>
                </div>
              </div>
            </div>

            <div class="srv-card-actions">
              <button class="btn btn-success btn-xs btn-srv-start" onclick="controlServer('start', '<?php echo $key; ?>')" title="Sunucuyu Başlat">
                <i class="fa-solid fa-play"></i> Başlat
              </button>
              <button class="btn btn-warning btn-xs btn-srv-restart" onclick="controlServer('restart', '<?php echo $key; ?>')" title="Yeniden Başlat">
                <i class="fa-solid fa-rotate-right"></i> Yeniden Başlat
              </button>
              <button class="btn btn-danger btn-xs btn-srv-stop" onclick="controlServer('stop', '<?php echo $key; ?>')" title="Durdur">
                <i class="fa-solid fa-stop"></i> Durdur
              </button>
              <button class="btn btn-outline btn-xs" onclick="selectConsoleTab('<?php echo $key; ?>')" title="Konsolu Aç">
                <i class="fa-solid fa-terminal"></i> Konsol
              </button>
            </div>
          </div>
        <?php endforeach; ?>
      </div>

      <!-- Canlı Web Terminal & Konsol Bölümü -->
      <div class="console-section" id="consoleSection">
        <div class="console-header">
          <div class="console-title-group">
            <i class="fa-solid fa-terminal"></i>
            <span>Canlı Konsol & Komut Yürütücü</span>
          </div>

          <!-- Sunucu Seçme Sekmeleri -->
          <div class="console-tabs" id="consoleTabs">
            <?php foreach ($servers as $key => $s): ?>
              <button class="console-tab-btn <?php echo $key === 'smp' ? 'active' : ''; ?>" onclick="selectConsoleTab('<?php echo $key; ?>')" data-key="<?php echo $key; ?>">
                <span class="tab-indicator" id="tab-indicator-<?php echo $key; ?>"></span>
                <span><?php echo htmlspecialchars($s['name']); ?></span>
              </button>
            <?php endforeach; ?>
          </div>

          <div class="console-tools">
            <button class="btn-tool" onclick="fetchConsoleLogs()" title="Logları Yenile"><i class="fa-solid fa-rotate"></i></button>
            <button class="btn-tool" onclick="clearTerminalScreen()" title="Ekranı Temizle"><i class="fa-solid fa-broom"></i></button>
            <label class="auto-scroll-toggle" title="Yeni log geldiğinde otomatik aşağı kaydır">
              <input type="checkbox" id="chkAutoScroll" checked> Otomatik Kaydır
            </label>
          </div>
        </div>

        <!-- Terminal Gövdesi -->
        <div class="terminal-body" id="terminalOutput">
          <div class="terminal-line system-line">[SİSTEM] GrizmMC Web Terminal hazır. Loglar alınıyor...</div>
        </div>

        <!-- Hızlı Komut Makroları -->
        <div class="console-quick-macros">
          <span class="macro-label">Hızlı Komutlar:</span>
          <button class="macro-btn" onclick="executeQuickMacro('tps')">tps</button>
          <button class="macro-btn" onclick="executeQuickMacro('list')">list</button>
          <button class="macro-btn" onclick="executeQuickMacro('save-all')">save-all</button>
          <button class="macro-btn" onclick="executeQuickMacro('timings report')">timings</button>
          <button class="macro-btn" onclick="executeQuickMacro('whitelist on')">whitelist on</button>
          <button class="macro-btn" onclick="executeQuickMacro('whitelist off')">whitelist off</button>
          <button class="macro-btn" onclick="executeQuickMacro('say §a[Duyuru] §fSunucuda bakim tamamlanmistir.')">duyuru gönder</button>
        </div>

        <!-- Komut Gönderme Giriş Satırı -->
        <form class="terminal-input-bar" id="terminalCmdForm" onsubmit="handleCommandSubmit(event)">
          <div class="prompt-prefix">
            <span class="prefix-user">admin@grizmmc</span>:<span class="prefix-srv" id="currentSelectedSrvName">smp</span>#
          </div>
          <input type="text" id="terminalInput" placeholder="Konsol komutu girin (Örn: say Merhaba, op Oyuncu, kick isim)..." autocomplete="off">
          <button type="submit" class="btn btn-primary" id="btnSendCmd">
            <i class="fa-solid fa-paper-plane"></i> Gönder
          </button>
        </form>
      </div>

    </div>
  </div>
</main>

<style>
/* ==========================================================================
   SUNUCU YÖNETİM MERKEZİ - ULTRA MODERN NOIR STİLLERİ
   ========================================================================== */
.admin-panel-wrapper {
  padding: 40px 0 80px 0;
  position: relative;
  z-index: 2;
}

.admin-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  gap: 20px;
  flex-wrap: wrap;
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
  margin-bottom: 12px;
}

.admin-title {
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}

.admin-subtitle {
  color: var(--text-secondary);
  font-size: 0.95rem;
  max-width: 680px;
  line-height: 1.5;
}

.admin-global-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* Sistem İstatistik Kartları */
.system-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: rgba(18, 18, 22, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  backdrop-filter: blur(10px);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.2);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: #fff;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.stat-value {
  font-family: var(--font-mono);
  font-size: 1.05rem;
  color: #fff;
}

/* PIN Kilit Ekranı */
.pin-lock-overlay {
  position: relative;
  background: rgba(14, 14, 18, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 50px 24px;
  text-align: center;
  margin-bottom: 30px;
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
}

.pin-lock-overlay.hidden {
  display: none;
}

.blurred-content {
  filter: blur(5px);
  pointer-events: none;
  user-select: none;
  opacity: 0.5;
}

.pin-card {
  max-width: 440px;
  margin: 0 auto;
}

.pin-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px auto;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
  color: #fff;
}

.pin-card h2 {
  font-size: 1.4rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.pin-card p {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin-bottom: 24px;
  line-height: 1.4;
}

.pin-input-wrap {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.pin-input-wrap input {
  flex: 1;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  padding: 12px 16px;
  color: #fff;
  font-family: var(--font-mono);
  font-size: 1.1rem;
  letter-spacing: 2px;
}

.pin-input-wrap input:focus {
  outline: none;
  border-color: #fff;
}

.pin-error-msg {
  color: #ff5555;
  font-size: 0.85rem;
  min-height: 20px;
  margin-bottom: 10px;
}

.pin-hint {
  display: inline-block;
  font-size: 0.78rem;
  color: var(--text-muted);
}

.pin-hint code {
  color: #00ff88;
  background: rgba(0, 255, 136, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

/* Bölüm Başlıkları */
.section-heading-sm {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.section-heading-sm h3 {
  font-size: 1.15rem;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
}

.auto-refresh-pill {
  font-size: 0.75rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
}

.live-indicator {
  width: 8px;
  height: 8px;
  background: #00ff88;
  border-radius: 50%;
  box-shadow: 0 0 10px #00ff88;
  animation: pulseLive 1.5s infinite;
}

@keyframes pulseLive {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

/* Sunucu Kartları Grid */
.server-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
  margin-bottom: 36px;
}

.srv-card {
  background: rgba(18, 18, 22, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.25s ease;
}

.srv-card:hover {
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}

.srv-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.srv-badge {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.5px;
}

.srv-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: #fff;
  margin: 4px 0 6px 0;
}

.srv-ports {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.badge-rcon {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.7rem;
  color: #a0a0ab;
}

.srv-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.srv-status-badge .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-online {
  background: rgba(0, 255, 136, 0.12);
  border: 1px solid rgba(0, 255, 136, 0.3);
  color: #00ff88;
}
.status-online .status-dot {
  background: #00ff88;
  box-shadow: 0 0 8px #00ff88;
}

.status-offline {
  background: rgba(255, 85, 85, 0.12);
  border: 1px solid rgba(255, 85, 85, 0.3);
  color: #ff5555;
}
.status-offline .status-dot {
  background: #ff5555;
}

.status-checking {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #aaa;
}
.status-checking .status-dot {
  background: #aaa;
}

.srv-desc {
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.4;
  margin-bottom: 14px;
}

.srv-meta-stats {
  display: flex;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.srv-meta-stats strong {
  color: #fff;
  font-family: var(--font-mono);
}

.srv-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.srv-card-actions .btn {
  flex: 1;
  font-size: 0.75rem;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

/* Canlı Konsol Penceresi */
.console-section {
  background: #09090c;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.6);
}

.console-header {
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.console-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  color: #fff;
  font-size: 0.95rem;
}

.console-tabs {
  display: flex;
  gap: 6px;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.console-tab-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.console-tab-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}

.console-tab-btn.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.tab-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff5555;
}

.tab-indicator.online {
  background: #00ff88;
}

.console-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-tool {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #ddd;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-tool:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.auto-scroll-toggle {
  font-size: 0.78rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

/* Terminal Gövdesi */
.terminal-body {
  height: 380px;
  padding: 16px;
  overflow-y: auto;
  font-family: var(--font-mono);
  font-size: 0.85rem;
  line-height: 1.5;
  background: #050507;
  color: #d1d5db;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  white-space: pre-wrap;
  word-break: break-all;
}

.terminal-line {
  margin-bottom: 2px;
}

.terminal-line.system-line {
  color: #38bdf8;
}

.terminal-line.cmd-sent {
  color: #00ff88;
  font-weight: 700;
}

.terminal-line.cmd-output {
  color: #facc15;
}

.terminal-line.error-line {
  color: #ff5555;
}

/* Makrolar */
.console-quick-macros {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.macro-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  font-weight: 700;
}

.macro-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #aaa;
  border-radius: 6px;
  padding: 4px 10px;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.macro-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}

/* Terminal Input Bar */
.terminal-input-bar {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: #09090d;
  gap: 10px;
}

.prompt-prefix {
  font-family: var(--font-mono);
  font-size: 0.85rem;
  color: var(--text-muted);
  white-space: nowrap;
}

.prefix-user {
  color: #38bdf8;
}

.prefix-srv {
  color: #00ff88;
  font-weight: 700;
}

.terminal-input-bar input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-family: var(--font-mono);
  font-size: 0.9rem;
}

.btn-xs {
  font-size: 0.75rem;
  padding: 6px 10px;
  border-radius: 6px;
}

.btn-danger {
  background: #ff5555;
  color: #fff;
  border: none;
}
.btn-danger:hover {
  background: #e04444;
}

.btn-warning {
  background: #f59e0b;
  color: #000;
  border: none;
  font-weight: 600;
}
.btn-warning:hover {
  background: #d97706;
}

.btn-success {
  background: #00ff88;
  color: #000;
  border: none;
  font-weight: 600;
}
.btn-success:hover {
  background: #00dd77;
}
</style>

<script>
// ==============================================================================
// GRIZMMC NETWORK - CLIENT-SIDE SUNUCU YÖNETİM SİSTEMİ
// ==============================================================================

let currentActiveServer = 'smp';
let isPanelUnlocked = <?php echo $isUnlocked ? 'true' : 'false'; ?>;
let refreshInterval = null;
let lastLogLinesCount = 0;

document.addEventListener('DOMContentLoaded', () => {
  if (isPanelUnlocked) {
    initServerManagement();
  }
});

function showPinModal() {
  document.getElementById('pinLockOverlay').classList.remove('hidden');
  document.getElementById('adminPinInput').focus();
}

// PIN Doğrulama
async function handlePinSubmit(e) {
  e.preventDefault();
  const pinInput = document.getElementById('adminPinInput');
  const errorMsg = document.getElementById('pinErrorMsg');
  const btnSubmit = document.getElementById('btnPinSubmit');

  errorMsg.textContent = '';
  btnSubmit.disabled = true;
  btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Doğrulanıyor...';

  try {
    const formData = new FormData();
    formData.append('pin', pinInput.value);

    const res = await fetch('api/server_control.php?action=verify_pin', {
      method: 'POST',
      body: formData
    });
    const data = await res.json();

    if (data.success) {
      isPanelUnlocked = true;
      document.getElementById('pinLockOverlay').classList.add('hidden');
      document.getElementById('adminPanelContent').classList.remove('blurred-content');
      initServerManagement();
      location.reload();
    } else {
      errorMsg.textContent = data.message || 'Geçersiz PIN!';
      pinInput.value = '';
      pinInput.focus();
    }
  } catch (err) {
    errorMsg.textContent = 'Bağlantı hatası oluştu: ' + err.message;
  } finally {
    btnSubmit.disabled = false;
    btnSubmit.innerHTML = '<i class="fa-solid fa-unlock"></i> Kilidi Aç';
  }
}

// Paneli Kilitle
document.getElementById('btnLockPanel')?.addEventListener('click', async () => {
  if (confirm('Paneli kilitlemek istediğinize emin misiniz?')) {
    await fetch('api/server_control.php?action=lock_panel');
    location.reload();
  }
});

// Yönetim Döngüsünü Başlatma
function initServerManagement() {
  fetchServerStatus();
  fetchConsoleLogs();
  
  if (refreshInterval) clearInterval(refreshInterval);
  refreshInterval = setInterval(() => {
    fetchServerStatus();
    fetchConsoleLogs(true); // sessiz log güncellemesi
  }, 3500);

  // Toplu buton dinleyicileri
  document.getElementById('btnStartAll')?.addEventListener('click', () => triggerGlobalAction('start_all', 'TÜM sunucuları başlatmak istediğinize emin misiniz?'));
  document.getElementById('btnStopAll')?.addEventListener('click', () => triggerGlobalAction('stop_all', 'DİKKAT: TÜM sunucuları kapatmak istediğinize emin misiniz?'));
}

// Durum Sorgulama
async function fetchServerStatus() {
  if (!isPanelUnlocked) return;

  try {
    const res = await fetch('api/server_control.php?action=status');
    const json = await res.json();
    if (!json.success || !json.data) return;

    const data = json.data;

    // Sistem verileri
    if (data.system) {
      document.getElementById('sysCpuVal').textContent = data.system.cpu_usage || 'Aktif';
      document.getElementById('sysRamVal').textContent = data.system.ram_usage || 'N/A';
    }
    document.getElementById('sysTotalPlayers').innerHTML = `<span class="pulse-dot"></span> ${data.total_players || 0} Oyuncu`;

    // Her sunucu kartını güncelle
    for (const [key, srv] of Object.entries(data.servers)) {
      updateServerCard(key, srv);
    }
  } catch (err) {
    console.error('Durum sorgu hatası:', err);
  }
}

// Kart Görünümünü Güncelleme
function updateServerCard(key, srv) {
  const badge = document.getElementById(`badge-status-${key}`);
  const playersEl = document.getElementById(`players-${key}`);
  const tabIndicator = document.getElementById(`tab-indicator-${key}`);
  const card = document.getElementById(`srv-card-${key}`);

  if (!badge) return;

  if (srv.is_online) {
    badge.className = 'srv-status-badge status-online';
    badge.innerHTML = '<span class="status-dot"></span><span>ÇALIŞIYOR</span>';
    playersEl.textContent = `${srv.players.online} / ${srv.players.max || 50}`;
    if (tabIndicator) tabIndicator.className = 'tab-indicator online';
    if (card) card.style.borderColor = 'rgba(0, 255, 136, 0.3)';
  } else {
    badge.className = 'srv-status-badge status-offline';
    badge.innerHTML = '<span class="status-dot"></span><span>KAPALI</span>';
    playersEl.textContent = '0 / 0';
    if (tabIndicator) tabIndicator.className = 'tab-indicator';
    if (card) card.style.borderColor = 'rgba(255, 255, 255, 0.1)';
  }
}

// Tekil Sunucu Kontrolü (start, stop, restart)
async function controlServer(action, key) {
  const actionsMap = {
    start: 'Başlatılıyor...',
    stop: 'Durduruluyor...',
    restart: 'Yeniden başlatılıyor...'
  };

  appendTerminalLine(`[SİSTEM] ${key.toUpperCase()} için '${action}' emri verildi...`, 'system-line');

  try {
    const res = await fetch(`api/server_control.php?action=${action}&server=${key}`);
    const json = await res.json();

    if (json.success) {
      appendTerminalLine(`[BAŞARILI] ${json.message}`, 'cmd-sent');
    } else {
      appendTerminalLine(`[HATA] ${json.message}`, 'error-line');
    }

    setTimeout(fetchServerStatus, 1500);
  } catch (err) {
    appendTerminalLine(`[HATA] Ağ isteği başarısız: ${err.message}`, 'error-line');
  }
}

// Toplu Eylemler (start_all, stop_all)
async function triggerGlobalAction(action, confirmMsg) {
  if (!confirm(confirmMsg)) return;

  appendTerminalLine(`[TOPLU İŞLEM] ${action.toUpperCase()} yürütülüyor...`, 'system-line');

  try {
    const res = await fetch(`api/server_control.php?action=${action}`);
    const json = await res.json();

    if (json.success) {
      appendTerminalLine(`[BİLGİ] ${json.message}`, 'cmd-sent');
    } else {
      appendTerminalLine(`[HATA] ${json.message}`, 'error-line');
    }

    setTimeout(fetchServerStatus, 2500);
  } catch (err) {
    appendTerminalLine(`[HATA] Ağ hatası: ${err.message}`, 'error-line');
  }
}

// Konsol Sekmesi Değiştirme
function selectConsoleTab(key) {
  currentActiveServer = key;
  document.getElementById('currentSelectedSrvName').textContent = key;

  document.querySelectorAll('.console-tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-key') === key);
  });

  appendTerminalLine(`[SİSTEM] Aktif konsol '${key.toUpperCase()}' olarak seçildi. Loglar yükleniyor...`, 'system-line');
  fetchConsoleLogs();
}

// Logları Çekme
async function fetchConsoleLogs(isAuto = false) {
  if (!isPanelUnlocked) return;

  try {
    const res = await fetch(`api/server_control.php?action=logs&server=${currentActiveServer}&lines=80`);
    const json = await res.json();

    if (json.success && json.logs) {
      const term = document.getElementById('terminalOutput');
      
      // Sadece yeni satırlar varsa veya manuel yenilemede ekrana dök
      if (!isAuto || json.logs.length !== lastLogLinesCount) {
        term.innerHTML = '';
        json.logs.forEach(line => {
          const div = document.createElement('div');
          div.className = 'terminal-line';
          if (line.includes('WARN')) div.className += ' cmd-output';
          if (line.includes('ERROR') || line.includes('Exception')) div.className += ' error-line';
          div.textContent = line;
          term.appendChild(div);
        });

        lastLogLinesCount = json.logs.length;

        if (document.getElementById('chkAutoScroll').checked) {
          term.scrollTop = term.scrollHeight;
        }
      }
    }
  } catch (err) {
    if (!isAuto) {
      appendTerminalLine(`[HATA] Loglar alınırken sorun: ${err.message}`, 'error-line');
    }
  }
}

// Komut Gönderme
async function handleCommandSubmit(e) {
  e.preventDefault();
  const input = document.getElementById('terminalInput');
  const cmd = input.value.trim();
  if (!cmd) return;

  appendTerminalLine(`> [${currentActiveServer}] /${cmd}`, 'cmd-sent');
  input.value = '';

  try {
    const fd = new FormData();
    fd.append('cmd', cmd);

    const res = await fetch(`api/server_control.php?action=command&server=${currentActiveServer}`, {
      method: 'POST',
      body: fd
    });
    const json = await res.json();

    if (json.success) {
      if (json.output) {
        appendTerminalLine(json.output, 'cmd-output');
      }
    } else {
      appendTerminalLine(`[HATA] ${json.message}`, 'error-line');
    }
  } catch (err) {
    appendTerminalLine(`[HATA] Komut iletilemedi: ${err.message}`, 'error-line');
  }
}

// Hızlı Makrolar
function executeQuickMacro(macroCmd) {
  document.getElementById('terminalInput').value = macroCmd;
  document.getElementById('btnSendCmd').click();
}

function clearTerminalScreen() {
  document.getElementById('terminalOutput').innerHTML = '<div class="terminal-line system-line">[SİSTEM] Terminal ekranı temizlendi.</div>';
}

function appendTerminalLine(text, className = '') {
  const term = document.getElementById('terminalOutput');
  const div = document.createElement('div');
  div.className = `terminal-line ${className}`;
  div.textContent = text;
  term.appendChild(div);

  if (document.getElementById('chkAutoScroll').checked) {
    term.scrollTop = term.scrollHeight;
  }
}
</script>

<?php require_once __DIR__ . '/includes/footer.php'; ?>

