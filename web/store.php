<?php
/**
 * GrizmMC Network - Birleşik VIP Mağazası, Cüzdan & GrizmPerms Rol/İzin Yönetim Merkezi
 * Hem oyuncular için VIP Mağazası/Cüzdan/Kupon ekranını hem de yetkililer için
 * Rol, İzin Ağacı, Oyuncu Bakiye ve Sipariş Yönetim Merkezini tek dosyada barındırır.
 */

require_once __DIR__ . '/config.php';

$isLogged = is_logged_in();
$isStaff = is_staff_user();
$requestedView = $_GET['view'] ?? ($_GET['tab'] ?? 'store');
$activeView = ($isStaff && in_array($requestedView, ['admin', 'roles', 'players', 'coupons', 'orders'], true)) ? 'admin' : 'store';
$initialAdminTab = in_array($requestedView, ['roles', 'players', 'coupons', 'orders'], true) ? $requestedView : 'roles';

$pageTitle = ($activeView === 'admin') ? "Rol, İzin & VIP Mağaza Yönetimi" : "VIP Mağazası & Cüzdan";
$activePage = "store";

$currentMcName = $isLogged ? get_auth_username() : '';
$storeUserRecord = $isLogged ? gperm_get_user($currentMcName) : null;
$storeCredits = floatval($storeUserRecord['credits'] ?? 0);
$storePrimary = $isLogged ? gperm_get_user_primary_role($currentMcName)['role'] : null;
$storeActiveVip = $isLogged ? gperm_get_user_active_vip($currentMcName) : null;
$storePermSummary = $isLogged ? gperm_get_user_effective_summary($currentMcName) : null;

