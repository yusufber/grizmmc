<?php
// Navbar bileşeni
$activePage = $activePage ?? 'home';
$displayUsername = is_logged_in() ? get_auth_username() : null;
$authUser = is_logged_in() ? get_auth_user() : null;
$navUserPerms = ($displayUsername && function_exists('gperm_get_user')) ? gperm_get_user($displayUsername) : null;
$navPrimaryRole = ($displayUsername && function_exists('gperm_get_user_primary_role')) ? gperm_get_user_primary_role($displayUsername)['role'] : null;
$navPermSummary = ($displayUsername && function_exists('gperm_get_user_effective_summary')) ? gperm_get_user_effective_summary($displayUsername) : null;
$navEffPerms = $navPermSummary['effective_permissions'] ?? [];
$navHasWildcard = in_array('*', $navEffPerms, true);
?>
<header class="site-navbar">
  <div class="container">
    <div class="navbar-inner">
      <!-- Logo -->
      <a href="index.php" class="brand-logo">
        <div class="logo-symbol">
          <i class="fa-solid fa-cube"></i>
        </div>
        <div class="brand-text">
          <div class="brand-title">GRIZM<span>MC</span></div>
          <div class="brand-subtitle">NETWORK</div>
        </div>
      </a>

      <!-- Masaüstü Temel Navigasyon (Sade, Ferah ve Şık) -->
      <nav class="desktop-nav">
        <ul class="nav-menu">
          <li><a href="index.php" class="nav-link <?php echo ($activePage === 'home') ? 'active' : ''; ?>">Ana Sayfa</a></li>
          <li><a href="launcher.php" class="nav-link <?php echo ($activePage === 'launcher') ? 'active' : ''; ?>">Launcher</a></li>
          <li><a href="store.php" class="nav-link <?php echo ($activePage === 'store') ? 'active' : ''; ?>">Mağaza</a></li>
          <li><a href="support.php" class="nav-link <?php echo ($activePage === 'support') ? 'active' : ''; ?>">Destek</a></li>
        </ul>
      </nav>

      <!-- Sağ Aksiyon Alanı: Giriş/Profil & Hamburger Menü Butonu -->
      <div class="nav-actions">


        <!-- Yetkili Hızlı Erişim Butonları -->
        <?php if (is_staff_user()): ?>
          <a href="store.php?view=admin" class="btn btn-outline btn-sm" style="border-color: rgba(255, 255, 255, 0.35); color: #ffffff; gap: 6px; padding: 6px 12px;" title="Rol, İzin & VIP Yönetimi">
            <i class="fa-solid fa-crown"></i>
            <span class="desktop-only">Roller & VIP</span>
          </a>
          <a href="admin_servers.php" class="btn btn-outline btn-sm" style="border-color: rgba(0, 255, 136, 0.4); color: #00ff88; gap: 6px; padding: 6px 12px;" title="Sunucu Yönetim Paneli">
            <i class="fa-solid fa-server"></i>
            <span class="desktop-only">Sunucu Paneli</span>
          </a>
        <?php endif; ?>

        <!-- Kullanıcı Giriş / Profil Butonu -->
        <?php if (is_logged_in()): ?>
          <a href="store.php" class="btn btn-ghost btn-sm desktop-only" style="padding: 6px 11px; font-family: var(--font-mono); font-size: 0.78rem; gap: 6px;" title="Cüzdan Bakiyesi">
            <i class="fa-solid fa-wallet" style="color: #d4d4d8;"></i>
            <span data-wallet-balance><?php echo number_format(floatval($navUserPerms['credits'] ?? 0), 2, '.', ''); ?></span> <?php echo STORE_CURRENCY; ?>
          </a>
          <div class="user-nav-dropdown">
            <a href="profile.php" class="user-nav-btn" title="Aktif Rol: <?php echo htmlspecialchars($navPrimaryRole['name'] ?? 'Oyuncu'); ?> | Aktif İzinler: <?php echo htmlspecialchars(implode(', ', array_slice($navEffPerms, 0, 10))); ?>">
              <img 
                src="https://mc-heads.net/avatar/<?php echo urlencode($displayUsername); ?>/26" 
                alt="Avatar" 
                class="user-nav-avatar"
                onerror="this.src='https://mc-heads.net/avatar/Steve/26';"
              >
              <span class="user-nav-name"><?php echo htmlspecialchars($displayUsername); ?></span>
              <?php if ($navPrimaryRole): ?>
                <span style="font-size: 0.65rem; font-family: var(--font-mono); font-weight: 700; padding: 2px 7px; border-radius: 999px; background: rgba(255,255,255,0.08); border: 1px solid <?php echo htmlspecialchars($navPrimaryRole['badge_color'] ?? '#fff'); ?>; color: <?php echo htmlspecialchars($navPrimaryRole['badge_color'] ?? '#fff'); ?>;">
                  <?php echo htmlspecialchars($navPrimaryRole['name']); ?>
                </span>
              <?php endif; ?>
              <span class="desktop-only" style="font-size: 0.62rem; font-family: var(--font-mono); padding: 2px 6px; border-radius: 999px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.16); color: #d4d4d8;">
                <?php echo $navHasWildcard ? '★ *' : count($navEffPerms) . ' Perm'; ?>
              </span>
            </a>
            <a href="logout.php" class="user-logout-icon" title="Çıkış Yap">
              <i class="fa-solid fa-right-from-bracket"></i>
            </a>
          </div>
        <?php else: ?>
          <a href="login.php" class="btn btn-primary btn-sm login-btn">
            <i class="fa-solid fa-user-lock"></i>
            <span>Giriş Yap</span>
          </a>
        <?php endif; ?>

        <!-- Hamburger Menü Butonu (Hem Masaüstü Hem Mobilde Açılır Çekmece) -->
        <button class="hamburger-menu-btn" id="menuDrawerOpenBtn" aria-label="Menüyü Aç" title="Menüyü Aç">
          <i class="fa-solid fa-bars-staggered"></i>
        </button>
      </div>
    </div>
  </div>
