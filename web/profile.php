<?php
$pageTitle = "Profilim";
$activePage = "profile";
require_once __DIR__ . '/config.php';

// Oturum kontrolü
if (!is_logged_in()) {
    header('Location: login.php');
    exit;
}

$user = get_auth_user();
$mcUsername = get_auth_username();
$permUser = gperm_get_user($mcUsername);
$primaryRoleInfo = gperm_get_user_primary_role($mcUsername);
$primaryRole = $primaryRoleInfo['role'];
$activeVip = gperm_get_user_active_vip($mcUsername);
$userOrders = gperm_get_user_orders($mcUsername);
$profilePermSummary = gperm_get_user_effective_summary($mcUsername);

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section" style="padding-top: 60px;">
  <div class="container">
    <div class="section-header">
      <span class="section-tag"><i class="fa-solid fa-id-card"></i> GrizmID Hesabı</span>
      <h1 class="section-title">KULLANICI PROFİLİ</h1>
      <p class="section-desc">GrizmID hesabınızla başarıyla bağlandınız. Sunucu rütbenizi, cüzdan bakiyenizi ve mağaza işlemlerinizi bu panelden takip edebilirsiniz.</p>
    </div>

    <div style="max-width: 860px; margin: 0 auto;">
      <!-- Profil Kartı -->
      <div class="feature-box" style="padding: 36px; margin-bottom: 26px; position: relative;">
        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 24px;">
          <div style="display: flex; align-items: center; gap: 24px;">
            <div style="width: 80px; height: 80px; border-radius: var(--radius-md); background: #141418; border: 2px solid var(--border-medium); padding: 6px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 24px rgba(0,0,0,0.6);">
              <img 
                src="https://mc-heads.net/avatar/<?php echo urlencode($mcUsername); ?>/68" 
                alt="Avatar" 
                style="width: 100%; height: 100%; image-rendering: pixelated; border-radius: var(--radius-sm);"
                onerror="this.src='https://mc-heads.net/avatar/Steve/68';"
              >
            </div>
            <div>
              <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 6px; flex-wrap: wrap;">
                <h3 style="font-family: var(--font-display); font-size: 1.6rem; color: #fff;"><?php echo htmlspecialchars($mcUsername); ?></h3>
                <span style="font-family: var(--font-mono); font-size: 0.72rem; padding: 3px 10px; background: rgba(255,255,255,0.07); border: 1px solid <?php echo htmlspecialchars($primaryRole['badge_color'] ?? '#fff'); ?>; border-radius: var(--radius-pill); color: <?php echo htmlspecialchars($primaryRole['badge_color'] ?? '#fff'); ?>;">
                  <i class="fa-solid fa-crown"></i> <?php echo htmlspecialchars($primaryRole['name']); ?>
                </span>
                <span style="font-family: var(--font-mono); font-size: 0.72rem; padding: 3px 10px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.3); border-radius: var(--radius-pill); color: #fff;">
                  <i class="fa-solid fa-circle-check"></i> Doğrulandı
                </span>
              </div>
              <div style="font-size: 0.88rem; color: var(--text-secondary); display: flex; align-items: center; gap: 14px; flex-wrap: wrap;">
                <span><i class="fa-solid fa-id-badge"></i> GrizmID: <?php echo htmlspecialchars($user['full_name']); ?></span>
                <span><i class="fa-solid fa-envelope"></i> <?php echo htmlspecialchars($user['email']); ?></span>
              </div>
            </div>
          </div>

          <div style="display: flex; align-items: center; gap: 10px;">
            <a href="set-username.php?edit=1" class="btn btn-ghost btn-sm" title="Minecraft Adını Değiştir">
              <i class="fa-solid fa-pen-to-square"></i>
              <span>Adı Değiştir</span>
            </a>
            <a href="logout.php" class="btn btn-ghost btn-sm" style="color: #ff5555; border-color: rgba(255,85,85,0.3);" title="Çıkış Yap">
              <i class="fa-solid fa-right-from-bracket"></i>
              <span>Çıkış Yap</span>
            </a>
          </div>
        </div>