$isUnlocked = !empty($_SESSION['server_panel_unlocked']);
$syncMeta = gperm_get_sync_meta();

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<!-- MAĞAZA & ROL YÖNETİMİ ANA KAPSAYICI -->
<section class="store-hero" style="padding-bottom: 80px;">
  <div class="container">

    <?php if ($isStaff): ?>
      <!-- YETKİLİ GÖRÜNÜM GEÇİŞ SEKMELERİ (MAĞAZA VİTRİNİ <-> ROL & İZİN YÖNETİMİ) -->
      <div class="store-mode-switcher">
        <button type="button" class="store-mode-btn <?php echo $activeView === 'store' ? 'active' : ''; ?>" onclick="switchStoreMainView('store')">
          <i class="fa-solid fa-bag-shopping"></i>
          <span>VIP Mağazası & Cüzdan</span>
        </button>
        <button type="button" class="store-mode-btn <?php echo $activeView === 'admin' ? 'active' : ''; ?>" onclick="switchStoreMainView('admin')">
          <i class="fa-solid fa-crown"></i>
          <span>Rol, İzin & Mağaza Yönetimi</span>
          <span class="store-mode-pill">GrizmPerms</span>
        </button>
      </div>
    <?php endif; ?>

    <!-- ======================================================================
         BÖLÜM 1: OYUNCU VIP MAĞAZASI, CÜZDAN & AKTİF ROL/İZİN ÖZETİ
         ====================================================================== -->
    <div id="storeCustomerView" style="display: <?php echo $activeView === 'store' ? 'block' : 'none'; ?>;">
      <div class="section-header">
        <span class="section-tag">Ayrıcalıklar & Destek</span>
        <h1 class="section-title">VIP MAĞAZASI</h1>
        <p class="section-desc">
          Sunucu masraflarımıza destek olarak özel komutlar, kozmetikler ve oyun içi ayrıcalıklar elde edin. Tüm alımlar <strong>GrizmPerms</strong> altyapısıyla tüm sunucularda (TAB listesi, karakter üstü isim ve sohbet dahil) saniyeler içinde otomatik teslim edilir.
        </p>
      </div>

      <!-- KULLANICI CÜZDAN, AKTİF RÜTBE, İZİNLER (PERMLER) VE KUPON ÇUBUĞU -->
      <div class="feature-box" style="padding: 24px 28px; margin-bottom: 36px; border: 1px solid var(--border-medium); background: linear-gradient(180deg, rgba(255,255,255,0.035) 0%, rgba(14,14,17,0.95) 100%);">
        <?php if ($isLogged): ?>
          <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 20px;">
            <!-- Sol: Oyuncu & Aktif Rütbe Bilgisi -->
            <div style="display: flex; align-items: center; gap: 16px;">
              <img
                src="https://mc-heads.net/avatar/<?php echo urlencode($currentMcName); ?>/52"
                alt="Avatar"
                style="width: 52px; height: 52px; border-radius: var(--radius-sm); border: 1px solid var(--border-medium); image-rendering: pixelated;"
                onerror="this.src='https://mc-heads.net/avatar/Steve/52';"
              >
              <div>
                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                  <strong style="color: #fff; font-size: 1.08rem;"><?php echo htmlspecialchars($currentMcName); ?></strong>
                  <?php if ($storePrimary): ?>
                    <span id="store-active-role-badge" style="font-family: var(--font-mono); font-size: 0.73rem; font-weight: 700; padding: 2px 10px; border-radius: 999px; border: 1px solid <?php echo htmlspecialchars($storePrimary['badge_color'] ?? '#fff'); ?>; color: <?php echo htmlspecialchars($storePrimary['badge_color'] ?? '#fff'); ?>; background: rgba(255,255,255,0.06);">
                      <i class="fa-solid fa-shield-halved" style="font-size: 0.65rem; margin-right: 3px;"></i><?php echo htmlspecialchars($storePrimary['name']); ?>
                    </span>
                  <?php endif; ?>
                  <?php if ($storePermSummary && !empty($storePermSummary['assigned_roles'])): ?>
                    <?php foreach ($storePermSummary['assigned_roles'] as $assignedRoleItem):
                      if ($storePrimary && $assignedRoleItem['role']['id'] === $storePrimary['id']) continue;
                    ?>
                      <span style="font-family: var(--font-mono); font-size: 0.68rem; padding: 2px 8px; border-radius: 999px; border: 1px solid rgba(255,255,255,0.22); color: #d4d4d8; background: rgba(255,255,255,0.03);">
                        +<?php echo htmlspecialchars($assignedRoleItem['role']['name']); ?>
                      </span>
                    <?php endforeach; ?>
                  <?php endif; ?>
                </div>
                <div id="store-vip-status-line" style="font-size: 0.82rem; color: var(--text-secondary); margin-top: 5px;">
                  <?php if ($storeActiveVip): ?>
                    <i class="fa-solid fa-crown" style="color: #facc15;"></i>
                    Aktif VIP: <strong style="color: #fff;"><?php echo htmlspecialchars($storeActiveVip['role']['name']); ?></strong>
                    &bull; Kalan Süre: <span style="font-family: var(--font-mono); color: #fff;"><?php echo htmlspecialchars(gperm_format_remaining($storeActiveVip['expires_at'])); ?></span>
                  <?php else: ?>
                    <span>Aktif süreli VIP üyeliğiniz bulunmuyor. Aşağıdan dilediğiniz paketi anında aktifleştirebilirsiniz.</span>
                  <?php endif; ?>
                </div>
              </div>
            </div>

            <!-- Orta: Cüzdan Bakiyesi & Kupon -->
            <div style="display: flex; align-items: center; gap: 18px; flex-wrap: wrap;">
              <div style="padding: 10px 18px; border-radius: var(--radius-sm); background: rgba(255,255,255,0.04); border: 1px solid var(--border-subtle); text-align: right;">
                <div style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); font-family: var(--font-mono);">Cüzdan Bakiyesi</div>
                <div style="font-size: 1.3rem; font-weight: 800; color: #fff; font-family: var(--font-mono);">
                  <span data-wallet-balance><?php echo number_format($storeCredits, 2, '.', ''); ?></span> <?php echo STORE_CURRENCY; ?>
                </div>
              </div>

              <!-- Sağ: Hızlı Kupon / Bakiye Kodu Formu -->
              <form id="store-coupon-form" style="display: flex; align-items: center; gap: 8px;">
                <input
                  type="text"
                  id="store-coupon-input"
                  class="form-input"
                  placeholder="Hediye / Bakiye Kodu"
                  style="width: 185px; padding: 10px 12px; font-family: var(--font-mono); font-size: 0.82rem; text-transform: uppercase;"
                  autocomplete="off"
                >
                <button type="submit" class="btn btn-ghost btn-sm" style="padding: 10px 14px; white-space: nowrap;">
                  <i class="fa-solid fa-ticket"></i>
                  <span>Kodu Kullan</span>
                </button>
              </form>
            </div>
          </div>

          <!-- Oyuncunun Aktif Rol & İzinleri (Permler) Özeti -->
          <?php if ($storePermSummary):
            $effPerms = $storePermSummary['effective_permissions'] ?? [];
            $inhRoles = $storePermSummary['inherited_roles'] ?? [];
          ?>
            <div style="margin-top: 18px; padding-top: 14px; border-top: 1px solid rgba(255,255,255,0.08); display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
              <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                <span style="font-family: var(--font-mono); font-size: 0.72rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em;">
                  <i class="fa-solid fa-key" style="color: #fff; margin-right: 4px;"></i>Aktif Rol & İzinleriniz (<?php echo count($effPerms); ?> Perm):
                </span>
                <?php foreach ($inhRoles as $ir): ?>
                  <span style="font-family: var(--font-mono); font-size: 0.7rem; padding: 2px 8px; border-radius: 6px; background: rgba(255,255,255,0.07); border: 1px solid <?php echo htmlspecialchars($ir['badge_color'] ?? '#fff'); ?>; color: #fff;">
                    Rol: <?php echo htmlspecialchars($ir['name']); ?>
                  </span>
                <?php endforeach; ?>
                <?php
                  $maxShowPerms = 8;
                  $shownPerms = array_slice($effPerms, 0, $maxShowPerms);
                  foreach ($shownPerms as $permNode):
                ?>
                  <code style="font-family: var(--font-mono); font-size: 0.71rem; padding: 2px 7px; border-radius: 5px; background: rgba(0,0,0,0.45); border: 1px solid rgba(255,255,255,0.12); color: #e4e4e7;">
                    <?php echo htmlspecialchars($permNode === '*' ? '★ Tüm İzinler (*)' : $permNode); ?>
                  </code>
                <?php endforeach; ?>
                <?php if (count($effPerms) > $maxShowPerms): ?>
                  <a href="profile.php" style="font-family: var(--font-mono); font-size: 0.71rem; padding: 2px 8px; border-radius: 5px; background: rgba(255,255,255,0.08); color: #fff; text-decoration: none;">
                    +<?php echo count($effPerms) - $maxShowPerms; ?> izin daha (Tümünü Gör)
                  </a>
                <?php endif; ?>
              </div>
              <div style="font-family: var(--font-mono); font-size: 0.72rem; color: var(--text-muted);">
                Oyun İçi Görünüm: <span style="color: #fff; background: rgba(0,0,0,0.5); padding: 3px 8px; border-radius: 5px; border: 1px solid rgba(255,255,255,0.1);">[<?php echo htmlspecialchars($storePrimary['name'] ?? 'Oyuncu'); ?>] <?php echo htmlspecialchars($currentMcName); ?></span>
              </div>
            </div>
          <?php endif; ?>

        <?php else: ?>
          <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px;">
            <div>
              <strong style="color: #fff; font-size: 1rem;"><i class="fa-solid fa-wallet"></i> Otomatik VIP Teslimatı İçin Giriş Yapın</strong>
              <p style="font-size: 0.85rem; color: var(--text-secondary); margin: 4px 0 0;">
                GrizmID hesabınızla giriş yaparak cüzdan bakiyenizi, aktif rol/perm ağacınızı görüntüleyebilir, kupon kodu kullanabilir ve tek tıkla VIP satın alabilirsiniz.
              </p>
            </div>
            <a href="login.php" class="btn btn-primary btn-sm">
              <i class="fa-solid fa-user-lock"></i>
              <span>Giriş Yap</span>
            </a>
          </div>
        <?php endif; ?>
      </div>

      <!-- VIP PAKETLERİ GRID -->
      <div class="vip-grid">
        <?php foreach ($VIP_PACKAGES as $vip):
          $isCurrentVip = ($storeActiveVip && strtolower($storeActiveVip['role']['id']) === strtolower($vip['id']));
        ?>
          <div class="vip-card <?php echo !empty($vip['highlight']) ? 'featured' : ''; ?>">
            <?php if (!empty($vip['highlight'])): ?>
              <div class="featured-ribbon"><?php echo htmlspecialchars($vip['badge']); ?></div>
            <?php endif; ?>

            <div>
              <div class="vip-header">
                <span class="vip-tier-badge"><?php echo htmlspecialchars($vip['tier']); ?> tier</span>
                <h3 class="vip-name"><?php echo htmlspecialchars($vip['name']); ?></h3>
                <div class="vip-pricing">
                  <span class="vip-currency"><?php echo STORE_CURRENCY; ?></span>
                  <span class="vip-amount"><?php echo htmlspecialchars($vip['price']); ?></span>
                  <span class="vip-period">/ <?php echo htmlspecialchars($vip['duration']); ?></span>
                </div>
              </div>

              <ul class="vip-perks-list">
                <?php foreach ($vip['perks'] as $perk): ?>
                  <li>
                    <i class="fa-solid fa-check"></i>
                    <span><?php echo htmlspecialchars($perk); ?></span>
                  </li>
                <?php endforeach; ?>
              </ul>
            </div>

            <button 
              class="btn <?php echo !empty($vip['highlight']) ? 'btn-primary' : 'btn-ghost'; ?> btn-sm"
              style="width: 100%;"
              data-buy-vip="true"
              data-role-id="<?php echo htmlspecialchars($vip['id']); ?>"
              data-package-name="<?php echo htmlspecialchars($vip['name']); ?>"
              data-package-price="<?php echo htmlspecialchars($vip['price'] . ' ' . STORE_CURRENCY); ?>"
              data-package-price-num="<?php echo htmlspecialchars($vip['price_num'] ?? floatval($vip['price'])); ?>"
              data-package-duration="<?php echo htmlspecialchars($vip['duration']); ?>"
            >
              <i class="fa-solid <?php echo $isCurrentVip ? 'fa-clock-rotate-left' : 'fa-cart-shopping'; ?>"></i>
              <span><?php echo $isCurrentVip ? 'Süreyi Uzat (+30 Gün)' : 'Satın Al'; ?></span>
            </button>
          </div>
        <?php endforeach; ?>
      </div>

      <!-- ======================================================================
           KASA & ANAHTAR MARKETİ (CRATE KEYS STORE - 6 KADEME)
           ====================================================================== -->
      <div id="crate-keys-section" style="margin-top: 60px;">
        <div class="section-header" style="text-align: left; margin-bottom: 28px; display: flex; align-items: flex-end; justify-content: space-between; flex-wrap: wrap; gap: 16px;">
          <div>
            <span class="section-tag" style="background: rgba(234, 179, 8, 0.1); border-color: rgba(234, 179, 8, 0.3); color: #facc15;">
              <i class="fa-solid fa-key" style="margin-right: 5px;"></i>ÖZEL ŞANS SANDIKLARI
            </span>
            <h2 class="section-title" style="font-size: 2.1rem; margin-top: 8px;">KASA & ANAHTAR MARKETİ</h2>
            <p class="section-desc" style="max-width: 680px; margin: 6px 0 0;">
              Oyun içi kasaları açarak efsanevi eşyalar, netherite setler ve süper nadir ödüller kazanın. <strong>Çoklu paket alımlarında %20'ye varan indirim!</strong> Satın aldığınız anda anahtarlarınız GrizmPerms güvencesiyle oyun içi envanterinize otomatik teslim edilir.
            </p>
          </div>
          <div style="font-family: var(--font-mono); font-size: 0.76rem; color: var(--text-muted); background: rgba(255,255,255,0.03); border: 1px solid var(--border-subtle); padding: 8px 14px; border-radius: 8px;">
            <i class="fa-solid fa-bolt" style="color: #facc15; margin-right: 5px;"></i>Komut: <code>/anahtarver {player} {seviye} {adet}</code>
          </div>
        </div>

        <?php $crateCatalog = gperm_get_crate_keys_catalog(); ?>
        <div class="crate-grid">
          <?php foreach ($crateCatalog as $tierKey => $crate): ?>
            <div class="crate-card" data-crate-tier="<?php echo htmlspecialchars($crate['tier']); ?>" style="--crate-color: <?php echo htmlspecialchars($crate['color']); ?>; --crate-glow: <?php echo htmlspecialchars($crate['glow_color']); ?>;">
              
              <!-- Üst Rozet & Işıma Çizgisi -->
              <div class="crate-card-glow-bar"></div>
              
              <div class="crate-header">
                <div class="crate-tier-badge">
                  <i class="fa-solid fa-gem" style="font-size: 0.65rem;"></i>
                  <span><?php echo htmlspecialchars($crate['badge']); ?></span>
                </div>
                <div class="crate-icon-wrap">
                  <div class="crate-icon-aura"></div>
                  <i class="<?php echo htmlspecialchars($crate['icon']); ?>"></i>
                </div>
                <h3 class="crate-title"><?php echo htmlspecialchars($crate['name']); ?></h3>
                <span class="crate-subtitle"><?php echo $crate['subtitle']; ?></span>
              </div>

              <p class="crate-desc"><?php echo htmlspecialchars($crate['description']); ?></p>

              <!-- Öne Çıkan Ödüller -->
              <div class="crate-perks">
                <span class="crate-perks-title"><i class="fa-solid fa-cube"></i> Öne Çıkan Sandık İçeriği</span>
                <ul class="crate-perks-list">
                  <?php foreach ($crate['highlights'] as $highlight): ?>
                    <li>
                      <i class="fa-solid fa-circle-check"></i>
                      <span><?php echo htmlspecialchars($highlight); ?></span>
                    </li>
                  <?php endforeach; ?>
                </ul>
              </div>

              <!-- Paket Seçici (1x, 3x, 5x, 10x) -->
              <div class="crate-bundle-selector">
                <span class="crate-bundle-label">Paket Seçin:</span>
                <div class="crate-bundle-pills">
                  <?php foreach ($crate['bundles'] as $amt => $b): ?>
                    <button
                      type="button"
                      class="crate-bundle-btn <?php echo $amt === 1 ? 'active' : ''; ?>"
                      data-amount="<?php echo $amt; ?>"
                      data-price="<?php echo htmlspecialchars($b['price']); ?>"
                      data-discount="<?php echo htmlspecialchars($b['badge']); ?>"
                      onclick="selectCrateBundle(this, '<?php echo htmlspecialchars($crate['tier']); ?>')"
                    >
                      <span class="bundle-amount"><?php echo $amt; ?>x</span>
                      <?php if (!empty($b['badge'])): ?>
                        <span class="bundle-badge"><?php echo htmlspecialchars($b['badge']); ?></span>
                      <?php endif; ?>
                    </button>
                  <?php endforeach; ?>
                </div>
              </div>

              <!-- Fiyatlandırma Alanı -->
              <div class="crate-pricing">
                <div class="crate-price-line">
                  <span class="crate-price-currency"><?php echo STORE_CURRENCY; ?></span>
                  <span class="crate-price-val" data-crate-price-display="<?php echo htmlspecialchars($crate['tier']); ?>">
                    <?php echo number_format($crate['unit_price'], 2, '.', ''); ?>
                  </span>
                  <span class="crate-unit-hint" data-crate-unit-hint="<?php echo htmlspecialchars($crate['tier']); ?>">/ 1 Anahtar</span>
                </div>
                <div class="crate-discount-note" data-crate-discount-note="<?php echo htmlspecialchars($crate['tier']); ?>" style="display: none;"></div>
              </div>

              <!-- Satın Alma Butonu -->
              <button
                type="button"
                class="btn btn-primary crate-buy-btn"
                data-buy-crate="true"
                data-tier="<?php echo htmlspecialchars($crate['tier']); ?>"
                data-tier-name="<?php echo htmlspecialchars($crate['name']); ?>"
                data-amount="1"
                data-price="<?php echo htmlspecialchars($crate['unit_price']); ?>"
                data-color="<?php echo htmlspecialchars($crate['color']); ?>"
                onclick="openCrateCheckoutModal(this)"
              >
                <i class="fa-solid fa-key"></i>
                <span class="crate-buy-text">1 Adet Satın Al</span>
              </button>

            </div>
          <?php endforeach; ?>
        </div>
      </div>

      <!-- GÜVENLİ ÖDEME BİLGİSİ -->
      <div class="features-grid" style="margin-top: 50px;">
        <div class="feature-box">
          <div class="feature-box-icon"><i class="fa-solid fa-bolt"></i></div>
          <h4 class="feature-box-title">Anında Otomatik Teslimat</h4>
          <p class="feature-box-desc">Satın aldığınız anda <strong>GrizmPerms</strong> tüm sunucularda (Lobi, TekBlok, Towny SMP, PvP) TAB listesi, karakter üstü etiketi, izinlerinizi ve ödül paranızı otomatik tanımlar.</p>
        </div>

        <div class="feature-box">
          <div class="feature-box-icon"><i class="fa-solid fa-gift"></i></div>
          <h4 class="feature-box-title">Süre Uzatma & Arkadaşa Hediye</h4>
          <p class="feature-box-desc">Mevcut VIP paketinizin süresi bitmeden yeniden alırsanız kalan sürenizin üzerine eklenir. Dilerseniz başka bir oyuncuya hediye edebilirsiniz.</p>
        </div>

        <div class="feature-box">
          <div class="feature-box-icon"><i class="fa-solid fa-headset"></i></div>
          <h4 class="feature-box-title">Bakiye Yükleme & Canlı Destek</h4>
          <p class="feature-box-desc">Kart, Havale/EFT veya hediye kodları ile cüzdanınıza bakiye yükleyebilir; her türlü soruda <a href="support.php" style="color: #fff; text-decoration: underline;">Destek Merkezi</a> üzerinden bize ulaşabilirsiniz.</p>
        </div>
      </div>
    </div>

    <?php if ($isStaff): ?>
    <!-- ======================================================================
         BÖLÜM 2: YETKİLİ ROL, İZİN AĞACI, VIP & CÜZDAN YÖNETİM MERKEZİ
         ====================================================================== -->
    <div id="storeAdminView" style="display: <?php echo $activeView === 'admin' ? 'block' : 'none'; ?>;">

      <!-- Üst Başlık & Aksiyonlar -->
      <div class="admin-header-row">
        <div class="admin-title-group">
          <div class="admin-badge">
            <i class="fa-solid fa-crown"></i>
            <span>GRIZMPERMS CENTRAL ENGINE</span>
          </div>
          <h1 class="admin-title">Rol, İzin & VIP Mağaza Yönetim Merkezi</h1>
          <p class="admin-subtitle">
            Lobi, TekBlok SkyBlock, Towny SMP ve PvP sunucularındaki tüm rütbeleri, TAB & karakter üstü görünümü, izin ağacını, VIP paketlerini, oyuncu bakiyelerini ve promosyon kodlarını tek panelden yönetin.
          </p>
        </div>

        <div class="admin-global-actions">
          <a href="admin_servers.php" class="btn btn-outline btn-sm">
            <i class="fa-solid fa-server"></i> Sunucu Konsolu
          </a>
          <?php if ($isUnlocked): ?>
            <button class="btn btn-primary btn-sm" id="btnForceSync" title="Tüm açık Paper sunucularına RCON üzerinden anlık senkronizasyon komutu gönderir">
              <i class="fa-solid fa-rotate"></i> Tüm Sunucuları Eşitle
            </button>
            <button class="btn btn-outline btn-sm" id="btnLockPanel">
              <i class="fa-solid fa-lock"></i> Kilitle
            </button>
          <?php endif; ?>
        </div>
      </div>

      <!-- Üst Özet Kartları -->
      <div class="system-stats-grid">
        <div class="stat-card">
          <div class="stat-icon"><i class="fa-solid fa-layer-group"></i></div>
          <div class="stat-info">
            <span class="stat-label">Tanımlı Roller</span>
            <strong class="stat-value" id="statRolesCount"><?php echo count(gperm_get_roles()); ?> Grup</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon"><i class="fa-solid fa-user-shield"></i></div>
          <div class="stat-info">
            <span class="stat-label">Yetkili / VIP Kayıtları</span>
            <strong class="stat-value" id="statUsersCount"><?php echo count(gperm_get_all_user_perms(false)); ?> Oyuncu</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon"><i class="fa-solid fa-ticket"></i></div>
          <div class="stat-info">
            <span class="stat-label">Aktif Kupon Kodları</span>
            <strong class="stat-value" id="statCouponsCount"><?php echo count(gperm_get_coupons()); ?> Kod</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon"><i class="fa-solid fa-bolt"></i></div>
          <div class="stat-info">
            <span class="stat-label">Senkron Revizyonu</span>
            <strong class="stat-value" id="statRevisionVal">#<?php echo intval($syncMeta['revision'] ?? 1); ?></strong>
          </div>
        </div>
      </div>

      <!-- PIN Kilit Ekranı -->
      <div id="pinLockOverlay" class="pin-lock-overlay <?php echo $isUnlocked ? 'hidden' : ''; ?>">
        <div class="pin-card">
          <div class="pin-icon"><i class="fa-solid fa-key"></i></div>
          <h2>Yönetici PIN Doğrulaması</h2>
          <p>Rolleri, izinleri ve oyuncu bakiyelerini düzenlemek için lütfen Yönetici PIN kodunuzu girin.</p>
          <form onsubmit="handlePermPinSubmit(event)">
            <div class="pin-input-wrap">
              <input type="password" id="adminPinInput" placeholder="PIN Kodu Girin" required autocomplete="off">
              <button type="submit" class="btn btn-primary" id="btnPinSubmit">
                <i class="fa-solid fa-unlock"></i> Kilidi Aç
              </button>
            </div>
            <div id="pinErrorMsg" class="pin-error-msg"></div>
          </form>
          <span class="pin-hint"><i class="fa-solid fa-info-circle"></i> Varsayılan PIN: <code>Grizm2026!</code></span>
        </div>
      </div>

      <!-- ANA YÖNETİM PANELİ GÖVDESİ -->
      <div id="adminPanelContent" class="<?php echo !$isUnlocked ? 'blurred-content' : ''; ?>">

        <!-- Sekme Menüsü -->
        <div class="gp-tabs">
          <button class="gp-tab <?php echo $initialAdminTab === 'roles' ? 'active' : ''; ?>" data-tab="roles"><i class="fa-solid fa-sitemap"></i> Roller & İzin Ağacı</button>
          <button class="gp-tab <?php echo $initialAdminTab === 'players' ? 'active' : ''; ?>" data-tab="players"><i class="fa-solid fa-users-gear"></i> Oyuncu Yetki & Bakiye</button>
          <button class="gp-tab <?php echo $initialAdminTab === 'coupons' ? 'active' : ''; ?>" data-tab="coupons"><i class="fa-solid fa-ticket"></i> Kupon & Hediye Kodları</button>
          <button class="gp-tab <?php echo $initialAdminTab === 'orders' ? 'active' : ''; ?>" data-tab="orders"><i class="fa-solid fa-receipt"></i> Satın Alımlar & Webhook</button>
        </div>

        <!-- SEKME 1: ROLLER VE İZİNLER EDİTÖRÜ -->
        <div class="gp-pane <?php echo $initialAdminTab === 'roles' ? 'active' : ''; ?>" id="pane-roles">
          <div class="gp-roles-layout">
            <!-- Sol: Rol Listesi -->
            <div class="gp-card">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
                <h3 class="gp-card-title" style="margin: 0;">Gruplar (Ağırlık Sıralı)</h3>
                <button class="btn btn-primary btn-sm" onclick="startCreateNewRole()">
                  <i class="fa-solid fa-plus"></i> Yeni Rol
                </button>
              </div>
              <div id="rolesSidebarList" class="gp-role-list"></div>
            </div>

            <!-- Sağ: Seçili Rol Düzenleyici -->
            <div class="gp-card">
              <form id="roleEditorForm" onsubmit="saveCurrentRole(event)">
                <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; padding-bottom: 14px; border-bottom: 1px solid rgba(255,255,255,0.08);">
                  <div>
                    <span style="font-family: var(--font-mono); font-size: 0.72rem; color: var(--text-muted); text-transform: uppercase;">Seçili Grup Düzenleyici</span>
                    <h3 id="editorHeadingTitle" style="color: #fff; font-size: 1.35rem; margin-top: 2px;">Rol Seçin</h3>
                  </div>
                  <div style="display: flex; gap: 8px;">
                    <button type="button" class="btn btn-outline btn-sm" id="btnDeleteRole" style="color: #ff6b6b; border-color: rgba(255,107,107,0.35);" onclick="deleteCurrentRole()">
                      <i class="fa-solid fa-trash"></i> Sil
                    </button>
                    <button type="submit" class="btn btn-primary btn-sm">
                      <i class="fa-solid fa-floppy-disk"></i> Kaydet & Eşitle
                    </button>
                  </div>
                </div>

                <!-- Canlı Sohbet, TAB Listesi & Karakter Üstü İsim Önizlemesi -->
                <div class="gp-chat-preview-box">
                  <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                    <div>
                      <span style="font-size: 0.68rem; font-family: var(--font-mono); color: #888; text-transform: uppercase;">Oyun İçi Sohbet Önizlemesi:</span>
                      <div id="mcChatPreview" style="font-family: var(--font-mono); font-size: 0.9rem; margin-top: 5px; color: #fff;"></div>
                    </div>
                    <div>
                      <span style="font-size: 0.68rem; font-family: var(--font-mono); color: #888; text-transform: uppercase;">TAB Listesi & Karakter Üstü İsim:</span>
                      <div id="mcNametagPreview" style="font-family: var(--font-mono); font-size: 0.9rem; margin-top: 5px; color: #fff; background: rgba(255,255,255,0.04); display: inline-block; padding: 2px 10px; border-radius: 4px;"></div>
                    </div>
                  </div>
                </div>

                <div class="gp-grid-3">
                  <div class="form-group">
                    <label class="form-label">Rol Kimliği (ID)</label>
                    <input type="text" id="roleIdInput" class="form-input" placeholder="orn: mvip" required>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Görünen Ad</label>
                    <input type="text" id="roleNameInput" class="form-input" placeholder="Örn: MVIP" required>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Ağırlık / TAB Sırası (Weight)</label>
                    <input type="number" id="roleWeightInput" class="form-input" value="100" required>
                  </div>
                </div>

                <div class="gp-grid-4">
                  <div class="form-group">
                    <label class="form-label">Prefix (TAB, Baş Üstü & Sohbet)</label>
                    <input type="text" id="rolePrefixInput" class="form-input" placeholder="&b&l[VIP] &b" oninput="renderChatPreview()">
                  </div>
                  <div class="form-group">
                    <label class="form-label">Suffix (TAB, Baş Üstü & Sohbet)</label>
                    <input type="text" id="roleSuffixInput" class="form-input" placeholder=" &b❖" oninput="renderChatPreview()">
                  </div>
                  <div class="form-group">
                    <label class="form-label">Mesaj Rengi</label>
                    <input type="text" id="roleChatColorInput" class="form-input" value="&f" oninput="renderChatPreview()">
                  </div>
                  <div class="form-group">
                    <label class="form-label">Web Rozet Rengi (Hex)</label>
                    <input type="text" id="roleBadgeColorInput" class="form-input" value="#ffffff">
                  </div>
                </div>

                <!-- Miras (Inheritance) -->
                <div class="form-group">
                  <label class="form-label">Miras Alınan Roller (Alt grupların tüm izinlerini devralır)</label>
                  <div id="roleInheritanceChecks" class="gp-check-pills"></div>
                </div>

                <!-- İzinler: Global + Sunucu Bazlı -->
                <div class="gp-grid-2">
                  <div class="form-group">
                    <label class="form-label">Global İzinler (Her satıra 1 izin &bull; <code>*</code>, <code>essentials.*</code>, <code>-yasak.izin</code>)</label>
                    <textarea id="roleGlobalPerms" class="form-input gp-mono-area" rows="8" placeholder="essentials.fly&#10;essentials.heal"></textarea>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Sunucuya Özel İzin / Engel Kuralları (Adil PvP vb.)</label>
                    <div class="gp-mini-srv-grid">
                      <div>
                        <small class="gp-srv-tag">LOBBY (Hub)</small>
                        <textarea id="roleSrvLobby" class="form-input gp-mono-area" rows="2" placeholder="essentials.fly"></textarea>
                      </div>
                      <div>
                        <small class="gp-srv-tag">SKYBLOCK (TekBlok)</small>
                        <textarea id="roleSrvSkyblock" class="form-input gp-mono-area" rows="2" placeholder="grizmoneblock.vip"></textarea>
                      </div>
                      <div>
                        <small class="gp-srv-tag">SMP (Towny)</small>
                        <textarea id="roleSrvSmp" class="form-input gp-mono-area" rows="2" placeholder="grizmtowny.vip"></textarea>
                      </div>
                      <div>
                        <small class="gp-srv-tag">PVP (Adil Arena Negasyonları)</small>
                        <textarea id="roleSrvPvp" class="form-input gp-mono-area" rows="2" placeholder="-essentials.fly&#10;-essentials.heal"></textarea>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- VIP Mağaza Ayarları -->
                <div class="gp-vip-settings-box">
                  <div style="display: flex; align-items: center; gap: 20px; flex-wrap: wrap; margin-bottom: 12px;">
                    <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; color: #fff; font-weight: 600;">
                      <input type="checkbox" id="roleIsVipCheck" onchange="toggleVipFields()">
                      <span>VIP Mağazasında Satışa Açık Paket</span>
                    </label>
                    <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; color: var(--text-secondary); font-size: 0.85rem;">
                      <input type="checkbox" id="roleVipHighlightCheck">
                      <span>"EN POPÜLER" Olarak Öne Çıkar</span>
                    </label>
                    <label style="display: flex; align-items: center; gap: 8px; cursor: pointer; color: var(--text-secondary); font-size: 0.85rem;">
                      <input type="checkbox" id="roleIsDefaultCheck">
                      <span>Varsayılan Oyuncu Rolü</span>
                    </label>
                  </div>

                  <div id="vipExtraFields" style="display: none;">
                    <div class="gp-grid-4">
                      <div class="form-group">
                        <label class="form-label">Mağaza Fiyatı (<?php echo STORE_CURRENCY; ?>)</label>
                        <input type="number" step="0.01" id="roleVipPriceInput" class="form-input" value="75">
                      </div>
                      <div class="form-group">
                        <label class="form-label">Paket Süresi (Gün)</label>
                        <input type="number" id="roleVipDaysInput" class="form-input" value="30">
                      </div>
                      <div class="form-group">
                        <label class="form-label">Tier Kodu</label>
                        <input type="text" id="roleVipTierInput" class="form-input" placeholder="iron / gold / diamond">
                      </div>
                      <div class="form-group">
                        <label class="form-label">Üst Rozet Yazısı</label>
                        <input type="text" id="roleVipBadgeInput" class="form-input" placeholder="EN POPÜLER">
                      </div>
                    </div>

                    <div class="gp-grid-2">
                      <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label">Mağaza Kartı Özellikleri (Her satıra 1 madde)</label>
                        <textarea id="roleVipPerksArea" class="form-input" rows="5"></textarea>
                      </div>
                      <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label">Satın Alım Anında Çalışacak Konsol Komutları (<code>sunucu:komut</code>, <code>{player}</code> destekli)</label>
                        <textarea id="roleVipCommandsArea" class="form-input gp-mono-area" rows="5" placeholder="skyblock:eco give {player} 50000&#10;smp:eco give {player} 50000"></textarea>
                      </div>
                    </div>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>

        <!-- SEKME 2: OYUNCU YETKİ, SÜRELİ VIP VE CÜZDAN YÖNETİMİ -->
        <div class="gp-pane <?php echo $initialAdminTab === 'players' ? 'active' : ''; ?>" id="pane-players">
          <div class="gp-grid-2" style="margin-bottom: 22px;">
            <!-- Hızlı Rol / VIP Tanımlama Kartı -->
            <div class="gp-card">
              <h3 class="gp-card-title"><i class="fa-solid fa-crown"></i> Oyuncuya Rol / Süreli VIP Tanımla</h3>
              <form onsubmit="handleAdminGrantRole(event)">
                <div class="gp-grid-3">
                  <div class="form-group">
                    <label class="form-label">Minecraft Oyuncu Adı</label>
                    <input type="text" id="grantUsername" class="form-input" placeholder="Örn: yusufoyndberk" required>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Verilecek Rol / VIP</label>
                    <select id="grantRoleSelect" class="form-input"></select>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Süre (Gün, 0 = Kalıcı)</label>
                    <input type="number" id="grantDaysInput" class="form-input" value="30" min="0">
                  </div>
                </div>
                <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
                  <label style="display: flex; align-items: center; gap: 8px; font-size: 0.84rem; color: var(--text-secondary); cursor: pointer;">
                    <input type="checkbox" id="grantCelebrateCheck" checked>
                    <span>Oyun içinde VIP kutlama efekti & satın alım ödül komutlarını tetikle</span>
                  </label>
                  <button type="submit" class="btn btn-primary btn-sm">
                    <i class="fa-solid fa-bolt"></i> Yetkiyi Tanımla / Uzat
                  </button>
                </div>
              </form>
            </div>

            <!-- Hızlı Cüzdan Bakiye Yönetimi Kartı -->
            <div class="gp-card">
              <h3 class="gp-card-title"><i class="fa-solid fa-wallet"></i> Oyuncu Cüzdan Bakiyesi Yönetimi (<?php echo STORE_CURRENCY; ?>)</h3>
              <form onsubmit="handleAdminCredits(event)">
                <div class="gp-grid-3">
                  <div class="form-group">
                    <label class="form-label">Minecraft Oyuncu Adı</label>
                    <input type="text" id="creditUsername" class="form-input" placeholder="Örn: yusufoyndberk" required>
                  </div>
                  <div class="form-group">
                    <label class="form-label">İşlem Türü</label>
                    <select id="creditModeSelect" class="form-input">
                      <option value="add">Bakiye Ekle / Çıkar (+/-)</option>
                      <option value="set">Bakiyeyi Sabitle (=)</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Tutar (<?php echo STORE_CURRENCY; ?>)</label>
                    <input type="number" step="0.01" id="creditAmountInput" class="form-input" value="100" required>
                  </div>
                </div>
                <div style="display: flex; justify-content: flex-end;">
                  <button type="submit" class="btn btn-primary btn-sm">
                    <i class="fa-solid fa-coins"></i> Bakiyeyi Güncelle
                  </button>
                </div>
              </form>
            </div>
          </div>

          <!-- Kayıtlı Oyuncular Tablosu -->
          <div class="gp-card">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 16px;">
              <h3 class="gp-card-title" style="margin: 0;"><i class="fa-solid fa-users"></i> Oyuncu Rol & Bakiye Listesi</h3>
              <input type="text" id="playerSearchInput" class="form-input" placeholder="Oyuncu veya e-posta ara..." style="max-width: 280px; padding: 8px 12px;" oninput="renderPlayersTable()">
            </div>
            <div style="overflow-x: auto;">
              <table class="gp-table">
                <thead>
                  <tr>
                    <th>Oyuncu</th>
                    <th>Aktif Roller & Kalan Süre</th>
                    <th>Özel İzinler</th>
                    <th>Cüzdan</th>
                    <th>Hızlı İşlem</th>
                  </tr>
                </thead>
                <tbody id="playersTableBody"></tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- SEKME 3: KUPON & HEDİYE KODLARI -->
        <div class="gp-pane <?php echo $initialAdminTab === 'coupons' ? 'active' : ''; ?>" id="pane-coupons">
          <div class="gp-grid-2">
            <div class="gp-card">
              <h3 class="gp-card-title"><i class="fa-solid fa-plus-circle"></i> Yeni Promosyon / Hediye Kodu Oluştur</h3>
              <form onsubmit="handleCreateCoupon(event)">
                <div class="gp-grid-2">
                  <div class="form-group">
                    <label class="form-label">Kupon Kodu</label>
                    <input type="text" id="cpCode" class="form-input" placeholder="Örn: YAZ2026" style="text-transform: uppercase; font-family: var(--font-mono);" required>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Ödül Tipi</label>
                    <select id="cpType" class="form-input" onchange="toggleCouponTypeFields()">
                      <option value="credit">Cüzdan Bakiyesi (<?php echo STORE_CURRENCY; ?>)</option>
                      <option value="vip">Doğrudan VIP / Rol Paketi</option>
                    </select>
                  </div>
                </div>

                <div id="cpCreditWrap" class="form-group">
                  <label class="form-label">Verilecek Bakiye Tutarı (<?php echo STORE_CURRENCY; ?>)</label>
                  <input type="number" step="0.01" id="cpAmount" class="form-input" value="50">
                </div>

                <div id="cpVipWrap" class="gp-grid-2" style="display: none;">
                  <div class="form-group">
                    <label class="form-label">Verilecek VIP / Rol</label>
                    <select id="cpRoleSelect" class="form-input"></select>
                  </div>
                  <div class="form-group">
                    <label class="form-label">VIP Süresi (Gün)</label>
                    <input type="number" id="cpVipDays" class="form-input" value="30">
                  </div>
                </div>

                <div class="gp-grid-2">
                  <div class="form-group">
                    <label class="form-label">Maksimum Kullanım Limiti (0 = Sınırsız)</label>
                    <input type="number" id="cpMaxUses" class="form-input" value="1" min="0">
                  </div>
                  <div class="form-group">
                    <label class="form-label">Son Kullanma Süresi (Gün, 0 = Süresiz)</label>
                    <input type="number" id="cpExpireDays" class="form-input" value="0" min="0">
                  </div>
                </div>

                <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">
                  <i class="fa-solid fa-ticket"></i> Kupon Kodunu Oluştur
                </button>
              </form>
            </div>

            <div class="gp-card">
              <h3 class="gp-card-title"><i class="fa-solid fa-tags"></i> Aktif Kupon Kodları</h3>
              <div style="overflow-x: auto;">
                <table class="gp-table">
                  <thead>
                    <tr>
                      <th>Kod</th>
                      <th>Ödül</th>
                      <th>Kullanım</th>
                      <th>Sil</th>
                    </tr>
                  </thead>
                  <tbody id="couponsTableBody"></tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <!-- SEKME 4: SİPARİŞ KAYITLARI & ÖDEME WEBHOOK ENTEGRASYONU -->
        <div class="gp-pane <?php echo $initialAdminTab === 'orders' ? 'active' : ''; ?>" id="pane-orders">
          <div class="gp-card" style="margin-bottom: 22px;">
            <h3 class="gp-card-title"><i class="fa-solid fa-plug"></i> Otomatik Ödeme Sağlayıcı Webhook Bağlantısı (Shopier / PayTR / İyzico)</h3>
            <p style="font-size: 0.86rem; color: var(--text-secondary); margin-bottom: 12px;">
              Harici sanal POS veya ödeme köprünüzden ödeme onaylandığında aşağıdaki uç noktaya <code>POST</code> isteği göndererek oyuncuya otomatik bakiye yükleyebilir veya doğrudan VIP tanımlayabilirsiniz:
            </p>
            <div class="gp-grid-2">
              <div style="background: rgba(0,0,0,0.45); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 12px; font-family: var(--font-mono); font-size: 0.78rem; color: #d4d4d8;">
                <div style="color: #888; margin-bottom: 4px;">Webhook URL:</div>
                <div style="color: #fff; word-break: break-all;"><?php echo htmlspecialchars(SITE_URL . '/api/perms.php?action=payment_webhook'); ?></div>
                <div style="color: #888; margin-top: 8px; margin-bottom: 4px;">Header (`X-Webhook-Secret`):</div>
                <div style="color: #fff;"><?php echo htmlspecialchars(PAYMENT_WEBHOOK_SECRET); ?></div>
              </div>
              <div style="background: rgba(0,0,0,0.45); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 12px; font-family: var(--font-mono); font-size: 0.75rem; color: #d4d4d8; white-space: pre-wrap;">{
  "order_id": "SHP-9001",
  "username": "OyuncuAdi",
  "mode": "vip",
  "role_id": "diamond-vip",
  "days": 30,
  "provider": "shopier"
}</div>
            </div>
          </div>

          <div class="gp-card">
            <h3 class="gp-card-title"><i class="fa-solid fa-clock-rotate-left"></i> Son Mağaza, VIP ve Bakiye İşlemleri</h3>
            <div style="overflow-x: auto;">
              <table class="gp-table">
                <thead>
                  <tr>
                    <th>İşlem No</th>
                    <th>Oyuncu</th>
                    <th>Tür / Paket</th>
                    <th>Tutar</th>
                    <th>Kaynak & Not</th>
                    <th>Tarih</th>
                  </tr>
                </thead>
                <tbody id="ordersTableBody"></tbody>
              </table>
            </div>
          </div>
        </div>

      </div>
    </div>
    <?php endif; ?>

  </div>
