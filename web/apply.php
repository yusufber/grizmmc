<?php
/**
 * GrizmMC Network - Yetkili & Kadro Başvuru Sistemi
 */
$pageTitle = "Yetkili Başvurusu";
$activePage = "apply";
require_once __DIR__ . '/config.php';

// POST: Başvuru Gönderimi
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'submit_application') {
    verify_csrf_token();
    if (!is_logged_in()) {
        $_SESSION['flash_error'] = 'Başvuru yapabilmek için lütfen önce giriş yapın.';
        header('Location: login.php');
        exit;
    }

    $position = trim($_POST['position'] ?? 'rehber');
    $fullName = trim($_POST['full_name'] ?? '');
    $age = (int)($_POST['age'] ?? 0);
    $discord = trim($_POST['discord'] ?? '');
    $playTime = trim($_POST['play_time'] ?? '');
    $experience = trim($_POST['experience'] ?? '');
    $whyUs = trim($_POST['why_us'] ?? '');
    $about = trim($_POST['about'] ?? '');

    if (empty($fullName) || $age <= 0 || empty($discord) || empty($experience) || empty($whyUs)) {
        $_SESSION['flash_error'] = 'Lütfen formdaki tüm zorunlu alanları eksiksiz doldurun.';
        header('Location: apply.php?tab=form');
        exit;
    }

    if (!isset($APPLICATION_POSITIONS[$position])) {
        $position = 'rehber';
    }

    $user = get_auth_user();
    $newApp = create_application([
        'position' => $position,
        'full_name' => $fullName,
        'minecraft_username' => get_auth_username(),
        'user_email' => $user['email'],
        'age' => $age,
        'discord' => $discord,
        'play_time' => $playTime,
        'experience' => $experience,
        'why_us' => $whyUs,
        'about' => $about
    ]);

    $_SESSION['flash_success'] = 'Kadro başvurunuz #' . $newApp['id'] . ' başarıyla alındı! Ekibimiz başvurunuzu inceledikten sonra buradan ve Discord üzerinden dönüş yapacaktır.';
    header('Location: apply.php?tab=my');
    exit;
}

// POST: Yetkili Durum Güncellemesi
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'review_application') {
    verify_csrf_token();
    if (!is_staff_user()) {
        $_SESSION['flash_error'] = 'Bu işlem için yetkiniz bulunmamaktadır.';
        header('Location: apply.php');
        exit;
    }

    $appId = trim($_POST['app_id'] ?? '');
    $newStatus = trim($_POST['status'] ?? 'pending');
    $adminNote = trim($_POST['admin_note'] ?? '');

    if (isset($APPLICATION_STATUSES[$newStatus])) {
        update_application_status($appId, $newStatus, $adminNote);
        $_SESSION['flash_success'] = 'Başvuru #' . $appId . ' durumu güncellendi: ' . $APPLICATION_STATUSES[$newStatus]['title'];
    }

    header('Location: apply.php?tab=staff');
    exit;
}

$user = get_auth_user();
$isStaff = is_staff_user();
$currentTab = $_GET['tab'] ?? ($isStaff && isset($_GET['staff']) ? 'staff' : (is_logged_in() ? 'positions' : 'info'));

// Kullanıcı ve Yetkili Başvuruları
$userApps = is_logged_in() ? get_user_applications($user['email']) : [];
$allApps = $isStaff ? get_all_applications() : [];