</header>

<!-- HAMBURGER ÇEKMECE MENÜ (OFFCANVAS DRAWER) -->
<div class="drawer-overlay" id="siteDrawerOverlay"></div>
<aside class="site-drawer" id="siteDrawer">
  <div class="drawer-header">
    <div class="brand-logo">
      <div class="logo-symbol">
        <i class="fa-solid fa-cube"></i>
      </div>
      <div class="brand-text">
        <div class="brand-title">GRIZM<span>MC</span></div>
        <div class="brand-subtitle">NETWORK</div>
      </div>
    </div>
    <button class="drawer-close-btn" id="menuDrawerCloseBtn" aria-label="Menüyü Kapat">
      <i class="fa-solid fa-xmark"></i>
    </button>
  </div>

  <div class="drawer-body">
    <!-- Kullanıcı Durum Kartı (Çekmece İçi) -->
    <?php if (is_logged_in()): ?>
      <div class="drawer-user-card">
        <div class="drawer-user-avatar-wrap">
          <img 
            src="https://mc-heads.net/avatar/<?php echo urlencode($displayUsername); ?>/48" 
            alt="Avatar" 
            class="drawer-user-avatar"
            onerror="this.src='https://mc-heads.net/avatar/Steve/48';"
          >
        </div>
        <div class="drawer-user-info">
          <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap;">
            <strong class="drawer-user-name"><?php echo htmlspecialchars($displayUsername); ?></strong>
            <?php if ($navPrimaryRole): ?>
              <span style="font-size: 0.65rem; font-family: var(--font-mono); font-weight: 700; padding: 1px 7px; border-radius: 999px; background: rgba(255,255,255,0.08); border: 1px solid <?php echo htmlspecialchars($navPrimaryRole['badge_color'] ?? '#fff'); ?>; color: <?php echo htmlspecialchars($navPrimaryRole['badge_color'] ?? '#fff'); ?>;">
                <?php echo htmlspecialchars($navPrimaryRole['name']); ?>
              </span>
            <?php endif; ?>
          </div>
          <span class="drawer-user-email"><?php echo htmlspecialchars($authUser['email'] ?? ''); ?></span>
          <?php if (!empty($navEffPerms)): ?>
            <div style="display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px;">
              <?php foreach (array_slice($navEffPerms, 0, 4) as $dPerm): ?>
                <code style="font-family: var(--font-mono); font-size: 0.64rem; padding: 1px 5px; border-radius: 4px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.12); color: #d4d4d8;">
                  <?php echo htmlspecialchars($dPerm === '*' ? '★ Tüm İzinler (*)' : $dPerm); ?>
                </code>
              <?php endforeach; ?>
              <?php if (count($navEffPerms) > 4): ?>
                <span style="font-family: var(--font-mono); font-size: 0.64rem; color: #888;">+<?php echo count($navEffPerms) - 4; ?> perm</span>
              <?php endif; ?>
            </div>
          <?php endif; ?>
        </div>
        <div class="drawer-user-actions">
          <a href="profile.php" class="btn btn-ghost btn-sm" style="flex: 1;"><i class="fa-solid fa-id-card"></i> Profil</a>
          <a href="logout.php" class="btn btn-ghost btn-sm" style="color: #ff5555; border-color: rgba(255,85,85,0.3);"><i class="fa-solid fa-right-from-bracket"></i></a>
        </div>
      </div>
    <?php else: ?>
      <div class="drawer-login-banner">
        <div style="font-size: 1.8rem; margin-bottom: 8px;"><i class="fa-solid fa-id-badge"></i></div>
        <h4>GrizmID Hesabınız Var mı?</h4>
        <p>Giriş yaparak profilinizi özelleştirin ve mağaza ayrıcalıklarına erişin.</p>
        <a href="login.php" class="btn btn-primary btn-sm" style="width: 100%; margin-top: 12px;">
          <i class="fa-solid fa-user-lock"></i>
          <span>Giriş Yap</span>
        </a>
      </div>
    <?php endif; ?>

    <!-- Çekmece Menü Linkleri -->
    <div class="drawer-nav-section">
      <span class="drawer-section-title">Sayfalar & Bölümler</span>
      <ul class="drawer-links-list">
        <li>
          <a href="index.php" class="drawer-link <?php echo ($activePage === 'home') ? 'active' : ''; ?>">
            <div class="drawer-link-icon"><i class="fa-solid fa-house"></i></div>
            <div class="drawer-link-text">
              <strong>Ana Sayfa</strong>
              <small>Sunucu tanıtımı ve hızlı bağlantı</small>
            </div>
          </a>
        </li>
        <li>
          <a href="launcher.php" class="drawer-link <?php echo ($activePage === 'launcher') ? 'active' : ''; ?>">
            <div class="drawer-link-icon" style="color: #00ff88;"><i class="fa-solid fa-download"></i></div>
            <div class="drawer-link-text">
              <strong>Launcher İndir</strong>
              <small>Tek tıkla otomatik giriş yapan resmi istemci</small>
            </div>
          </a>
        </li>
        <li>
          <a href="index.php#modes" class="drawer-link">
            <div class="drawer-link-icon"><i class="fa-solid fa-gamepad"></i></div>
            <div class="drawer-link-text">
              <strong>Oyun Modları</strong>
              <small>SkyBlock, SMP, PvP ve Lobi modları</small>
            </div>
          </a>
        </li>
        <li>
          <a href="store.php" class="drawer-link <?php echo ($activePage === 'store') ? 'active' : ''; ?>">
            <div class="drawer-link-icon"><i class="fa-solid fa-cart-shopping"></i></div>
            <div class="drawer-link-text">
              <strong>VIP Mağazası</strong>
              <small>Rütbeler, kitler ve kozmetikler</small>
            </div>
          </a>
        </li>
        <li>
          <a href="rules.php" class="drawer-link <?php echo ($activePage === 'rules') ? 'active' : ''; ?>">
            <div class="drawer-link-icon"><i class="fa-solid fa-scroll"></i></div>
            <div class="drawer-link-text">
              <strong>Sunucu Kuralları</strong>
              <small>Topluluk standartları ve ceza listesi</small>
            </div>
          </a>
        </li>
        <?php if (is_staff_user()): ?>
        <li>
          <a href="store.php?view=admin" class="drawer-link <?php echo ($activePage === 'store' && ($_GET['view'] ?? '') === 'admin') ? 'active' : ''; ?>" style="border-left: 3px solid #ffffff;">
            <div class="drawer-link-icon" style="color: #ffffff;"><i class="fa-solid fa-crown"></i></div>
            <div class="drawer-link-text">
              <strong style="color: #ffffff;">Rol, İzin & VIP Yönetimi</strong>
              <small>GrizmPerms grupları, izinler, bakiye ve kuponlar</small>
            </div>
          </a>
        </li>
        <li>
          <a href="admin_servers.php" class="drawer-link <?php echo ($activePage === 'admin_servers') ? 'active' : ''; ?>" style="border-left: 3px solid #00ff88;">
            <div class="drawer-link-icon" style="color: #00ff88;"><i class="fa-solid fa-server"></i></div>
            <div class="drawer-link-text">
              <strong style="color: #00ff88;">Sunucu Kontrol Merkezi</strong>
              <small>Konsol, RCON, başlat/durdur yönetimi</small>
            </div>
          </a>
        </li>
        <?php endif; ?>
        <li>
          <a href="staff.php" class="drawer-link <?php echo ($activePage === 'staff') ? 'active' : ''; ?>">
            <div class="drawer-link-icon"><i class="fa-solid fa-shield-halved"></i></div>
            <div class="drawer-link-text">
              <strong>Yetkili Kadrosu</strong>
              <small>Yönetim, geliştirici ve moderatörler</small>
            </div>
          </a>
        </li>
        <li>
          <a href="support.php" class="drawer-link <?php echo ($activePage === 'support') ? 'active' : ''; ?>">
            <div class="drawer-link-icon" style="color: #58a6ff;"><i class="fa-solid fa-headset"></i></div>
            <div class="drawer-link-text">
              <strong>Destek Talebi</strong>
              <small>Hata bildirimi, mağaza ve bilet sistemi</small>
            </div>
          </a>
        </li>
        <li>
          <a href="apply.php" class="drawer-link <?php echo ($activePage === 'apply') ? 'active' : ''; ?>">
            <div class="drawer-link-icon" style="color: #3fb950;"><i class="fa-solid fa-user-plus"></i></div>
            <div class="drawer-link-text">
              <strong>Kadro Başvurusu</strong>
              <small>Rehber, moderatör, mimar & dev alımı</small>
            </div>
          </a>
        </li>
        <li>
          <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="drawer-link">
            <div class="drawer-link-icon" style="color: #5865F2;"><i class="fa-brands fa-discord"></i></div>
            <div class="drawer-link-text">
              <strong>Discord Topluluğu</strong>
              <small>Destek talebi, sohbet ve çekilişler</small>
            </div>
          </a>
        </li>
      </ul>
    </div>

    <!-- Canlı Sunucu Durumu Widget'ı -->
    <div class="drawer-server-widget">
      <div class="drawer-server-top">
        <span class="pulse-dot"></span>
        <span style="font-size: 0.82rem; font-weight: 700; color: #fff;">SUNUCU ÇEVRİMİÇİ</span>
        <span style="margin-left: auto; font-family: var(--font-mono); font-size: 0.78rem; color: #aaa;">
          <span class="server-player-count">148</span> / 500
        </span>
      </div>
      <div class="drawer-ip-box" data-copy-ip="<?php echo SERVER_IP; ?>">
        <span><?php echo SERVER_IP; ?></span>
        <i class="fa-regular fa-copy"></i>
      </div>
    </div>
  </div>

  <!-- Çekmece Alt Sosyal Medya -->
  <div class="drawer-footer">
    <div class="drawer-socials">
      <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="social-btn" aria-label="Discord"><i class="fa-brands fa-discord"></i></a>
      <a href="<?php echo YOUTUBE_URL; ?>" target="_blank" class="social-btn" aria-label="YouTube"><i class="fa-brands fa-youtube"></i></a>
      <a href="<?php echo TIKTOK_URL; ?>" target="_blank" class="social-btn" aria-label="TikTok"><i class="fa-brands fa-tiktok"></i></a>
      <a href="<?php echo INSTAGRAM_URL; ?>" target="_blank" class="social-btn" aria-label="Instagram"><i class="fa-brands fa-instagram"></i></a>
    </div>
    <div style="font-size: 0.75rem; color: var(--text-muted); text-align: center; margin-top: 10px;">
      &copy; <?php echo date('Y'); ?> <?php echo SERVER_NAME; ?>. Tüm hakları saklıdır.
    </div>
    <div style="font-size: 0.7rem; color: #666672; text-align: center; margin-top: 6px; line-height: 1.4;">
      Mojang AB ve Microsoft ile hiçbir alakamız veya resmi bağımız yoktur.
    </div>
  </div>
</aside>