</section>

<!-- KASA & ANAHTAR SATIN ALIM MODAL PENCERESİ -->
<div class="modal-overlay" id="crate-checkout-modal" data-logged-in="<?php echo $isLogged ? '1' : '0'; ?>" data-user-credits="<?php echo htmlspecialchars(number_format($storeCredits, 2, '.', '')); ?>">
  <div class="modal-box" style="max-width: 500px; border-color: rgba(255,255,255,0.18);">
    <button class="modal-close" aria-label="Kapat" onclick="closeCrateCheckoutModal()"><i class="fa-solid fa-xmark"></i></button>

    <div class="modal-header" style="text-align: center; padding-bottom: 12px;">
      <div id="crate-modal-icon-wrap" style="width: 56px; height: 56px; margin: 0 auto 10px; border-radius: 14px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 1.6rem; color: #fff;">
        <i class="fa-solid fa-key" id="crate-modal-icon"></i>
      </div>
      <span id="crate-modal-badge" style="font-family: var(--font-mono); font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.1em; color: var(--text-muted);">KASA ANAHTARI TESLİMATI</span>
      <h3 id="crate-modal-title" style="margin-top: 4px; font-size: 1.45rem;">Sıradan Kasa Anahtarı</h3>
      <p id="crate-modal-subtitle" style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 3px;">1 Adet Anahtar Paketi</p>
    </div>

    <?php if (!$isLogged): ?>
      <div style="padding: 20px; border-radius: var(--radius-sm); background: rgba(255,255,255,0.04); border: 1px solid var(--border-medium); text-align: center; margin-bottom: 16px;">
        <i class="fa-solid fa-user-shield" style="font-size: 2rem; color: #fff; margin-bottom: 10px;"></i>
        <h4 style="color: #fff; margin-bottom: 6px;">GrizmID Oturumu Gerekli</h4>
        <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 16px;">
          Kasa anahtarlarınızın hesabınıza anında tanımlanması için lütfen giriş yapın.
        </p>
        <a href="login.php" class="btn btn-primary" style="width: 100%;">
          <i class="fa-solid fa-right-to-bracket"></i>
          <span>Giriş Yap ve Satın Al</span>
        </a>
      </div>
    <?php else: ?>
      <form id="crate-checkout-form" onsubmit="submitCratePurchase(event)">
        <input type="hidden" id="crate-checkout-tier" value="">
        <input type="hidden" id="crate-checkout-amount" value="1">
        <input type="hidden" id="crate-checkout-price-num" value="0">

        <div class="form-group">
          <label for="crate-target-username" class="form-label">Anahtarların Teslim Edileceği Minecraft Oyuncusu</label>
          <input type="text" id="crate-target-username" class="form-input" placeholder="Örn: ProGamer_34" value="<?php echo htmlspecialchars($currentMcName); ?>" required autocomplete="off">
          <small style="color: #888; font-size: 0.76rem; display: block; margin-top: 6px;">
            <i class="fa-solid fa-gift" style="color: #fff;"></i> Kendi adınız seçilidir. Başka bir arkadaşınıza <strong>hediye göndermek</strong> için oyuncu adını değiştirebilirsiniz.
          </small>
        </div>

        <!-- Cüzdan Hesap Özeti -->
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 14px 16px; margin-bottom: 16px; font-size: 0.86rem;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
            <span style="color: var(--text-secondary);">Mevcut Cüzdan Bakiyeniz:</span>
            <strong style="font-family: var(--font-mono); color: #fff;"><span data-wallet-balance><?php echo number_format($storeCredits, 2, '.', ''); ?></span> <?php echo STORE_CURRENCY; ?></strong>
          </div>
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
            <span style="color: var(--text-secondary);">Paket Tutarı (<span id="crate-summary-amount-label">1x</span>):</span>
            <strong style="font-family: var(--font-mono); color: #fff;" id="crate-summary-price">0.00 <?php echo STORE_CURRENCY; ?></strong>
          </div>
          <div style="border-top: 1px solid var(--border-subtle); padding-top: 8px; margin-top: 6px; display: flex; justify-content: space-between; align-items: center;">
            <span id="crate-checkout-after-label" style="color: var(--text-secondary);">İşlem Sonrası Bakiye:</span>
            <strong id="crate-checkout-after-balance" style="font-family: var(--font-mono); color: #fff;">0.00 <?php echo STORE_CURRENCY; ?></strong>
          </div>
        </div>

        <div id="crate-checkout-insufficient-box" style="display: none; background: rgba(255, 85, 85, 0.08); border: 1px solid rgba(255, 85, 85, 0.35); border-radius: var(--radius-sm); padding: 12px 14px; margin-bottom: 14px; font-size: 0.8rem; color: #fca5a5; line-height: 1.45;">
          <i class="fa-solid fa-triangle-exclamation"></i>
          <strong>Cüzdan bakiyeniz bu paket için yetersiz.</strong> Mağaza üst barından hediye/bakiye kodu kullanabilir veya <a href="support.php?tab=new" style="color: #fff; text-decoration: underline;">Destek & Bakiye Talebi</a> üzerinden cüzdanınıza bakiye yükleyebilirsiniz.
        </div>

        <div id="crate-checkout-feedback-msg" style="display: none; padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 14px; font-size: 0.84rem;"></div>

        <button type="submit" id="crate-checkout-submit-btn" class="btn btn-primary" style="width: 100%; margin-top: 4px;">
          <i class="fa-solid fa-bolt"></i>
          <span>Cüzdan ile Hemen Satın Al</span>
        </button>

        <div style="font-size: 0.72rem; color: #777782; text-align: center; margin-top: 14px; line-height: 1.4;">
          <i class="fa-solid fa-circle-info"></i> Satın alım onaylandığında teslimat komutu (<code>/anahtarver</code>) GrizmPerms teslimat motoru tarafından anında yürütülür.
        </div>
      </form>
    <?php endif; ?>
  </div>