// Son güncellenen en üstte
if ($isStaff) {
    uasort($allApps, function($a, $b) {
        return ($b['created_at'] ?? 0) <=> ($a['created_at'] ?? 0);
    });
}

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section" style="padding-top: 50px; min-height: 85vh;">
  <div class="container">

    <!-- Başlık & Bilgi -->
    <div class="section-header" style="margin-bottom: 36px;">
      <span class="section-tag"><i class="fa-solid fa-users"></i> Yetkili Alımları</span>
      <h1 class="section-title">GRIZMMC EKİBİNE KATIL</h1>
      <p class="section-desc">
        Gelişmiş altyapımız, yenilikçi oyun modlarımız ve tutkulu oyuncu topluluğumuzla büyüyen ailemizin bir parçası olun.
      </p>
    </div>

    <!-- Yetkili Durum Bildirim Çubuğu -->
    <?php if (is_logged_in()): ?>
      <div class="staff-mode-bar">
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="width: 10px; height: 10px; border-radius: 50%; background: <?php echo $isStaff ? '#3fb950' : '#8b949e'; ?>; box-shadow: 0 0 10px <?php echo $isStaff ? '#3fb950' : 'transparent'; ?>;"></span>
          <span style="font-size: 0.88rem; color: #fff;">
            Mod: <strong><?php echo $isStaff ? 'Yetkili / Yönetici Paneli Aktif' : 'Oyuncu Görünümü'; ?></strong>
          </span>
          <?php if ($isStaff): ?>
            <span class="staff-pill"><i class="fa-solid fa-shield-check"></i> Yetkili</span>
          <?php endif; ?>
        </div>
      </div>
    <?php endif; ?>

    <!-- Sekmeler (Tab Menü) -->
    <div class="portal-tabs">
      <a href="apply.php?tab=positions" class="portal-tab <?php echo ($currentTab === 'positions') ? 'active' : ''; ?>">
        <i class="fa-solid fa-id-card-clip"></i>
        <span>Açık Pozisyonlar & Şartlar</span>
      </a>

      <?php if (is_logged_in()): ?>
        <a href="apply.php?tab=form" class="portal-tab <?php echo ($currentTab === 'form') ? 'active' : ''; ?>">
          <i class="fa-solid fa-file-pen"></i>
          <span>Başvuru Formu Doldur</span>
        </a>

        <a href="apply.php?tab=my" class="portal-tab <?php echo ($currentTab === 'my') ? 'active' : ''; ?>">
          <i class="fa-solid fa-clipboard-check"></i>
          <span>Başvurularım</span>
          <span class="portal-tab-badge"><?php echo count($userApps); ?></span>
        </a>

        <?php if ($isStaff): ?>
          <a href="apply.php?tab=staff" class="portal-tab <?php echo ($currentTab === 'staff') ? 'active' : ''; ?>" style="color: <?php echo ($currentTab === 'staff') ? '#000' : '#58a6ff'; ?>;">
            <i class="fa-solid fa-user-shield"></i>
            <span>Tüm Başvurular (Yetkili İnceleme)</span>
            <span class="portal-tab-badge" style="background: <?php echo ($currentTab === 'staff') ? 'rgba(0,0,0,0.15)' : 'rgba(88,166,255,0.2)'; ?>; color: inherit;">
              <?php echo count($allApps); ?>
            </span>
          </a>
        <?php endif; ?>
      <?php else: ?>
        <a href="apply.php?tab=info" class="portal-tab <?php echo ($currentTab === 'info') ? 'active' : ''; ?>">
          <i class="fa-solid fa-circle-info"></i>
          <span>Başvuru Bilgisi</span>
        </a>
      <?php endif; ?>
    </div>

    <!-- TAB: POZİSYONLAR & ŞARTLAR -->
    <?php if ($currentTab === 'positions' || (!is_logged_in() && $currentTab === 'info')): ?>
      <div style="margin-bottom: 30px;">
        <h3 style="font-family: var(--font-display); font-size: 1.4rem; color: #fff; margin-bottom: 8px;">
          AÇIK KADRO POZİSYONLARI
        </h3>
        <p style="color: var(--text-secondary); font-size: 0.92rem;">
          Yeteneklerinize ve ilgi alanınıza en uygun pozisyonu seçerek GrizmMC ekibine dahil olabilirsiniz.
        </p>
      </div>

      <div class="position-grid" style="grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));">
        <?php foreach ($APPLICATION_POSITIONS as $posKey => $pos): ?>
          <div class="position-card">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
              <div class="position-card-icon" style="color: <?php echo $pos['color']; ?>;">
                <i class="<?php echo $pos['icon']; ?>"></i>
              </div>
              <span class="staff-pill" style="background: rgba(255,255,255,0.08); color: <?php echo $pos['color']; ?>; border: 1px solid rgba(255,255,255,0.2);">
                <?php echo $pos['badge']; ?>
              </span>
            </div>

            <h4 class="position-card-title"><?php echo htmlspecialchars($pos['title']); ?></h4>
            <p class="position-card-summary"><?php echo htmlspecialchars($pos['summary']); ?></p>

            <div style="font-size: 0.76rem; text-transform: uppercase; letter-spacing: 0.6px; color: #fff; font-weight: 700; margin-bottom: 8px;">
              Aranan Şartlar:
            </div>
            <ul class="position-req-list" style="margin-bottom: 20px;">
              <?php foreach ($pos['reqs'] as $req): ?>
                <li><i class="fa-solid fa-check"></i> <span><?php echo htmlspecialchars($req); ?></span></li>
              <?php endforeach; ?>
            </ul>

            <?php if (is_logged_in()): ?>
              <a href="apply.php?tab=form&role=<?php echo urlencode($posKey); ?>" class="btn btn-primary btn-sm" style="margin-top: auto; width: 100%;">
                <i class="fa-solid fa-paper-plane"></i>
                <span>Bu Role Başvur</span>
              </a>
            <?php else: ?>
              <a href="login.php" class="btn btn-ghost btn-sm" style="margin-top: auto; width: 100%;">
                <i class="fa-solid fa-lock"></i>
                <span>Başvurmak İçin Giriş Yap</span>
              </a>
            <?php endif; ?>
          </div>
        <?php endforeach; ?>
      </div>

    <!-- TAB: BAŞVURU FORMU -->
    <?php elseif ($currentTab === 'form' && is_logged_in()): 
      $preselectedRole = $_GET['role'] ?? 'rehber';
    ?>
      <div class="feature-box" style="padding: 40px; max-width: 860px; margin: 0 auto;">
        <div style="margin-bottom: 28px;">
          <h2 style="font-family: var(--font-display); font-size: 1.6rem; color: #fff; margin-bottom: 6px; text-transform: uppercase;">
            Yetkili Başvuru Formu
          </h2>
          <p style="color: var(--text-secondary); font-size: 0.92rem;">
            Lütfen aşağıdaki soruları içtenlikle ve doğru bilgilerle yanıtlayınız. Formunuz üst yönetim tarafından titizlikle değerlendirilecektir.
          </p>
        </div>

        <form method="POST" action="apply.php">
          <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
          <input type="hidden" name="action" value="submit_application">

          <!-- Pozisyon Seçimi (Görsel Kartlar) -->
          <div class="form-group">
            <label class="form-label">Başvurmak İstediğiniz Pozisyon</label>
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px;">
              <?php foreach ($APPLICATION_POSITIONS as $posKey => $pos): 
                $isSelected = ($preselectedRole === $posKey);
              ?>
                <label style="background: rgba(255,255,255,0.03); border: 1px solid <?php echo $isSelected ? '#fff' : 'var(--border-medium)'; ?>; border-radius: var(--radius-sm); padding: 12px; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: var(--transition-fast);">
                  <input type="radio" name="position" value="<?php echo $posKey; ?>" <?php echo $isSelected ? 'checked' : ''; ?> required>
                  <span style="font-size: 0.88rem; font-weight: 600; color: #fff; display: flex; align-items: center; gap: 6px;">
                    <i class="<?php echo $pos['icon']; ?>" style="color: <?php echo $pos['color']; ?>;"></i> <?php echo htmlspecialchars($pos['badge']); ?>
                  </span>
                </label>
              <?php endforeach; ?>
            </div>
          </div>

          <!-- Kişisel Bilgiler -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;" class="form-row">
            <div class="form-group">
              <label for="full_name" class="form-label">Adınız ve Soyadınız</label>
              <input 
                type="text" 
                id="full_name" 
                name="full_name" 
                class="form-input" 
                value="<?php echo htmlspecialchars($user['full_name'] ?? ''); ?>" 
                placeholder="Örn: Ahmet Yılmaz" 
                required
              >
            </div>

            <div class="form-group">
              <label for="age" class="form-label">Yaşınız</label>
              <input 
                type="number" 
                id="age" 
                name="age" 
                class="form-input" 
                placeholder="Örn: 17" 
                min="13" 
                max="60" 
                required
              >
            </div>
          </div>

          <!-- İletişim & Karakter Bilgileri -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;" class="form-row">
            <div class="form-group">
              <label class="form-label">Minecraft Oyuncu Adınız</label>
              <div style="display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,0.04); border: 1px solid var(--border-medium); padding: 12px 16px; border-radius: var(--radius-sm);">
                <img 
                  src="https://mc-heads.net/avatar/<?php echo urlencode(get_auth_username()); ?>/26" 
                  alt="Avatar" 
                  style="width: 26px; height: 26px; border-radius: 4px;"
                >
                <strong style="color: #fff; font-family: var(--font-mono); font-size: 0.95rem;"><?php echo htmlspecialchars(get_auth_username()); ?></strong>
                <span style="margin-left: auto; font-size: 0.74rem; color: #3fb950;"><i class="fa-solid fa-circle-check"></i> Doğrulandı</span>
              </div>
            </div>

            <div class="form-group">
              <label for="discord" class="form-label">Discord Kullanıcı Adınız</label>
              <input 
                type="text" 
                id="discord" 
                name="discord" 
                class="form-input" 
                placeholder="Örn: ahmet_mc veya ahmet#1234" 
                required
              >
              <small style="color: var(--text-muted); font-size: 0.74rem; display: block; margin-top: 4px;">
                Mülakat ve iletişim Discord üzerinden sağlanacaktır.
              </small>
            </div>
          </div>

          <!-- Günlük Aktiflik -->
          <div class="form-group">
            <label for="play_time" class="form-label">Günlük Ortalama Aktiflik Süreniz</label>
            <input 
              type="text" 
              id="play_time" 
              name="play_time" 
              class="form-input" 
              placeholder="Örn: Hafta içi 3-4 saat, hafta sonu 6+ saat" 
              required
            >
          </div>

          <!-- Deneyimler -->
          <div class="form-group">
            <label for="experience" class="form-label">Daha Önceki Sunucu & Moderasyon Tecrübeleriniz</label>
            <textarea 
              name="experience" 
              id="experience" 
              class="form-textarea" 
              rows="4" 
              placeholder="Daha önce hangi Minecraft sunucularında görev aldınız, hangi pozisyonlarda bulundunuz ve neden ayrıldınız?" 
              required
            ></textarea>
          </div>

          <!-- Neden GrizmMC -->
          <div class="form-group">
            <label for="why_us" class="form-label">Neden GrizmMC Network? Neden Sizi Seçmeliyiz?</label>
            <textarea 
              name="why_us" 
              id="why_us" 
              class="form-textarea" 
              rows="4" 
              placeholder="Sunucumuza katabileceğiniz değerler, güçlü yönleriniz ve vizyonunuz nedir?" 
              required
            ></textarea>
          </div>

          <!-- Kendinizi Tanıtın -->
          <div class="form-group">
            <label for="about" class="form-label">Kendinizi Tanıtın & Eklemek İstedikleriniz (Opsiyonel)</label>
            <textarea 
              name="about" 
              id="about" 
              class="form-textarea" 
              rows="3" 
              placeholder="İlgi alanlarınız, bildiğiniz araçlar (WorldEdit vb.), mimari veya yazılım çalışmalarınız..."
            ></textarea>
          </div>

          <div style="display: flex; justify-content: flex-end; gap: 14px; margin-top: 26px;">
            <a href="apply.php?tab=positions" class="btn btn-ghost">Vazgeç</a>
            <button type="submit" class="btn btn-primary btn-lg">
              <i class="fa-solid fa-paper-plane"></i>
              <span>Başvuruyu Tamamla & Gönder</span>
            </button>
          </div>
        </form>
      </div>

    <!-- TAB: BAŞVURULARIM -->
    <?php elseif ($currentTab === 'my' && is_logged_in()): ?>
      <div style="margin-bottom: 24px;">
        <h3 style="font-family: var(--font-display); font-size: 1.4rem; color: #fff;">
          GÖNDERDİĞİNİZ BAŞVURULAR
        </h3>
        <p style="color: var(--text-secondary); font-size: 0.88rem;">
          Başvurularınızın güncel durumunu, mülakat davetlerini ve yetkili notlarını buradan takip edebilirsiniz.
        </p>
      </div>

      <?php if (empty($userApps)): ?>
        <div class="empty-box">
          <i class="fa-regular fa-clipboard"></i>
          <h3>Henüz Başvuru Yapmadınız</h3>
          <p style="margin-bottom: 20px;">Açık pozisyonları inceleyerek ekibimize katılmak için hemen başvurun.</p>
          <a href="apply.php?tab=positions" class="btn btn-primary">
            <i class="fa-solid fa-arrow-right"></i>
            <span>Pozisyonları İncele & Başvur</span>
          </a>
        </div>
      <?php else: ?>
        <div style="display: flex; flex-direction: column; gap: 20px;">
          <?php foreach ($userApps as $app): 
            $pos = $APPLICATION_POSITIONS[$app['position']] ?? $APPLICATION_POSITIONS['rehber'];
            $status = $APPLICATION_STATUSES[$app['status']] ?? $APPLICATION_STATUSES['pending'];
          ?>
            <div class="feature-box" style="padding: 28px;">
              <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 18px; padding-bottom: 16px; border-bottom: 1px solid var(--border-subtle);">
                <div style="display: flex; align-items: center; gap: 14px;">
                  <div class="portal-stat-icon" style="color: <?php echo $pos['color']; ?>; width: 44px; height: 44px;">
                    <i class="<?php echo $pos['icon']; ?>"></i>
                  </div>
                  <div>
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-muted);">#<?php echo htmlspecialchars($app['id']); ?></span>
                      <h4 style="font-size: 1.15rem; color: #fff; margin: 0;"><?php echo htmlspecialchars($pos['title']); ?> Başvurusu</h4>
                    </div>
                    <span style="font-size: 0.78rem; color: var(--text-muted);"><i class="fa-regular fa-calendar"></i> <?php echo date('d.m.Y H:i', $app['created_at']); ?></span>
                  </div>
                </div>

                <div style="display: flex; align-items: center; gap: 12px;">
                  <span class="badge-status <?php echo $status['badge']; ?>" style="font-size: 0.85rem; padding: 6px 14px;">
                    <i class="<?php echo $status['icon']; ?>"></i> <?php echo $status['title']; ?>
                  </span>
                </div>
              </div>

              <!-- Başvuru Özeti -->
              <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-bottom: 16px; font-size: 0.88rem;">
                <div>
                  <span style="color: var(--text-muted); display: block; font-size: 0.75rem; text-transform: uppercase;">İsim / Yaş:</span>
                  <strong style="color: #fff;"><?php echo htmlspecialchars($app['full_name']); ?> (<?php echo (int)$app['age']; ?> yaş)</strong>
                </div>
                <div>
                  <span style="color: var(--text-muted); display: block; font-size: 0.75rem; text-transform: uppercase;">Discord:</span>
                  <strong style="color: #5865F2; font-family: var(--font-mono);"><i class="fa-brands fa-discord"></i> <?php echo htmlspecialchars($app['discord']); ?></strong>
                </div>
                <div>
                  <span style="color: var(--text-muted); display: block; font-size: 0.75rem; text-transform: uppercase;">Aktiflik:</span>
                  <strong style="color: #fff;"><?php echo htmlspecialchars($app['play_time']); ?></strong>
                </div>
              </div>

              <!-- Yönetici Notu (Varsa) -->
              <?php if (!empty($app['admin_note'])): ?>
                <div style="background: rgba(88, 166, 255, 0.08); border-left: 3px solid #58a6ff; padding: 14px 18px; border-radius: var(--radius-sm); margin-top: 14px;">
                  <strong style="color: #58a6ff; display: block; font-size: 0.85rem; margin-bottom: 4px;">
                    <i class="fa-solid fa-comment-dots"></i> Yönetici Değerlendirme Notu:
                  </strong>
                  <p style="color: #fff; font-size: 0.9rem; line-height: 1.5; margin: 0;">
                    <?php echo nl2br(htmlspecialchars($app['admin_note'])); ?>
                  </p>
                </div>
              <?php endif; ?>
            </div>
          <?php endforeach; ?>
        </div>
      <?php endif; ?>

    <!-- TAB: YETKİLİ İNCELEME PANELİ -->
    <?php elseif ($currentTab === 'staff' && $isStaff): ?>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 14px;">
        <div>
          <h3 style="font-family: var(--font-display); font-size: 1.4rem; color: #fff;">
            YETKİLİ BAŞVURU İNCELEME HAVUZU
          </h3>
          <p style="color: var(--text-secondary); font-size: 0.88rem;">
            Tüm adayların başvurularını detaylıca inceleyebilir, mülakata çağırabilir veya durumu güncelleyebilirsiniz.
          </p>
        </div>
        <span class="portal-tab-badge" style="background: rgba(255,255,255,0.1); color: #fff; padding: 6px 14px; font-size: 0.82rem;">
          Toplam Başvuru: <?php echo count($allApps); ?>
        </span>
      </div>

      <?php if (empty($allApps)): ?>
        <div class="empty-box">
          <i class="fa-solid fa-inbox"></i>
          <h3>Henüz Başvuru Bulunmamaktadır</h3>
          <p>Oyuncular başvuru gönderdiğinde burada listelenecektir.</p>
        </div>
      <?php else: ?>
        <div style="display: flex; flex-direction: column; gap: 24px;">
          <?php foreach ($allApps as $app): 
            $pos = $APPLICATION_POSITIONS[$app['position']] ?? $APPLICATION_POSITIONS['rehber'];
            $status = $APPLICATION_STATUSES[$app['status']] ?? $APPLICATION_STATUSES['pending'];
          ?>
            <div class="feature-box" style="padding: 30px; border-left: 4px solid <?php echo $status['color']; ?>;">
              <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--border-subtle);">
                <div style="display: flex; align-items: center; gap: 14px;">
                  <img 
                    src="https://mc-heads.net/avatar/<?php echo urlencode($app['minecraft_username']); ?>/48" 
                    alt="Avatar" 
                    style="width: 48px; height: 48px; border-radius: 6px; background: #111;"
                    onerror="this.src='https://mc-heads.net/avatar/Steve/48';"
                  >
                  <div>
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted);">#<?php echo htmlspecialchars($app['id']); ?></span>
                      <h4 style="font-size: 1.2rem; color: #fff; margin: 0;"><?php echo htmlspecialchars($app['full_name']); ?></h4>
                      <span class="staff-pill" style="color: <?php echo $pos['color']; ?>;"><?php echo $pos['badge']; ?></span>
                    </div>
                    <div style="font-size: 0.82rem; color: var(--text-secondary); margin-top: 4px; display: flex; gap: 14px; flex-wrap: wrap;">
                      <span><i class="fa-solid fa-gamepad"></i> IGN: <strong><?php echo htmlspecialchars($app['minecraft_username']); ?></strong></span>
                      <span><i class="fa-brands fa-discord"></i> <?php echo htmlspecialchars($app['discord']); ?></span>
                      <span><i class="fa-solid fa-cake-candles"></i> <?php echo (int)$app['age']; ?> Yaş</span>
                      <span><i class="fa-regular fa-clock"></i> <?php echo time_ago_tr($app['created_at']); ?></span>
                    </div>
                  </div>
                </div>

                <span class="badge-status <?php echo $status['badge']; ?>" style="font-size: 0.85rem; padding: 6px 14px;">
                  <i class="<?php echo $status['icon']; ?>"></i> <?php echo $status['title']; ?>
                </span>
              </div>

              <!-- Detaylar -->
              <div style="display: flex; flex-direction: column; gap: 14px; font-size: 0.92rem; line-height: 1.6; margin-bottom: 24px;">
                <div>
                  <strong style="color: #fff; display: block; font-size: 0.82rem; text-transform: uppercase; margin-bottom: 4px; color: var(--text-muted);">Günlük Aktiflik:</strong>
                  <div style="color: #ededed;"><?php echo htmlspecialchars($app['play_time']); ?></div>
                </div>

                <div>
                  <strong style="color: #fff; display: block; font-size: 0.82rem; text-transform: uppercase; margin-bottom: 4px; color: var(--text-muted);">Önceki Tecrübeler & Sunucular:</strong>
                  <div style="background: rgba(255,255,255,0.03); padding: 12px 16px; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); color: #ededed; white-space: pre-wrap;"><?php echo htmlspecialchars($app['experience']); ?></div>
                </div>

                <div>
                  <strong style="color: #fff; display: block; font-size: 0.82rem; text-transform: uppercase; margin-bottom: 4px; color: var(--text-muted);">Neden GrizmMC & Neden Bu Aday:</strong>
                  <div style="background: rgba(255,255,255,0.03); padding: 12px 16px; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); color: #ededed; white-space: pre-wrap;"><?php echo htmlspecialchars($app['why_us']); ?></div>
                </div>

                <?php if (!empty($app['about'])): ?>
                  <div>
                    <strong style="color: #fff; display: block; font-size: 0.82rem; text-transform: uppercase; margin-bottom: 4px; color: var(--text-muted);">Ek Açıklama & Yetenekler:</strong>
                    <div style="background: rgba(255,255,255,0.03); padding: 12px 16px; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); color: #ededed; white-space: pre-wrap;"><?php echo htmlspecialchars($app['about']); ?></div>
                  </div>
                <?php endif; ?>
              </div>

              <!-- Yetkili Karar & Not Formu -->
              <div style="background: rgba(14, 14, 18, 0.7); border: 1px solid var(--border-medium); border-radius: var(--radius-sm); padding: 20px;">
                <form method="POST" action="apply.php?tab=staff">
                  <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
                  <input type="hidden" name="action" value="review_application">
                  <input type="hidden" name="app_id" value="<?php echo htmlspecialchars($app['id']); ?>">

                  <div style="display: grid; grid-template-columns: 200px 1fr auto; gap: 14px; align-items: flex-end;" class="form-row">
                    <div class="form-group" style="margin-bottom: 0;">
                      <label class="form-label" style="font-size: 0.76rem;">Karar / Durum</label>
                      <select name="status" class="form-select" style="padding: 10px 14px; font-size: 0.88rem;">
                        <?php foreach ($APPLICATION_STATUSES as $sk => $sv): ?>
                          <option value="<?php echo $sk; ?>" <?php echo ($app['status'] === $sk) ? 'selected' : ''; ?>>
                            <?php echo htmlspecialchars($sv['title']); ?>
                          </option>
                        <?php endforeach; ?>
                      </select>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                      <label class="form-label" style="font-size: 0.76rem;">Adaya Görünecek Değerlendirme Notu</label>
                      <input 
                        type="text" 
                        name="admin_note" 
                        class="form-input" 
                        style="padding: 10px 14px; font-size: 0.88rem;" 
                        placeholder="Örn: Discord sunucumuzda #mulakat odasına bekleniyorsunuz..." 
                        value="<?php echo htmlspecialchars($app['admin_note'] ?? ''); ?>"
                      >
                    </div>

                    <button type="submit" class="btn btn-primary btn-sm" style="height: 42px;">
                      <i class="fa-solid fa-check"></i>
                      <span>Kaydet</span>
                    </button>
                  </div>
                </form>
              </div>

            </div>
          <?php endforeach; ?>
        </div>
      <?php endif; ?>

    <?php endif; ?>

  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