<?php
$userTickets = get_user_tickets($user['email']);
$userApps = get_user_applications($user['email']);
$activeTicketsCount = 0;
foreach ($userTickets as $t) {
    if ($t['status'] !== 'closed') {
        $activeTicketsCount++;
    }
}
?>

        <div style="margin-top: 28px; padding-top: 22px; border-top: 1px solid var(--border-subtle); display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 18px;">
          <div>
            <div style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-family: var(--font-mono); margin-bottom: 4px;">Cüzdan Bakiyesi</div>
            <div style="font-size: 1.15rem; color: #fff; font-weight: 800; font-family: var(--font-mono);">
              <span data-wallet-balance><?php echo number_format(floatval($permUser['credits'] ?? 0), 2, '.', ''); ?></span> <?php echo STORE_CURRENCY; ?>
            </div>
          </div>
          <div>
            <div style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-family: var(--font-mono); margin-bottom: 4px;">Aktif VIP Durumu</div>
            <div style="font-size: 0.96rem; color: #fff; font-weight: 600;">
              <?php if ($activeVip): ?>
                <?php echo htmlspecialchars($activeVip['role']['name']); ?>
                <span style="display: block; font-size: 0.76rem; color: var(--text-secondary); font-family: var(--font-mono);">
                  Kalan: <?php echo htmlspecialchars(gperm_format_remaining($activeVip['expires_at'])); ?>
                </span>
              <?php else: ?>
                <span style="color: var(--text-secondary);">VIP Yok</span>
              <?php endif; ?>
            </div>
          </div>
          <div>
            <div style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-family: var(--font-mono); margin-bottom: 4px;">Aktif Talepler</div>
            <div style="font-size: 0.96rem; color: <?php echo $activeTicketsCount > 0 ? '#3fb950' : '#fff'; ?>; font-weight: 600;">
              <?php echo $activeTicketsCount > 0 ? $activeTicketsCount . ' Açık Destek Talebi' : 'Talep Bulunmuyor'; ?>
            </div>
          </div>
          <div>
            <div style="font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-family: var(--font-mono); margin-bottom: 4px;">Sunucu IP</div>
            <div style="font-size: 0.96rem; color: #fff; font-weight: 600; font-family: var(--font-mono);"><?php echo SERVER_IP; ?></div>
          </div>
        </div>

        <!-- Hediye / Kupon Kodu Kullanma Barı -->
        <div style="margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--border-subtle); display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
          <div style="font-size: 0.85rem; color: var(--text-secondary);">
            <i class="fa-solid fa-ticket" style="color: #fff;"></i> Hediye veya promosyon kodunuz varsa aşağıdan anında hesabınıza tanımlayın:
          </div>
          <form id="store-coupon-form" style="display: flex; gap: 8px; flex: 1; max-width: 340px;">
            <input type="text" id="store-coupon-input" class="form-input" placeholder="KUPON KODU" style="padding: 9px 12px; font-family: var(--font-mono); font-size: 0.82rem; text-transform: uppercase;" autocomplete="off">
            <button type="submit" class="btn btn-primary btn-sm" style="white-space: nowrap;">Kullan</button>
          </form>
        </div>

        <!-- Aktif Rol Ağacı & İzinler (Permler) Kutusu -->
        <?php if ($profilePermSummary):
          $pEffPerms = $profilePermSummary['effective_permissions'] ?? [];
          $pInhRoles = $profilePermSummary['inherited_roles'] ?? [];
        ?>
        <div style="margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--border-subtle);">
          <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; margin-bottom: 10px;">
            <span style="font-family: var(--font-mono); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-muted);">
              <i class="fa-solid fa-sitemap" style="color: #fff; margin-right: 5px;"></i>Aktif Rol Ağacınız & Oyun İçi İzinleriniz (<?php echo count($pEffPerms); ?> Perm)
            </span>
            <span style="font-family: var(--font-mono); font-size: 0.72rem; color: #aaa;">
              TAB & Baş Üstü Etiketi: <strong style="color: #fff;">[<?php echo htmlspecialchars($primaryRole['name']); ?>] <?php echo htmlspecialchars($mcUsername); ?></strong>
            </span>
          </div>
          <div style="display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px;">
            <?php foreach ($pInhRoles as $ir): ?>
              <span style="font-family: var(--font-mono); font-size: 0.72rem; padding: 3px 9px; border-radius: 999px; background: rgba(255,255,255,0.06); border: 1px solid <?php echo htmlspecialchars($ir['badge_color'] ?? '#fff'); ?>; color: #fff;">
                <i class="fa-solid fa-shield-halved" style="font-size: 0.65rem; margin-right: 3px;"></i><?php echo htmlspecialchars($ir['name']); ?>
              </span>
            <?php endforeach; ?>
          </div>
          <div style="display: flex; flex-wrap: wrap; gap: 6px;">
            <?php foreach ($pEffPerms as $permNode): ?>
              <code style="font-family: var(--font-mono); font-size: 0.72rem; padding: 3px 8px; border-radius: 5px; background: rgba(0,0,0,0.45); border: 1px solid rgba(255,255,255,0.12); color: #e4e4e7;">
                <?php echo htmlspecialchars($permNode === '*' ? '★ Tüm Sunucu İzinleri (*)' : $permNode); ?>
              </code>
            <?php endforeach; ?>
          </div>
        </div>
        <?php endif; ?>
      </div>

      <!-- Hızlı İşlemler Kartları (4'lü Grid) -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; margin-bottom: 26px;">
        <!-- VIP Mağazası Kartı -->
        <div class="feature-box" style="padding: 28px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
            <h4 class="feature-box-title" style="margin: 0;"><i class="fa-solid fa-crown"></i> VIP Mağazası</h4>
            <span class="portal-tab-badge" style="background: rgba(255, 255, 255, 0.08); color: #fff; font-weight: 700;">
              Otomatik Teslimat
            </span>
          </div>
          <p class="feature-box-desc" style="margin-bottom: 20px;">Cüzdan bakiyenizle özel VIP rütbeleri satın alın veya mevcut VIP sürenizi uzatın.</p>
          <a href="store.php" class="btn btn-primary btn-sm" style="width: 100%;">
            <i class="fa-solid fa-cart-shopping"></i>
            <span>Mağazaya Git</span>
          </a>
        </div>

        <!-- Destek Talepleri Kartı -->
        <div class="feature-box" style="padding: 28px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
            <h4 class="feature-box-title" style="margin: 0;"><i class="fa-solid fa-headset" style="color: #58a6ff;"></i> Destek & Bakiye Talebi</h4>
            <span class="portal-tab-badge" style="background: rgba(88, 166, 255, 0.15); color: #58a6ff; font-weight: 700;">
              <?php echo count($userTickets); ?> Bilet
            </span>
          </div>
          <p class="feature-box-desc" style="margin-bottom: 20px;">
            Hata bildirimlerinizi, bakiye yükleme taleplerinizi veya sorularınızı takip edin.
          </p>
          <div style="display: flex; gap: 10px;">
            <a href="support.php?tab=my" class="btn btn-ghost btn-sm" style="flex: 1;">
              <span>Taleplerim</span>
            </a>
            <a href="support.php?tab=new" class="btn btn-ghost btn-sm" title="Yeni Talep Aç">
              <i class="fa-solid fa-plus"></i>
            </a>
          </div>
        </div>

        <!-- Yetkili Başvuruları Kartı -->
        <div class="feature-box" style="padding: 28px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
            <h4 class="feature-box-title" style="margin: 0;"><i class="fa-solid fa-user-plus" style="color: #3fb950;"></i> Kadro Başvurusu</h4>
            <span class="portal-tab-badge" style="background: rgba(63, 185, 80, 0.15); color: #3fb950; font-weight: 700;">
              <?php echo count($userApps); ?> Başvuru
            </span>
          </div>
          <p class="feature-box-desc" style="margin-bottom: 20px;">
            Rehber, moderatör, mimar veya geliştirici pozisyonlarına başvurarak ekibe katılın.
          </p>
          <div style="display: flex; gap: 10px;">
            <a href="apply.php?tab=positions" class="btn btn-ghost btn-sm" style="flex: 1;">
              <span>Pozisyonlar</span>
            </a>
            <a href="apply.php?tab=my" class="btn btn-ghost btn-sm" title="Başvurularımı Gör">
              <i class="fa-solid fa-list-check"></i>
            </a>
          </div>
        </div>

        <!-- Topluluk & Discord Kartı -->
        <div class="feature-box" style="padding: 28px;">
          <h4 class="feature-box-title"><i class="fa-brands fa-discord" style="color: #5865F2;"></i> Discord Topluluğu</h4>
          <p class="feature-box-desc" style="margin-bottom: 20px;">Discord sunucumuzda rolünüzü senkronize edin ve çekilişlere katılın.</p>
          <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="btn btn-ghost btn-sm" style="width: 100%;">
            <span>Discord'a Git</span>
          </a>
        </div>
      </div>

      <!-- VIP & Cüzdan İşlem Geçmişi -->
      <div class="feature-box" style="padding: 28px;">
        <h4 class="feature-box-title" style="margin-bottom: 14px;"><i class="fa-solid fa-clock-rotate-left"></i> Mağaza & Yetki İşlem Geçmişi</h4>
        <?php if (empty($userOrders)): ?>
          <p style="font-size: 0.86rem; color: var(--text-muted); margin: 0;">Henüz kayıtlı bir mağaza veya kupon işleminiz bulunmuyor.</p>
        <?php else: ?>
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; font-size: 0.84rem;">
              <thead>
                <tr style="border-bottom: 1px solid var(--border-subtle); text-align: left; color: var(--text-muted); font-family: var(--font-mono); font-size: 0.74rem; text-transform: uppercase;">
                  <th style="padding: 10px 8px;">İşlem No</th>
                  <th style="padding: 10px 8px;">Paket / İşlem</th>
                  <th style="padding: 10px 8px;">Tutar</th>
                  <th style="padding: 10px 8px;">Detay</th>
                  <th style="padding: 10px 8px;">Tarih</th>
                </tr>
              </thead>
              <tbody>
                <?php foreach (array_slice($userOrders, 0, 15) as $ord): ?>
                  <tr style="border-bottom: 1px solid rgba(255,255,255,0.04);">
                    <td style="padding: 10px 8px; font-family: var(--font-mono); color: #aaa;"><?php echo htmlspecialchars($ord['order_id']); ?></td>
                    <td style="padding: 10px 8px; color: #fff; font-weight: 600;"><?php echo htmlspecialchars($ord['role_name'] ?: $ord['type']); ?></td>
                    <td style="padding: 10px 8px; font-family: var(--font-mono); color: #fff;">
                      <?php echo floatval($ord['amount']) > 0 ? (number_format(floatval($ord['amount']), 2, '.', '') . ' ' . STORE_CURRENCY) : '-'; ?>
                    </td>
                    <td style="padding: 10px 8px; color: var(--text-secondary);"><?php echo htmlspecialchars($ord['note'] ?? ''); ?></td>
                    <td style="padding: 10px 8px; font-family: var(--font-mono); color: var(--text-muted);"><?php echo date('d.m.Y H:i', intval($ord['created_at'] ?? time())); ?></td>
                  </tr>
                <?php endforeach; ?>
              </tbody>
            </table>
          </div>
        <?php endif; ?>
      </div>
    </div>
  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