</div>

<!-- SATIN ALIM MODAL PENCERESİ -->
<div class="modal-overlay" id="checkout-modal" data-logged-in="<?php echo $isLogged ? '1' : '0'; ?>" data-user-credits="<?php echo htmlspecialchars(number_format($storeCredits, 2, '.', '')); ?>">
  <div class="modal-box" style="max-width: 480px;">
    <button class="modal-close" aria-label="Kapat"><i class="fa-solid fa-xmark"></i></button>

    <div class="modal-header">
      <span style="font-family: var(--font-mono); font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.1em; color: var(--text-muted);">GrizmPerms Otomatik Teslimat</span>
      <h3 id="modal-package-title" style="margin-top: 4px;">VIP Üyelik</h3>
      <p>Paket Tutarı: <strong id="modal-package-price" style="color: #fff;">0 ₺</strong> <span id="modal-package-duration" style="color: var(--text-muted); font-size: 0.85rem;">/ 30 Gün</span></p>
    </div>

    <?php if (!$isLogged): ?>
      <div style="padding: 20px; border-radius: var(--radius-sm); background: rgba(255,255,255,0.04); border: 1px solid var(--border-medium); text-align: center; margin-bottom: 16px;">
        <i class="fa-solid fa-user-shield" style="font-size: 2rem; color: #fff; margin-bottom: 10px;"></i>
        <h4 style="color: #fff; margin-bottom: 6px;">GrizmID Oturumu Gerekli</h4>
        <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 16px;">
          Güvenliğiniz ve oyun içi anında teslimat için lütfen önce hesabınızla giriş yapın.
        </p>
        <a href="login.php" class="btn btn-primary" style="width: 100%;">
          <i class="fa-solid fa-right-to-bracket"></i>
          <span>Giriş Yap ve Satın Al</span>
        </a>
      </div>
    <?php else: ?>
      <form id="checkout-form">
        <input type="hidden" id="checkout-role-id" value="">

        <div class="form-group">
          <label for="mc-username" class="form-label">Teslim Edilecek Minecraft Oyuncu Adı</label>
          <input type="text" id="mc-username" class="form-input" placeholder="Örn: ProGamer_34" value="<?php echo htmlspecialchars($currentMcName); ?>" required autocomplete="off">
          <small style="color: #888; font-size: 0.76rem; display: block; margin-top: 6px;">
            <i class="fa-solid fa-circle-check" style="color: #fff;"></i> Kendi hesabınız otomatik yazıldı. Arkadaşınıza <strong>hediye etmek</strong> isterseniz onun kullanıcı adını yazabilirsiniz.
          </small>
        </div>

        <!-- Cüzdan Hesap Özeti -->
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 14px 16px; margin-bottom: 16px; font-size: 0.86rem;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
            <span style="color: var(--text-secondary);">Mevcut Cüzdan Bakiyeniz:</span>
            <strong style="font-family: var(--font-mono); color: #fff;"><span data-wallet-balance><?php echo number_format($storeCredits, 2, '.', ''); ?></span> <?php echo STORE_CURRENCY; ?></strong>
          </div>
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
            <span style="color: var(--text-secondary);">Paket Ücreti:</span>
            <strong style="font-family: var(--font-mono); color: #fff;" id="checkout-summary-price">0.00 <?php echo STORE_CURRENCY; ?></strong>
          </div>
          <div style="border-top: 1px solid var(--border-subtle); padding-top: 8px; margin-top: 6px; display: flex; justify-content: space-between; align-items: center;">
            <span id="checkout-after-label" style="color: var(--text-secondary);">İşlem Sonrası Bakiye:</span>
            <strong id="checkout-after-balance" style="font-family: var(--font-mono); color: #fff;">0.00 <?php echo STORE_CURRENCY; ?></strong>
          </div>
        </div>

        <div id="checkout-insufficient-box" style="display: none; background: rgba(255, 85, 85, 0.08); border: 1px solid rgba(255, 85, 85, 0.35); border-radius: var(--radius-sm); padding: 12px 14px; margin-bottom: 14px; font-size: 0.8rem; color: #fca5a5; line-height: 1.45;">
          <i class="fa-solid fa-triangle-exclamation"></i>
          <strong>Cüzdan bakiyeniz bu paket için yetersiz.</strong> Mağaza üst barından hediye/bakiye kodu kullanabilir veya <a href="support.php?tab=new" style="color: #fff; text-decoration: underline;">Destek & Ödeme Talebi</a> / Discord üzerinden anında bakiye yükleyebilirsiniz.
        </div>

        <div id="checkout-feedback-msg" style="display: none; padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 14px; font-size: 0.84rem;"></div>

        <button type="submit" id="checkout-submit-btn" class="btn btn-primary" style="width: 100%; margin-top: 4px;">
          <i class="fa-solid fa-bolt"></i>
          <span>Cüzdan ile Anında Satın Al</span>
        </button>

        <div style="font-size: 0.72rem; color: #777782; text-align: center; margin-top: 14px; line-height: 1.4;">
          <i class="fa-solid fa-circle-info"></i> Onayladığınız anda tutar cüzdanınızdan düşülür ve VIP rütbeniz oyun içinde otomatik tanımlanır.
        </div>
      </form>
    <?php endif; ?>
  </div>
</div>

<?php if ($isStaff): ?>
<!-- OYUNCU ÖZEL İZİN DÜZENLEME MODALI -->
<div class="modal-overlay" id="userPermsModal">
  <div class="modal-box" style="max-width: 520px;">
    <button class="modal-close" onclick="document.getElementById('userPermsModal').classList.remove('active')"><i class="fa-solid fa-xmark"></i></button>
    <div class="modal-header">
      <h3 id="uPermModalTitle">Oyuncu Özel İzinleri</h3>
      <p style="font-size: 0.82rem; color: var(--text-secondary);">Rollerden bağımsız olarak bu oyuncuya özel eklenen veya <code>-izin</code> ile engellenen izinler.</p>
    </div>
    <form onsubmit="saveUserCustomPerms(event)">
      <input type="hidden" id="uPermTargetUser">
      <div class="form-group">
        <label class="form-label">Global Özel İzinler (Her satıra 1 izin)</label>
        <textarea id="uPermGlobalArea" class="form-input gp-mono-area" rows="6" placeholder="essentials.fly&#10;-essentials.tpa"></textarea>
      </div>
      <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">
        <i class="fa-solid fa-floppy-disk"></i> Özel İzinleri Kaydet
      </button>
    </form>
  </div>
</div>

<style>
.store-mode-switcher { display: flex; justify-content: center; gap: 10px; margin-bottom: 32px; flex-wrap: wrap; }
.store-mode-btn { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.12); color: var(--text-secondary); padding: 11px 22px; border-radius: 999px; font-weight: 700; font-size: 0.88rem; cursor: pointer; display: inline-flex; align-items: center; gap: 9px; transition: all 0.18s ease; }
.store-mode-btn:hover { color: #fff; border-color: rgba(255,255,255,0.3); }
.store-mode-btn.active { background: #ffffff; color: #070708; border-color: #ffffff; box-shadow: 0 0 24px rgba(255,255,255,0.18); }
.store-mode-pill { font-family: var(--font-mono); font-size: 0.68rem; padding: 2px 7px; border-radius: 99px; background: rgba(0,0,0,0.15); border: 1px solid currentColor; }

.admin-header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; gap: 20px; flex-wrap: wrap; }
.admin-badge { display: inline-flex; align-items: center; gap: 8px; padding: 6px 14px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.16); border-radius: 999px; font-size: 0.74rem; font-weight: 700; letter-spacing: 1px; color: #fff; margin-bottom: 10px; }
.admin-title { font-family: var(--font-display); font-size: 2.1rem; font-weight: 800; color: #fff; margin-bottom: 6px; }
.admin-subtitle { color: var(--text-secondary); font-size: 0.92rem; max-width: 700px; line-height: 1.5; }
.admin-global-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.system-stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 14px; margin-bottom: 28px; }
.stat-card { background: rgba(18,18,22,0.75); border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 16px 18px; display: flex; align-items: center; gap: 14px; }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 1.1rem; }
.stat-info { display: flex; flex-direction: column; }
.stat-label { font-size: 0.72rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 3px; }
.stat-value { font-family: var(--font-mono); font-size: 1.05rem; color: #fff; }

.pin-lock-overlay { background: rgba(14,14,18,0.88); border: 1px solid rgba(255,255,255,0.15); border-radius: 18px; padding: 46px 24px; text-align: center; margin-bottom: 28px; }
.pin-lock-overlay.hidden { display: none; }
.blurred-content { filter: blur(5px); pointer-events: none; user-select: none; opacity: 0.45; }
.pin-card { max-width: 430px; margin: 0 auto; }
.pin-icon { width: 60px; height: 60px; margin: 0 auto 14px; border-radius: 50%; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 1.5rem; color: #fff; }
.pin-input-wrap { display: flex; gap: 10px; margin: 18px 0 10px; }
.pin-input-wrap input { flex: 1; background: rgba(0,0,0,0.45); border: 1px solid rgba(255,255,255,0.2); border-radius: 10px; padding: 11px 14px; color: #fff; font-family: var(--font-mono); }
.pin-error-msg { color: #ff6b6b; font-size: 0.84rem; min-height: 20px; }
.pin-hint { font-size: 0.76rem; color: var(--text-muted); }

.gp-tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 12px; }
.gp-tab { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.1); color: var(--text-secondary); padding: 10px 18px; border-radius: 10px; font-weight: 600; font-size: 0.88rem; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; transition: all 0.18s ease; }
.gp-tab:hover { color: #fff; border-color: rgba(255,255,255,0.25); }
.gp-tab.active { background: #ffffff; color: #070708; border-color: #ffffff; }

.gp-pane { display: none; }
.gp-pane.active { display: block; }

.gp-roles-layout { display: grid; grid-template-columns: 300px 1fr; gap: 20px; align-items: start; }
@media (max-width: 980px) { .gp-roles-layout { grid-template-columns: 1fr; } }

.gp-card { background: rgba(15,15,19,0.9); border: 1px solid rgba(255,255,255,0.09); border-radius: 14px; padding: 22px; }
.gp-card-title { font-size: 1.05rem; font-weight: 700; color: #fff; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }

.gp-role-list { display: flex; flex-direction: column; gap: 8px; }
.gp-role-item { padding: 11px 13px; border-radius: 10px; background: rgba(255,255,255,0.025); border: 1px solid rgba(255,255,255,0.07); cursor: pointer; display: flex; align-items: center; justify-content: space-between; transition: all 0.15s ease; }
.gp-role-item:hover { background: rgba(255,255,255,0.06); }
.gp-role-item.active { background: rgba(255,255,255,0.11); border-color: #ffffff; }

.gp-chat-preview-box { background: #09090c; border: 1px solid rgba(255,255,255,0.12); border-radius: 10px; padding: 12px 16px; margin-bottom: 18px; }

.gp-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.gp-grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.gp-grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
@media (max-width: 780px) { .gp-grid-2, .gp-grid-3, .gp-grid-4 { grid-template-columns: 1fr; } }

.gp-mono-area { font-family: var(--font-mono); font-size: 0.8rem; line-height: 1.45; }
.gp-mini-srv-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.gp-srv-tag { display: block; font-family: var(--font-mono); font-size: 0.68rem; color: #a1a1aa; margin-bottom: 3px; }

.gp-check-pills { display: flex; flex-wrap: wrap; gap: 8px; }
.gp-check-pill { display: inline-flex; align-items: center; gap: 6px; padding: 6px 11px; border-radius: 8px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.12); font-size: 0.8rem; color: #e4e4e7; cursor: pointer; }

.gp-vip-settings-box { margin-top: 12px; padding: 16px; border-radius: 12px; background: rgba(255,255,255,0.025); border: 1px solid rgba(255,255,255,0.09); }

.gp-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
.gp-table th { text-align: left; padding: 10px 8px; border-bottom: 1px solid rgba(255,255,255,0.1); font-family: var(--font-mono); font-size: 0.72rem; text-transform: uppercase; color: var(--text-muted); }
.gp-table td { padding: 11px 8px; border-bottom: 1px solid rgba(255,255,255,0.05); vertical-align: middle; }
.gp-role-pill { display: inline-flex; align-items: center; gap: 6px; padding: 3px 9px; border-radius: 999px; font-family: var(--font-mono); font-size: 0.72rem; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.22); margin: 2px 4px 2px 0; }
.gp-role-pill button { background: none; border: none; color: #ff6b6b; cursor: pointer; padding: 0 2px; font-size: 0.75rem; }
</style>

<script>
const GP_UNLOCKED = <?php echo $isUnlocked ? 'true' : 'false'; ?>;
let GP_STATE = { roles: [], users: [], coupons: [], orders: [], revision: 1 };
let SELECTED_ROLE_ID = null;

const MC_COLORS = {
  '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
  '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
  '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
  'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF'
};

function switchStoreMainView(view) {
  const cust = document.getElementById('storeCustomerView');
  const adm = document.getElementById('storeAdminView');
  const btns = document.querySelectorAll('.store-mode-btn');
  if (view === 'admin') {
    if (cust) cust.style.display = 'none';
    if (adm) adm.style.display = 'block';
    btns[0]?.classList.remove('active');
    btns[1]?.classList.add('active');
    history.replaceState(null, '', 'store.php?view=admin');
    if (GP_UNLOCKED && (!GP_STATE.roles || GP_STATE.roles.length === 0)) {
      loadAdminOverview();
    }
  } else {
    if (cust) cust.style.display = 'block';
    if (adm) adm.style.display = 'none';
    btns[0]?.classList.add('active');
    btns[1]?.classList.remove('active');
    history.replaceState(null, '', 'store.php');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.gp-tab').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.gp-tab').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.gp-pane').forEach(p => p.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('pane-' + btn.dataset.tab).classList.add('active');
    });
  });

  document.getElementById('btnLockPanel')?.addEventListener('click', async () => {
    await fetch('api/perms.php?action=lock_panel');
    location.reload();
  });

  document.getElementById('btnForceSync')?.addEventListener('click', async () => {
    const res = await fetch('api/perms.php?action=admin_force_sync', { method: 'POST' });
    const data = await res.json();
    showToast(data.message || 'Senkronizasyon tetiklendi.');
    loadAdminOverview();
  });

  if (GP_UNLOCKED) {
    loadAdminOverview();
  }
});

async function handlePermPinSubmit(e) {
  e.preventDefault();
  const pin = document.getElementById('adminPinInput').value;
  const errEl = document.getElementById('pinErrorMsg');
  errEl.textContent = '';

  const res = await fetch('api/perms.php?action=unlock_panel', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pin })
  });
  const data = await res.json();
  if (res.ok && data.status === 'success') {
    location.href = 'store.php?view=admin';
  } else {
    errEl.textContent = data.message || 'Hatalı PIN!';
  }
}

async function loadAdminOverview() {
  const res = await fetch('api/perms.php?action=admin_overview');
  const data = await res.json();
  if (!res.ok || data.status !== 'success') return;

  GP_STATE = data;
  document.getElementById('statRolesCount').textContent = `${data.roles.length} Grup`;
  document.getElementById('statUsersCount').textContent = `${data.users.length} Oyuncu`;
  document.getElementById('statCouponsCount').textContent = `${data.coupons.length} Kod`;
  document.getElementById('statRevisionVal').textContent = `#${data.revision}`;

  renderRolesSidebar();
  populateRoleDropdowns();
  if (!SELECTED_ROLE_ID && data.roles.length > 0) {
    selectRoleForEdit(data.roles[0].id);
  } else if (SELECTED_ROLE_ID) {
    const stillExists = data.roles.find(r => r.id === SELECTED_ROLE_ID);
    if (stillExists) selectRoleForEdit(SELECTED_ROLE_ID);
    else if (data.roles.length > 0) selectRoleForEdit(data.roles[0].id);
  }

  renderPlayersTable();
  renderCouponsTable();
  renderOrdersTable();
}

function escapeHtml(str) {
  return String(str ?? '').replace(/[&<>"']/g, s => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[s]));
}

function mcFormatToHtml(raw) {
  let html = '';
  let color = '#ffffff';
  let bold = false;
  const text = String(raw || '');
  for (let i = 0; i < text.length; i++) {
    if ((text[i] === '&' || text[i] === '§') && i + 1 < text.length) {
      const code = text[i + 1].toLowerCase();
      if (MC_COLORS[code]) { color = MC_COLORS[code]; bold = false; i++; continue; }
      if (code === 'l') { bold = true; i++; continue; }
      if (code === 'r') { color = '#ffffff'; bold = false; i++; continue; }
    }
    html += `<span style="color:${color};${bold ? 'font-weight:800;' : ''}">${escapeHtml(text[i])}</span>`;
  }
  return html;
}

function renderChatPreview() {
  const prefix = document.getElementById('rolePrefixInput').value;
  const suffix = document.getElementById('roleSuffixInput').value;
  const chatColor = document.getElementById('roleChatColorInput').value || '&f';
  const sample = `${prefix}OyuncuAdi${suffix} &8» ${chatColor}Merhaba GrizmMC ailesi!`;
  const nametagSample = `${prefix}OyuncuAdi${suffix}`;
  document.getElementById('mcChatPreview').innerHTML = mcFormatToHtml(sample);
  const ntEl = document.getElementById('mcNametagPreview');
  if (ntEl) ntEl.innerHTML = mcFormatToHtml(nametagSample);
}

function renderRolesSidebar() {
  const listEl = document.getElementById('rolesSidebarList');
  listEl.innerHTML = GP_STATE.roles.map(r => `
    <div class="gp-role-item ${r.id === SELECTED_ROLE_ID ? 'active' : ''}" onclick="selectRoleForEdit('${escapeHtml(r.id)}')">
      <div>
        <div style="display:flex;align-items:center;gap:8px;">
          <span style="width:9px;height:9px;border-radius:50%;background:${escapeHtml(r.badge_color || '#fff')};"></span>
          <strong style="color:#fff;font-size:0.9rem;">${escapeHtml(r.name)}</strong>
          ${r.is_vip ? `<span style="font-size:0.65rem;padding:1px 6px;border-radius:99px;background:rgba(255,255,255,0.1);color:#fff;">${escapeHtml(r.vip_price)} ₺</span>` : ''}
        </div>
        <div style="font-family:var(--font-mono);font-size:0.72rem;color:#888;margin-top:2px;">ID: ${escapeHtml(r.id)} &bull; ${r.permissions.length} izin</div>
      </div>
      <span style="font-family:var(--font-mono);font-size:0.75rem;color:#aaa;">W:${r.weight}</span>
    </div>
  `).join('');
}

function populateRoleDropdowns() {
  const opts = GP_STATE.roles.map(r => `<option value="${escapeHtml(r.id)}">${escapeHtml(r.name)} (${escapeHtml(r.id)})</option>`).join('');
  document.getElementById('grantRoleSelect').innerHTML = opts;
  document.getElementById('cpRoleSelect').innerHTML = opts;
}

function selectRoleForEdit(roleId) {
  const role = GP_STATE.roles.find(r => r.id === roleId);
  if (!role) return;
  SELECTED_ROLE_ID = role.id;
  renderRolesSidebar();

  document.getElementById('editorHeadingTitle').textContent = `${role.name} (${role.id})`;
  const idInput = document.getElementById('roleIdInput');
  idInput.value = role.id;
  idInput.readOnly = true;

  document.getElementById('roleNameInput').value = role.name || '';
  document.getElementById('roleWeightInput').value = role.weight ?? 10;
  document.getElementById('rolePrefixInput').value = role.prefix || '';
  document.getElementById('roleSuffixInput').value = role.suffix || '';
  document.getElementById('roleChatColorInput').value = role.chat_color || '&f';
  document.getElementById('roleBadgeColorInput').value = role.badge_color || '#ffffff';

  document.getElementById('roleGlobalPerms').value = (role.permissions || []).join('\n');
  const sp = role.server_permissions || {};
  document.getElementById('roleSrvLobby').value = (sp.lobby || []).join('\n');
  document.getElementById('roleSrvSkyblock').value = (sp.skyblock || []).join('\n');
  document.getElementById('roleSrvSmp').value = (sp.smp || []).join('\n');
  document.getElementById('roleSrvPvp').value = (sp.pvp || []).join('\n');

  const inhSet = new Set(role.inheritance || []);
  document.getElementById('roleInheritanceChecks').innerHTML = GP_STATE.roles
    .filter(other => other.id !== role.id)
    .map(other => `
      <label class="gp-check-pill">
        <input type="checkbox" name="roleInh" value="${escapeHtml(other.id)}" ${inhSet.has(other.id) ? 'checked' : ''}>
        <span>${escapeHtml(other.name)}</span>
      </label>
    `).join('');

  document.getElementById('roleIsVipCheck').checked = !!role.is_vip;
  document.getElementById('roleVipHighlightCheck').checked = !!role.vip_highlight;
  document.getElementById('roleIsDefaultCheck').checked = !!role.is_default;
  document.getElementById('roleVipPriceInput').value = role.vip_price ?? 0;
  document.getElementById('roleVipDaysInput').value = role.vip_duration_days ?? 30;
  document.getElementById('roleVipTierInput').value = role.vip_tier || '';
  document.getElementById('roleVipBadgeInput').value = role.vip_badge || '';
  document.getElementById('roleVipPerksArea').value = (role.vip_perks || []).join('\n');
  document.getElementById('roleVipCommandsArea').value = (role.vip_reward_commands || [])
    .map(c => `${c.server || 'first'}:${c.command}`).join('\n');

  document.getElementById('btnDeleteRole').style.display = role.is_default ? 'none' : 'inline-flex';
  toggleVipFields();
  renderChatPreview();
}

function startCreateNewRole() {
  SELECTED_ROLE_ID = null;
  renderRolesSidebar();
  document.getElementById('editorHeadingTitle').textContent = 'Yeni Rol Oluştur';
  const idInput = document.getElementById('roleIdInput');
  idInput.value = '';
  idInput.readOnly = false;
  idInput.focus();

  document.getElementById('roleNameInput').value = '';
  document.getElementById('roleWeightInput').value = 50;
  document.getElementById('rolePrefixInput').value = '&7[YeniRol] &f';
  document.getElementById('roleSuffixInput').value = '';
  document.getElementById('roleChatColorInput').value = '&f';
  document.getElementById('roleBadgeColorInput').value = '#ffffff';
  document.getElementById('roleGlobalPerms').value = '';
  document.getElementById('roleSrvLobby').value = '';
  document.getElementById('roleSrvSkyblock').value = '';
  document.getElementById('roleSrvSmp').value = '';
  document.getElementById('roleSrvPvp').value = '';
  document.getElementById('roleIsVipCheck').checked = false;
  document.getElementById('roleVipHighlightCheck').checked = false;
  document.getElementById('roleIsDefaultCheck').checked = false;
  document.getElementById('roleVipPerksArea').value = '';
  document.getElementById('roleVipCommandsArea').value = '';
  document.getElementById('btnDeleteRole').style.display = 'none';

  document.getElementById('roleInheritanceChecks').innerHTML = GP_STATE.roles.map(other => `
    <label class="gp-check-pill">
      <input type="checkbox" name="roleInh" value="${escapeHtml(other.id)}" ${other.id === 'default' ? 'checked' : ''}>
      <span>${escapeHtml(other.name)}</span>
    </label>
  `).join('');

  toggleVipFields();
  renderChatPreview();
}

function toggleVipFields() {
  const show = document.getElementById('roleIsVipCheck').checked;
  document.getElementById('vipExtraFields').style.display = show ? 'block' : 'none';
}

async function saveCurrentRole(e) {
  e.preventDefault();
  const id = document.getElementById('roleIdInput').value.trim().toLowerCase();
  const inheritance = Array.from(document.querySelectorAll('input[name="roleInh"]:checked')).map(cb => cb.value);

  const rewardCommands = document.getElementById('roleVipCommandsArea').value
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(Boolean)
    .map(line => {
      const idx = line.indexOf(':');
      if (idx > 0) {
        return { server: line.slice(0, idx).trim(), command: line.slice(idx + 1).trim() };
      }
      return { server: 'first', command: line };
    });

  const payload = {
    id,
    name: document.getElementById('roleNameInput').value.trim(),
    weight: parseInt(document.getElementById('roleWeightInput').value || '10', 10),
    prefix: document.getElementById('rolePrefixInput').value,
    suffix: document.getElementById('roleSuffixInput').value,
    chat_color: document.getElementById('roleChatColorInput').value,
    badge_color: document.getElementById('roleBadgeColorInput').value,
    is_default: document.getElementById('roleIsDefaultCheck').checked,
    is_vip: document.getElementById('roleIsVipCheck').checked,
    vip_highlight: document.getElementById('roleVipHighlightCheck').checked,
    vip_price: parseFloat(document.getElementById('roleVipPriceInput').value || '0'),
    vip_duration_days: parseInt(document.getElementById('roleVipDaysInput').value || '30', 10),
    vip_tier: document.getElementById('roleVipTierInput').value.trim(),
    vip_badge: document.getElementById('roleVipBadgeInput').value.trim(),
    vip_perks: document.getElementById('roleVipPerksArea').value,
    vip_reward_commands: rewardCommands,
    inheritance,
    permissions: document.getElementById('roleGlobalPerms').value,
    server_permissions: {
      lobby: document.getElementById('roleSrvLobby').value,
      skyblock: document.getElementById('roleSrvSkyblock').value,
      smp: document.getElementById('roleSrvSmp').value,
      pvp: document.getElementById('roleSrvPvp').value
    }
  };

  const res = await fetch('api/perms.php?action=admin_save_role', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  if (res.ok && data.status === 'success') {
    SELECTED_ROLE_ID = data.role.id;
    showToast(data.message);
    loadAdminOverview();
  } else {
    showToast(data.message || 'Rol kaydedilemedi.');
  }
}

async function deleteCurrentRole() {
  if (!SELECTED_ROLE_ID) return;
  if (!confirm(`'${SELECTED_ROLE_ID}' rolünü kalıcı olarak silmek istediğinize emin misiniz?`)) return;

  const res = await fetch('api/perms.php?action=admin_delete_role', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id: SELECTED_ROLE_ID })
  });
  const data = await res.json();
  if (res.ok && data.status === 'success') {
    SELECTED_ROLE_ID = null;
    showToast('Rol silindi.');
    loadAdminOverview();
  } else {
    showToast(data.message || 'Rol silinemedi.');
  }
}

function formatExpiryShort(exp) {
  if (!exp || exp <= 0) return 'Kalıcı';
  const diff = exp - Math.floor(Date.now() / 1000);
  if (diff <= 0) return 'Doldu';
  const d = Math.floor(diff / 86400);
  const h = Math.floor((diff % 86400) / 3600);
  return d > 0 ? `${d}g ${h}s` : `${h}s`;
}

function renderPlayersTable() {
  const q = (document.getElementById('playerSearchInput')?.value || '').trim().toLowerCase();
  const roleMap = Object.fromEntries(GP_STATE.roles.map(r => [r.id, r]));
  const filtered = GP_STATE.users.filter(u => {
    if (!q) return true;
    return (u.username || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q);
  });

  const tbody = document.getElementById('playersTableBody');
  if (filtered.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" style="color:#888;text-align:center;padding:20px;">Eşleşen oyuncu bulunamadı.</td></tr>';
    return;
  }

  tbody.innerHTML = filtered.map(u => {
    const rolesHtml = (u.roles && u.roles.length > 0)
      ? u.roles.map(ass => {
          const rObj = roleMap[ass.role_id] || { name: ass.role_id, badge_color: '#fff' };
          return `<span class="gp-role-pill" style="border-color:${escapeHtml(rObj.badge_color || '#fff')};color:#fff;">
            ${escapeHtml(rObj.name)} <small style="color:#aaa;">(${formatExpiryShort(ass.expires_at)})</small>
            <button type="button" title="Rolü Kaldır" onclick="revokeUserRole('${escapeHtml(u.username)}','${escapeHtml(ass.role_id)}')">&times;</button>
          </span>`;
        }).join('')
      : '<span style="color:#777;font-family:var(--font-mono);font-size:0.76rem;">Oyuncu (Varsayılan)</span>';

    const customPermCount = (u.permissions || []).length;
    return `
      <tr>
        <td>
          <div style="display:flex;align-items:center;gap:10px;">
            <img src="https://mc-heads.net/avatar/${encodeURIComponent(u.username)}/28" style="width:28px;height:28px;border-radius:4px;image-rendering:pixelated;" onerror="this.src='https://mc-heads.net/avatar/Steve/28';">
            <div>
              <strong style="color:#fff;">${escapeHtml(u.username)}</strong>
              <div style="font-size:0.74rem;color:#888;">${escapeHtml(u.email || '')}</div>
            </div>
          </div>
        </td>
        <td>${rolesHtml}</td>
        <td style="font-family:var(--font-mono);font-size:0.78rem;color:#ccc;">${customPermCount} özel izin</td>
        <td style="font-family:var(--font-mono);font-weight:700;color:#fff;">${Number(u.credits || 0).toFixed(2)} ₺</td>
        <td>
          <div style="display:flex;gap:6px;flex-wrap:wrap;">
            <button class="btn btn-ghost btn-sm" style="padding:5px 10px;font-size:0.75rem;" onclick="prefillPlayerForms('${escapeHtml(u.username)}')">Seç</button>
            <button class="btn btn-outline btn-sm" style="padding:5px 10px;font-size:0.75rem;" onclick="openUserPermsModal('${escapeHtml(u.username)}')">İzinler</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function prefillPlayerForms(username) {
  document.getElementById('grantUsername').value = username;
  document.getElementById('creditUsername').value = username;
  window.scrollTo({ top: 240, behavior: 'smooth' });
}

async function handleAdminGrantRole(e) {
  e.preventDefault();
  const payload = {
    username: document.getElementById('grantUsername').value.trim(),
    role_id: document.getElementById('grantRoleSelect').value,
    duration_days: parseInt(document.getElementById('grantDaysInput').value || '0', 10),
    celebrate: document.getElementById('grantCelebrateCheck').checked
  };
  const res = await fetch('api/perms.php?action=admin_grant_role', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  showToast(data.message || 'İşlem tamamlandı.');
  if (res.ok) loadAdminOverview();
}

async function revokeUserRole(username, roleId) {
  if (!confirm(`${username} oyuncusundan '${roleId}' rolünü kaldırmak istiyor musunuz?`)) return;
  const res = await fetch('api/perms.php?action=admin_revoke_role', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, role_id: roleId })
  });
  const data = await res.json();
  showToast(data.message || 'Rol kaldırıldı.');
  if (res.ok) loadAdminOverview();
}

async function handleAdminCredits(e) {
  e.preventDefault();
  const payload = {
    username: document.getElementById('creditUsername').value.trim(),
    mode: document.getElementById('creditModeSelect').value,
    amount: parseFloat(document.getElementById('creditAmountInput').value || '0')
  };
  const res = await fetch('api/perms.php?action=admin_adjust_credits', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  showToast(data.message || 'Bakiye güncellendi.');
  if (res.ok) loadAdminOverview();
}

function openUserPermsModal(username) {
  const user = GP_STATE.users.find(u => u.username.toLowerCase() === username.toLowerCase());
  document.getElementById('uPermTargetUser').value = username;
  document.getElementById('uPermModalTitle').textContent = `${username} - Özel İzinler`;
  document.getElementById('uPermGlobalArea').value = (user?.permissions || []).join('\n');
  document.getElementById('userPermsModal').classList.add('active');
}

async function saveUserCustomPerms(e) {
  e.preventDefault();
  const username = document.getElementById('uPermTargetUser').value;
  const permissions = document.getElementById('uPermGlobalArea').value;
  const res = await fetch('api/perms.php?action=admin_save_user_perms', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, permissions })
  });
  const data = await res.json();
  showToast(data.message || 'Kaydedildi.');
  document.getElementById('userPermsModal').classList.remove('active');
  if (res.ok) loadAdminOverview();
}

function toggleCouponTypeFields() {
  const isVip = document.getElementById('cpType').value === 'vip';
  document.getElementById('cpCreditWrap').style.display = isVip ? 'none' : 'block';
  document.getElementById('cpVipWrap').style.display = isVip ? 'grid' : 'none';
}

async function handleCreateCoupon(e) {
  e.preventDefault();
  const payload = {
    code: document.getElementById('cpCode').value.trim(),
    type: document.getElementById('cpType').value,
    amount: parseFloat(document.getElementById('cpAmount').value || '0'),
    role_id: document.getElementById('cpRoleSelect').value,
    duration_days: parseInt(document.getElementById('cpVipDays').value || '30', 10),
    max_uses: parseInt(document.getElementById('cpMaxUses').value || '1', 10),
    expire_days: parseInt(document.getElementById('cpExpireDays').value || '0', 10)
  };
  const res = await fetch('api/perms.php?action=admin_create_coupon', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  showToast(data.message || 'Kupon oluşturuldu.');
  if (res.ok) {
    document.getElementById('cpCode').value = '';
    loadAdminOverview();
  }
}

async function deleteCoupon(code) {
  if (!confirm(`'${code}' kupon kodunu silmek istiyor musunuz?`)) return;
  const res = await fetch('api/perms.php?action=admin_delete_coupon', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code })
  });
  const data = await res.json();
  showToast(data.message || 'Silindi.');
  if (res.ok) loadAdminOverview();
}

function renderCouponsTable() {
  const tbody = document.getElementById('couponsTableBody');
  if (!GP_STATE.coupons.length) {
    tbody.innerHTML = '<tr><td colspan="4" style="color:#888;text-align:center;">Aktif kupon bulunmuyor.</td></tr>';
    return;
  }
  tbody.innerHTML = GP_STATE.coupons.map(c => {
    const used = (c.used_by || []).length;
    const limit = c.max_uses > 0 ? `${used} / ${c.max_uses}` : `${used} / ∞`;
    const reward = c.type === 'vip'
      ? `<span style="color:#fff;">${escapeHtml(c.role_id)} (${c.duration_days}g)</span>`
      : `<span style="color:#fff;font-family:var(--font-mono);">+${Number(c.amount).toFixed(2)} ₺</span>`;
    return `
      <tr>
        <td style="font-family:var(--font-mono);font-weight:700;color:#fff;">${escapeHtml(c.code)}</td>
        <td>${reward}</td>
        <td style="font-family:var(--font-mono);color:#aaa;">${limit}</td>
        <td>
          <button class="btn btn-outline btn-sm" style="color:#ff6b6b;border-color:rgba(255,107,107,0.3);padding:4px 9px;" onclick="deleteCoupon('${escapeHtml(c.code)}')">
            <i class="fa-solid fa-trash"></i>
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function renderOrdersTable() {
  const tbody = document.getElementById('ordersTableBody');
  if (!GP_STATE.orders.length) {
    tbody.innerHTML = '<tr><td colspan="6" style="color:#888;text-align:center;padding:18px;">Henüz kayıtlı işlem yok.</td></tr>';
    return;
  }
  tbody.innerHTML = GP_STATE.orders.map(o => {
    const dt = new Date((o.created_at || 0) * 1000).toLocaleString('tr-TR');
    let typeDisplay = escapeHtml(o.role_name || o.type);
    if (o.type === 'crate_key_purchase') {
      typeDisplay = `<span style="color:#facc15;"><i class="fa-solid fa-key" style="margin-right:4px;"></i>${escapeHtml(o.role_name || 'Kasa Anahtarı')}</span>`;
    }
    return `
      <tr>
        <td style="font-family:var(--font-mono);color:#aaa;">${escapeHtml(o.order_id)}</td>
        <td><strong style="color:#fff;">${escapeHtml(o.username)}</strong></td>
        <td>${typeDisplay}</td>
        <td style="font-family:var(--font-mono);color:#fff;">${Number(o.amount || 0) > 0 ? Number(o.amount).toFixed(2) + ' ₺' : '-'}</td>
        <td style="color:var(--text-secondary);">${escapeHtml(o.provider)} &bull; ${escapeHtml(o.note || '')}</td>
        <td style="font-family:var(--font-mono);color:#888;">${escapeHtml(dt)}</td>
      </tr>
    `;
  }).join('');
}
</script>

<script>
// ============================================================================
// KASA & ANAHTAR MARKETİ İSTEMCİ SCRIPTI
// ============================================================================
function selectCrateBundle(btn, tier) {
  const card = btn.closest('.crate-card');
  if (!card) return;

  card.querySelectorAll('.crate-bundle-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');

  const amount = parseInt(btn.dataset.amount || '1', 10);
  const price = parseFloat(btn.dataset.price || '0');
  const discount = btn.dataset.discount || '';

  const priceDisp = card.querySelector(`[data-crate-price-display="${tier}"]`);
  const unitHint = card.querySelector(`[data-crate-unit-hint="${tier}"]`);
  const discNote = card.querySelector(`[data-crate-discount-note="${tier}"]`);
  const buyBtn = card.querySelector('[data-buy-crate="true"]');

  if (priceDisp) priceDisp.textContent = price.toFixed(2);
  if (unitHint) unitHint.textContent = `/ ${amount} Anahtar`;
  if (discNote) {
    if (discount) {
      discNote.textContent = `★ ${discount} uygulandı!`;
      discNote.style.display = 'block';
    } else {
      discNote.style.display = 'none';
    }
  }

  if (buyBtn) {
    buyBtn.setAttribute('data-amount', amount);
    buyBtn.setAttribute('data-price', price);
    const textSpan = buyBtn.querySelector('.crate-buy-text');
    if (textSpan) {
      textSpan.textContent = `${amount} Adet Satın Al`;
    }
  }
}

function openCrateCheckoutModal(btn) {
  const modal = document.getElementById('crate-checkout-modal');
  if (!modal) return;

  const isLoggedIn = modal.getAttribute('data-logged-in') === '1';
  if (!isLoggedIn) {
    modal.classList.add('active');
    return;
  }

  const tier = btn.getAttribute('data-tier') || '';
  const tierName = btn.getAttribute('data-tier-name') || 'Kasa Anahtarı';
  const amount = parseInt(btn.getAttribute('data-amount') || '1', 10);
  const price = parseFloat(btn.getAttribute('data-price') || '0');
  const color = btn.getAttribute('data-color') || '#ffffff';

  const modalTitle = document.getElementById('crate-modal-title');
  const modalSub = document.getElementById('crate-modal-subtitle');
  const modalIconWrap = document.getElementById('crate-modal-icon-wrap');
  const tierInput = document.getElementById('crate-checkout-tier');
  const amountInput = document.getElementById('crate-checkout-amount');
  const priceInput = document.getElementById('crate-checkout-price-num');
  const summaryAmountLabel = document.getElementById('crate-summary-amount-label');
  const summaryPrice = document.getElementById('crate-summary-price');
  const feedbackMsg = document.getElementById('crate-checkout-feedback-msg');

  if (modalTitle) modalTitle.textContent = tierName;
  if (modalSub) modalSub.textContent = `${amount} Adet Anahtar Paketi`;
  if (modalIconWrap) {
    modalIconWrap.style.borderColor = color;
    modalIconWrap.style.color = color;
    modalIconWrap.style.boxShadow = `0 0 20px ${color}44`;
  }

  if (tierInput) tierInput.value = tier;
  if (amountInput) amountInput.value = amount;
  if (priceInput) priceInput.value = price;
  if (summaryAmountLabel) summaryAmountLabel.textContent = `${amount}x`;
  if (summaryPrice) summaryPrice.textContent = `${price.toFixed(2)} ₺`;
  if (feedbackMsg) feedbackMsg.style.display = 'none';

  refreshCrateCheckoutMath(price);
  modal.classList.add('active');
}

function closeCrateCheckoutModal() {
  const modal = document.getElementById('crate-checkout-modal');
  if (modal) modal.classList.remove('active');
}

function refreshCrateCheckoutMath(priceNum) {
  const modal = document.getElementById('crate-checkout-modal');
  if (!modal) return;

  const userCredits = parseFloat(modal.getAttribute('data-user-credits') || '0');
  const afterLabel = document.getElementById('crate-checkout-after-label');
  const afterBal = document.getElementById('crate-checkout-after-balance');
  const insuffBox = document.getElementById('crate-checkout-insufficient-box');
  const submitBtn = document.getElementById('crate-checkout-submit-btn');

  const diff = userCredits - priceNum;
  if (afterBal) {
    if (diff >= 0) {
      if (afterLabel) afterLabel.textContent = 'İşlem Sonrası Bakiye:';
      afterBal.textContent = `${diff.toFixed(2)} ₺`;
      afterBal.style.color = '#ffffff';
      if (insuffBox) insuffBox.style.display = 'none';
      if (submitBtn) submitBtn.disabled = false;
    } else {
      if (afterLabel) afterLabel.textContent = 'Eksik Bakiye:';
      afterBal.textContent = `${Math.abs(diff).toFixed(2)} ₺`;
      afterBal.style.color = '#ff6b6b';
      if (insuffBox) insuffBox.style.display = 'block';
      if (submitBtn) submitBtn.disabled = true;
    }
  }
}

async function submitCratePurchase(e) {
  e.preventDefault();
  const tier = document.getElementById('crate-checkout-tier').value;
  const amount = parseInt(document.getElementById('crate-checkout-amount').value || '1', 10);
  const targetUsername = document.getElementById('crate-target-username').value.trim();
  const submitBtn = document.getElementById('crate-checkout-submit-btn');
  const feedbackMsg = document.getElementById('crate-checkout-feedback-msg');

  if (!targetUsername) {
    alert('Lütfen teslim edilecek oyuncu adını yazın.');
    return;
  }

  const origBtnHtml = submitBtn.innerHTML;
  submitBtn.disabled = true;
  submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> <span>Teslimat Kuyruğuna Ekleniyor...</span>';

  try {
    const res = await fetch('api/perms.php?action=buy_crate_key', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        tier: tier,
        amount: amount,
        target_username: targetUsername
      })
    });
    const data = await res.json();

    if (res.ok && data.status === 'success') {
      if (typeof playCopySound === 'function') playCopySound();
      if (typeof updateWalletBalanceElements === 'function') {
        updateWalletBalanceElements(data.credits);
      }
      const modal = document.getElementById('crate-checkout-modal');
      if (modal) {
        modal.setAttribute('data-user-credits', data.credits);
      }
      refreshCrateCheckoutMath(data.total_price);

      if (feedbackMsg) {
        feedbackMsg.style.display = 'block';
        feedbackMsg.style.background = 'rgba(255, 255, 255, 0.08)';
        feedbackMsg.style.border = '1px solid rgba(255, 255, 255, 0.35)';
        feedbackMsg.style.color = '#ffffff';
        feedbackMsg.innerHTML = `<i class="fa-solid fa-circle-check" style="color:#22c55e;"></i> <strong>${data.message}</strong>`;
      }

      if (typeof showToast === 'function') showToast(data.message);
      setTimeout(() => {
        window.location.reload();
      }, 1800);
    } else {
      if (feedbackMsg) {
        feedbackMsg.style.display = 'block';
        feedbackMsg.style.background = 'rgba(255, 85, 85, 0.1)';
        feedbackMsg.style.border = '1px solid rgba(255, 85, 85, 0.4)';
        feedbackMsg.style.color = '#fca5a5';
        feedbackMsg.innerHTML = `<i class="fa-solid fa-circle-xmark"></i> ${data.message || 'İşlem gerçekleştirilemedi.'}`;
      }
      submitBtn.disabled = false;
      submitBtn.innerHTML = origBtnHtml;
    }
  } catch (err) {
    if (feedbackMsg) {
      feedbackMsg.style.display = 'block';
      feedbackMsg.style.background = 'rgba(255, 85, 85, 0.1)';
      feedbackMsg.style.border = '1px solid rgba(255, 85, 85, 0.4)';
      feedbackMsg.style.color = '#fca5a5';
      feedbackMsg.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Sunucu ile bağlantı hatası oluştu.';
    }
    submitBtn.disabled = false;
    submitBtn.innerHTML = origBtnHtml;
  }
}

// Modal dışına tıklayınca kapatma
document.addEventListener('click', (e) => {
  const modal = document.getElementById('crate-checkout-modal');
  if (modal && e.target === modal) {
    closeCrateCheckoutModal();
  }
});
</script>
<?php endif; ?>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
